"""enrich_ror_wikidata.py 단위 테스트 — 네트워크·DB 없이 순수 함수만 검증한다.

실행: python3 -I scripts/test_enrich_ror_wikidata.py  (또는 scripts/ 에서 python3 -m unittest)
"""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from enrich_ror_wikidata import (  # noqa: E402
    batch_cache_key, cached, host_suffixes, match_by_domain, match_by_name, name_similarity,
    normalize_name, pick_label, plan_name_change, write_apply_sql,
)


def ror(rid: str, name: str, country: str = "US", hosts: tuple[str, ...] = (),
        types: tuple[str, ...] = ("education",), status: str = "active") -> dict:
    return {
        "id": f"https://ror.org/{rid}", "status": status, "types": list(types),
        "names": [{"value": name, "types": ["ror_display"], "lang": None}],
        "domains": list(hosts), "links": [],
        "locations": [{"geonames_id": 1, "geonames_details": {"country_code": country, "lat": 0.0, "lng": 0.0}}],
        "external_ids": [],
    }


def master(name: str, domain: str = "", country: str = "US") -> dict:
    return {"id": "1", "name_en": name, "domain": domain, "website": "", "country_code": country}


def index(*recs: dict) -> dict:
    by_host: dict[str, list] = {}
    for rec in recs:
        for h in rec["domains"]:
            by_host.setdefault(h, []).append(rec)
    return by_host


class NormalizeTest(unittest.TestCase):
    def test_strips_accents_and_ampersand(self) -> None:
        self.assertEqual("universite de montreal", normalize_name("Université de Montréal"))
        self.assertEqual("texas a and m university", normalize_name("Texas A&M University"))

    def test_similarity_ignores_stopwords(self) -> None:
        self.assertEqual(1.0, name_similarity("University of Tokyo", "The University Tokyo"))
        self.assertEqual(0.0, name_similarity("", "University"))

    def test_host_suffixes_stop_before_last_label(self) -> None:
        self.assertEqual(["student.eit.edu.au", "eit.edu.au", "edu.au"], host_suffixes("student.eit.edu.au"))


class MatchByDomainTest(unittest.TestCase):
    def test_exact_host_match(self) -> None:
        rec = ror("a", "Stanford University", hosts=("stanford.edu",))
        cand, method = match_by_domain(master("Stanford University", "stanford.edu"), index(rec))
        self.assertEqual("domain_exact", method)
        self.assertIs(rec, cand[2])

    def test_other_country_is_ignored(self) -> None:
        rec = ror("a", "Stanford University", country="CA", hosts=("stanford.edu",))
        self.assertEqual((None, "no_domain_candidate"),
                         match_by_domain(master("Stanford University", "stanford.edu"), index(rec)))

    def test_suffix_match_needs_high_name_similarity(self) -> None:
        # 하위 도메인을 잘라 붙은 상위 기관(대학 시스템 본부)은 이름이 충분히 같지 않으면 버린다.
        system = ror("s", "Université du Québec", country="CA", hosts=("uquebec.ca",))
        row = master("Université du Québec en Abitibi-Témiscamingue", "uqat.uquebec.ca", "CA")
        self.assertEqual((None, "no_domain_candidate"), match_by_domain(row, index(system)))

    def test_suffix_match_accepted_when_names_agree(self) -> None:
        rec = ror("a", "Eastern Institute of Technology", country="AU", hosts=("eit.edu.au",))
        row = master("Eastern Institute of Technology", "student.eit.edu.au", "AU")
        self.assertEqual("domain_suffix", match_by_domain(row, index(rec))[1])

    def test_close_candidates_are_ambiguous(self) -> None:
        a = ror("a", "Lincoln University", hosts=("lincoln.edu",))
        b = ror("b", "Lincoln University", hosts=("lincoln.edu",))
        self.assertEqual((None, "ambiguous_domain"),
                         match_by_domain(master("Lincoln University", "lincoln.edu"), index(a, b)))


class MatchByNameTest(unittest.TestCase):
    def test_unique_and_ambiguous(self) -> None:
        a = ror("a", "Kyoto University", country="JP")
        key = (normalize_name("Kyoto University"), "JP")
        self.assertEqual("name_exact", match_by_name(master("Kyoto University", country="JP"), {key: [a]})[1])
        b = ror("b", "Kyoto University", country="JP")
        self.assertEqual((None, "ambiguous_name"),
                         match_by_name(master("Kyoto University", country="JP"), {key: [a, b]}))
        self.assertEqual((None, "no_match"), match_by_name(master("Nowhere", country="JP"), {}))


class PickLabelTest(unittest.TestCase):
    def test_script_mismatch_is_dropped(self) -> None:
        self.assertEqual((None, "script_mismatch"), pick_label("ko", {"ko": "Seoul National University"}, "KR", "x"))

    def test_zh_prefers_traditional_in_taiwan(self) -> None:
        labels = {"zh-hans": "台湾大学", "zh-hant": "臺灣大學"}
        self.assertEqual(("臺灣大學", "zh-hant"), pick_label("zh", labels, "TW", "National Taiwan University"))
        self.assertEqual(("台湾大学", "zh-hans"), pick_label("zh", labels, "CN", "National Taiwan University"))

    def test_copy_of_english_name_is_dropped(self) -> None:
        self.assertEqual((None, "same_as_en"), pick_label("de", {"de": "Harvard  university"}, "US", "Harvard University"))
        self.assertEqual(("Universität Wien", "de"), pick_label("de", {"de": "Universität Wien"}, "AT", "University of Vienna"))

    def test_pt_falls_back_to_brazilian(self) -> None:
        self.assertEqual(("Universidade de São Paulo", "pt-br"),
                         pick_label("pt", {"pt-br": "Universidade de São Paulo"}, "BR", "University of Sao Paulo"))


class PlanNameChangeTest(unittest.TestCase):
    def cur(self, name: str, source: str, qid: str = "Q1") -> dict:
        return {"name": name, "source": source, "qid": qid}

    def test_actions(self) -> None:
        self.assertEqual("insert", plan_name_change(None, "A", "Q1"))
        self.assertEqual("unchanged", plan_name_change(self.cur("A", "wikidata"), "A", "Q1"))
        self.assertEqual("keep_protected", plan_name_change(self.cur("B", "manual"), "A", "Q1"))
        self.assertEqual("keep_protected", plan_name_change(self.cur("B", "exchange"), "A", "Q1"))
        self.assertEqual("replace_suspicious", plan_name_change(self.cur("B", "wikidata_check"), "A", "Q1"))
        self.assertEqual("refresh", plan_name_change(self.cur("B", "wikidata"), "A", "Q1"))
        self.assertEqual("conflict_review", plan_name_change(self.cur("B", "wikidata", "Q2"), "A", "Q1"))


class CacheTest(unittest.TestCase):
    def test_key_depends_on_whole_batch(self) -> None:
        # 같은 위치·같은 첫 QID 라도 구성이 다르면 다른 키여야 옛 응답을 재사용하지 않는다.
        self.assertNotEqual(batch_cache_key("labels", ["Q1", "Q2"]), batch_cache_key("labels", ["Q1", "Q3"]))
        self.assertEqual(batch_cache_key("labels", ["Q1", "Q2"]), batch_cache_key("labels", ["Q1", "Q2"]))
        self.assertNotEqual(batch_cache_key("labels", ["Q1"]), batch_cache_key("sparql", ["Q1"]))

    def test_cached_fetches_once(self) -> None:
        calls = []
        with tempfile.TemporaryDirectory() as d:
            for _ in range(2):
                value = cached(Path(d), "k", lambda: calls.append(1) or {"v": 1})
            self.assertEqual({"v": 1}, value)
            self.assertEqual(1, len(calls))


class ApplySqlTest(unittest.TestCase):
    def test_guards_and_post_check(self) -> None:
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / "apply.sql"
            write_apply_sql(path,
                            [{"id": "7", "ror_id": "abc", "qid": "Q1", "geonames_id": 5, "ror_status": "active",
                              "lat": 1.5, "lon": 2.5}],
                            [{"id": "7", "lang": "fr", "name": "L'Université", "qid": "Q1"}],
                            {"ror_dump": "d.json", "rows": 1, "generated": "now", "expect_ror": 1, "expect_names": 1})
            sql = path.read_text()
        self.assertIn("\\set ON_ERROR_STOP on", sql)
        self.assertIn("RAISE EXCEPTION 'V31 미적용", sql)
        self.assertIn("'L''Université'", sql)  # 작은따옴표 이스케이프
        self.assertIn("COALESCE(g.ror_id, v.ror_id)", sql)
        self.assertIn("WHERE source = 'wikidata_ror'", sql)
        self.assertIn("IF n_ror < 1 OR n_names < 1 THEN", sql)
        self.assertNotIn("COMMIT;\n", sql.replace("-- 확인 후 수동으로 COMMIT;", ""))


if __name__ == "__main__":
    unittest.main()
