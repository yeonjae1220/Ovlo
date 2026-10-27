#!/usr/bin/env python3
"""global_universities 를 ROR → Wikidata 로 보강하는 dry-run 생성기.

DB 에는 아무것도 쓰지 않는다. 운영 스냅샷 CSV 를 읽어 매칭 결과·변경 diff·apply SQL 을
--out-dir 에 만든다. apply SQL 은 사람이 검토한 뒤 직접 실행한다(GLOBAL-PIT-063).

매칭 순서
  1. 도메인: 마스터 domain/website 호스트 ↔ ROR domains·website 호스트 (같은 국가만)
  2. 이름:   정규화한 영문명 ↔ ROR 의 모든 이름(별칭 포함), 같은 국가 + education 유형 + 후보 1개
  후보가 둘 이상이면서 점수 차가 작으면 '모호'로 버린다(오매칭보다 미매칭이 낫다).

Wikidata QID 는 이름 검색이 아니라 ROR 레코드의 external_ids 에서 가져온다.
라벨은 UI 언어(SUPPORTED_UI_LANGUAGES)와 기존 vi 를 가져오되, 문자 체계가 맞지 않는
라벨(예: ko 라벨이 라틴 문자)은 번역이 아니므로 버린다.

사용 예
  python3 -I scripts/enrich_ror_wikidata.py \
    --ror-json <dir>/v2.15-...-ror-data.json \
    --snapshot-gu <dir>/snap_gu.csv --snapshot-names <dir>/snap_names.csv \
    --out-dir <dir>/out --cache-dir <dir>/wd_cache
"""
import argparse
import csv
import hashlib
import json
import math
import re
import sys
import time
import unicodedata
import urllib.parse
import urllib.request
from collections import Counter, defaultdict
from pathlib import Path
from typing import Callable

UA = {"User-Agent": "OvloUniversityEnrichment/1.0 (https://ovlo.mungji.com)"}
LANGS = ["ko", "ja", "zh", "es", "fr", "de", "pt", "ru", "vi"]
NAME_SOURCE = "wikidata_ror"
# 이 출처의 기존 이름은 덮어쓰지 않는다(사람이 넣었거나 교환대학 원본).
PROTECTED_SOURCES = {"manual", "exchange"}
WD_BATCH = 50
SPARQL_BATCH = 200
MIN_NAME_SIM_FOR_DOMAIN = 0.34
MIN_NAME_SIM_FOR_SUFFIX = 0.6
MAX_COORD_DISTANCE_KM = 150
AMBIGUITY_MARGIN = 0.5
# 이름 유사도 0.5 미만 도메인 매칭 96건을 사람이 검토해 걸러낸 행(2026-10-27). 키는 운영 global_universities.id.
REJECTED_MATCHES = {
    "1244": "마스터 domain 이 다른 학교(westliberty.edu) — WVU Parkersburg ≠ West Liberty University",
    "5494": "단과대 행이 본교 레코드로 매칭됨 (Riara University School of Business and Law)",
    "2697": "이름이 일반적이라 Cali 의 Bellas Artes 인지 불확실",
    "9861": "학생 포털 도메인(aluno.ifsp.edu.br)으로 매칭 — 이름이 일반적이라 IFSP 인지 불확실",
}
# 화면에 바로 보이는 언어만 검사한다. 라틴 문자가 섞인 이름은 음역이 덜 된 라벨이고,
# QID 가 다른 학교와 같은 이름은 상위 기관·통합 전 학교 라벨일 가능성이 높다(예: 밀양대 → 부산대학교).
LATIN_CHECK_LANGS = {"ko"}
DUPLICATE_CHECK_LANGS = {"ko"}
LATIN_RE = re.compile(r"[A-Za-z]")

ZH_TRADITIONAL_COUNTRIES = {"TW", "HK", "MO"}
LANG_FALLBACKS = {
    "pt": ["pt", "pt-br"],
    "es": ["es"],
}
SCRIPT_PATTERNS = {
    "ko": re.compile(r"[가-힣]"),
    "ja": re.compile(r"[぀-ヿ一-鿿]"),
    "zh": re.compile(r"[一-鿿]"),
    "ru": re.compile(r"[Ѐ-ӿ]"),
}
STOPWORDS = {"the", "of", "and", "de", "la", "el", "du", "des", "di", "da", "der", "und", "for", "at", "in"}


# ── 정규화 ──────────────────────────────────────────────────────────────

def normalize_name(name: str | None) -> str:
    s = unicodedata.normalize("NFKD", name or "")
    s = "".join(ch for ch in s if not unicodedata.combining(ch)).lower()
    s = s.replace("&", " and ")
    s = re.sub(r"[^a-z0-9]+", " ", s)
    return " ".join(s.split())


def name_tokens(name: str) -> set[str]:
    return set(normalize_name(name).split()) - STOPWORDS


def name_similarity(a: str, b: str) -> float:
    ta, tb = name_tokens(a), name_tokens(b)
    if not ta or not tb:
        return 0.0
    return len(ta & tb) / len(ta | tb)


def host_of(value: str | None) -> str:
    if not value:
        return ""
    v = value.strip().lower()
    if "://" not in v:
        v = "http://" + v
    host = urllib.parse.urlsplit(v).hostname or ""
    return host[4:] if host.startswith("www.") else host


def host_suffixes(host: str) -> list[str]:
    """'student.eit.edu.au' → ['student.eit.edu.au', 'eit.edu.au', 'edu.au'].

    마지막 2라벨(공개 접미사일 수 있음)은 ROR 쪽 호스트와 정확히 같을 때만 매칭되므로
    'edu.au' 같은 값이 ROR 인덱스에 없으면 무해하다.
    """
    parts = host.split(".")
    return [".".join(parts[i:]) for i in range(len(parts) - 1)]


# ── ROR 인덱스 ──────────────────────────────────────────────────────────

def ror_short_id(ror_url: str) -> str:
    return ror_url.rsplit("/", 1)[-1]


def ror_wikidata_qid(rec: dict) -> str | None:
    for ext in rec.get("external_ids", []):
        if ext["type"] == "wikidata":
            return ext.get("preferred") or (ext["all"][0] if ext["all"] else None)
    return None


def ror_country(rec: dict) -> str | None:
    locs = rec.get("locations") or []
    return locs[0]["geonames_details"].get("country_code") if locs else None


def ror_city_coord(rec: dict) -> tuple[float, float] | None:
    locs = rec.get("locations") or []
    g = locs[0]["geonames_details"] if locs else {}
    return (g["lat"], g["lng"]) if g.get("lat") is not None and g.get("lng") is not None else None


def distance_km(a: tuple[float, float], b: tuple[float, float]) -> float:
    lat1, lon1, lat2, lon2 = map(math.radians, (*a, *b))
    h = math.sin((lat2 - lat1) / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin((lon2 - lon1) / 2) ** 2
    return 6371 * 2 * math.asin(math.sqrt(h))


def ror_hosts(rec: dict) -> set[str]:
    hosts = {d.lower().removeprefix("www.") for d in rec.get("domains", [])}
    hosts |= {host_of(l["value"]) for l in rec.get("links", []) if l["type"] == "website"}
    return {h for h in hosts if h and "." in h}


def build_ror_index(path: str) -> tuple[dict, dict]:
    records = json.loads(Path(path).read_text())
    by_host = defaultdict(list)
    by_name = defaultdict(list)
    for rec in records:
        if rec["status"] == "withdrawn":
            continue
        for h in ror_hosts(rec):
            by_host[h].append(rec)
        if "education" in rec["types"]:
            for n in rec["names"]:
                if "acronym" not in n["types"]:
                    by_name[(normalize_name(n["value"]), ror_country(rec))].append(rec)
    return by_host, by_name


# ── 매칭 ────────────────────────────────────────────────────────────────

def best_name_sim(name_en: str, rec: dict) -> float:
    return max((name_similarity(name_en, n["value"]) for n in rec["names"] if "acronym" not in n["types"]),
               default=0.0)


def score_candidate(row: dict, rec: dict, host_kind: str) -> tuple[float, float]:
    sim = best_name_sim(row["name_en"], rec)
    score = sim * 3 + (3 if host_kind == "exact" else 2)
    score += 0.5 if "education" in rec["types"] else 0
    score += 0.5 if rec["status"] == "active" else 0
    return score, sim


def match_by_domain(row: dict, by_host: dict) -> tuple[tuple | None, str]:
    master_hosts = sorted({h for h in (host_of(row["domain"]), host_of(row["website"])) if h})
    # 정확 일치를 먼저 본다 — 같은 ROR 레코드가 한 호스트에선 exact, 다른 호스트에선 suffix 로
    # 잡힐 때 순회 순서(set 해시)에 따라 결과가 달라지지 않게 한다.
    probes = sorted(((i, suffix) for mh in master_hosts for i, suffix in enumerate(host_suffixes(mh))),
                    key=lambda p: (p[0] > 0, p[1]))
    seen, cands = set(), []
    for i, suffix in probes:
        for rec in by_host.get(suffix, []):
            if rec["id"] in seen or ror_country(rec) != row["country_code"]:
                continue
            seen.add(rec["id"])
            kind = "exact" if i == 0 else "suffix"
            score, sim = score_candidate(row, rec, kind)
            cands.append((score, sim, rec, kind))
    # 하위 도메인을 잘라 상위 호스트로 붙은 후보는 상위 기관(대학 시스템 본부 등)일 수 있어 기준을 높인다.
    cands = [c for c in cands
             if c[1] >= (MIN_NAME_SIM_FOR_DOMAIN if c[3] == "exact" else MIN_NAME_SIM_FOR_SUFFIX)]
    if not cands:
        return None, "no_domain_candidate"
    cands.sort(key=lambda c: c[0], reverse=True)
    if len(cands) > 1 and cands[0][0] - cands[1][0] < AMBIGUITY_MARGIN:
        return None, "ambiguous_domain"
    score, sim, rec, kind = cands[0]
    return (score, sim, rec), f"domain_{kind}"


def match_by_name(row: dict, by_name: dict) -> tuple[tuple | None, str]:
    cands = by_name.get((normalize_name(row["name_en"]), row["country_code"]), [])
    unique = {r["id"]: r for r in cands}
    if len(unique) == 1:
        rec = next(iter(unique.values()))
        return (0.0, 1.0, rec), "name_exact"
    return None, "ambiguous_name" if unique else "no_match"


def match_all(rows: list[dict], by_host: dict, by_name: dict) -> dict[str, dict]:
    results = {}
    for row in rows:
        if row["id"] in REJECTED_MATCHES:
            results[row["id"]] = {"method": "rejected_manual"}
            continue
        cand, method = match_by_domain(row, by_host)
        if cand is None and method != "ambiguous_domain":
            cand, method = match_by_name(row, by_name)
        if cand is None:
            results[row["id"]] = {"method": method}
            continue
        _, sim, rec = cand
        results[row["id"]] = {
            "method": method,
            "name_sim": round(sim, 2),
            "ror_id": ror_short_id(rec["id"]),
            "ror_name": next((n["value"] for n in rec["names"] if "ror_display" in n["types"]), ""),
            "ror_status": rec["status"],
            "qid": ror_wikidata_qid(rec),
            "geonames_id": (rec.get("locations") or [{}])[0].get("geonames_id"),
            "ror_city_coord": ror_city_coord(rec),
        }
    return results


# ── Wikidata ────────────────────────────────────────────────────────────

def http_json(url: str, data: bytes | None = None) -> dict:
    for attempt in range(4):
        try:
            req = urllib.request.Request(url, data=data, headers={**UA, "Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=60) as resp:
                return json.load(resp)
        except Exception as e:  # 네트워크·429 는 재시도, 끝내 실패하면 중단(조용히 빈 결과로 넘기지 않음)
            if attempt == 3:
                raise RuntimeError(f"Wikidata 요청 실패: {url[:120]}") from e
            time.sleep(2 ** attempt)


def batch_cache_key(kind: str, batch: list[str]) -> str:
    """배치 내용 전체로 캐시 키를 만든다.

    위치·첫 QID 만으로 키를 만들면 스냅샷이 바뀌어 배치 구성이 달라져도 옛 응답을 읽어
    라벨·좌표가 조용히 빠진다.
    """
    digest = hashlib.sha256("|".join(batch).encode()).hexdigest()[:16]
    return f"{kind}_{digest}"


def cached(cache_dir: Path, key: str, fetch: Callable[[], dict]) -> dict:
    path = Path(cache_dir) / f"{key}.json"
    if path.exists():
        return json.loads(path.read_text())
    value = fetch()
    path.write_text(json.dumps(value, ensure_ascii=False))
    return value


def label_lang_codes() -> list[str]:
    codes = set(LANGS) | {"zh-hans", "zh-cn", "zh-hant", "zh-tw", "zh-hk", "pt-br"}
    return sorted(codes)


def fetch_labels(qids: list[str], cache_dir: Path) -> dict[str, dict[str, str]]:
    labels = {}
    for i in range(0, len(qids), WD_BATCH):
        batch = qids[i:i + WD_BATCH]
        url = "https://www.wikidata.org/w/api.php?" + urllib.parse.urlencode({
            "action": "wbgetentities", "ids": "|".join(batch), "props": "labels",
            "languages": "|".join(label_lang_codes()), "format": "json"})
        data = cached(cache_dir, batch_cache_key("labels", batch), lambda: http_json(url))
        for qid, ent in data.get("entities", {}).items():
            # 리다이렉트된 QID 는 entities 키가 새 QID 다. 요청한 QID 로도 찾을 수 있게 둘 다 기록.
            mapped = {k: v["value"] for k, v in ent.get("labels", {}).items()}
            labels[qid] = mapped
            labels[ent.get("id", qid)] = mapped
        print(f"  labels {min(i + WD_BATCH, len(qids))}/{len(qids)}", file=sys.stderr)
        time.sleep(0.3)
    return labels


def fetch_coords_and_ror(qids: list[str], cache_dir: Path) -> dict[str, dict]:
    """P625(좌표)와 P6782(ROR ID 역참조)를 SPARQL 로 가져온다."""
    out = defaultdict(lambda: {"coords": [], "ror": set()})
    for i in range(0, len(qids), SPARQL_BATCH):
        batch = qids[i:i + SPARQL_BATCH]
        query = ("SELECT ?item ?coord ?ror WHERE { VALUES ?item { %s } "
                 "OPTIONAL { ?item p:P625 ?st . ?st ps:P625 ?coord . ?st wikibase:rank ?rank . "
                 "FILTER(?rank != wikibase:DeprecatedRank) } "
                 "OPTIONAL { ?item wdt:P6782 ?ror } }") % " ".join(f"wd:{q}" for q in batch)
        body = urllib.parse.urlencode({"query": query, "format": "json"}).encode()
        data = cached(cache_dir, batch_cache_key("sparql", batch),
                      lambda: http_json("https://query.wikidata.org/sparql", data=body))
        for b in data["results"]["bindings"]:
            qid = b["item"]["value"].rsplit("/", 1)[-1]
            entry = out[qid]
            if "coord" in b:
                m = re.match(r"Point\(([-\d.eE]+) ([-\d.eE]+)\)", b["coord"]["value"])
                if m and (float(m.group(2)), float(m.group(1))) not in entry["coords"]:
                    entry["coords"].append((float(m.group(2)), float(m.group(1))))  # (lat, lon)
            if "ror" in b:
                entry["ror"].add(b["ror"]["value"])
        print(f"  sparql {min(i + SPARQL_BATCH, len(qids))}/{len(qids)}", file=sys.stderr)
        time.sleep(1.0)
    return out


def same_text(a: str, b: str) -> bool:
    return " ".join(a.casefold().split()) == " ".join(b.casefold().split())


def pick_label(lang: str, labels: dict[str, str], country_code: str,
               name_en: str) -> tuple[str | None, str]:
    if lang == "zh":
        order = (["zh-hant", "zh-tw", "zh-hk", "zh"] if country_code in ZH_TRADITIONAL_COUNTRIES
                 else ["zh-hans", "zh-cn", "zh"])
    else:
        order = LANG_FALLBACKS.get(lang, [lang])
    for code in order:
        value = (labels.get(code) or "").strip()
        if not value:
            continue
        pattern = SCRIPT_PATTERNS.get(lang)
        if pattern and not pattern.search(value):
            return None, "script_mismatch"
        # 영문명을 그대로 옮긴 라벨은 정보가 없다(화면은 어차피 name_en 으로 폴백).
        # 넣어 두면 나중에 진짜 번역이 생겨도 refresh 로 분류돼 자동 교체되지 않는다.
        if same_text(value, name_en):
            return None, "same_as_en"
        return value, code
    return None, "missing"


# ── diff · SQL ──────────────────────────────────────────────────────────

def plan_name_change(current: dict | None, proposed: str, qid: str) -> str:
    if current is None:
        return "insert"
    if current["name"] == proposed and current["qid"] == qid:
        return "unchanged"
    if current["source"] in PROTECTED_SOURCES:
        return "keep_protected"
    if current["source"] == "wikidata_check":
        return "replace_suspicious"
    if current["source"] in ("wikidata", NAME_SOURCE) and current["qid"] == qid:
        return "refresh"
    return "conflict_review"  # 다른 QID 로 들어간 기존 이름 — 자동 교체하지 않는다


# refresh(같은 QID, 라벨만 바뀜)는 자동 적용하지 않는다 — 최신 라벨이 더 나쁜 경우가 있다
# (예: zh-hans 라벨이 대학이 아니라 주 이름). names_diff.csv 에서 검토 후 수동 반영.
APPLIED_ACTIONS = {"insert", "replace_suspicious"}


def name_owners(current_names: dict, proposals: list[tuple]) -> dict[tuple[str, str], set]:
    """(lang, 이름) → 그 이름을 쓰거나 쓰게 될 대학 id 집합."""
    owners = defaultdict(set)
    for (uid, lang), cur in current_names.items():
        if lang in DUPLICATE_CHECK_LANGS:
            owners[(lang, cur["name"])].add(uid)
    for uid, lang, value, action in proposals:
        if lang in DUPLICATE_CHECK_LANGS and action in APPLIED_ACTIONS:
            owners[(lang, value)].add(uid)
    return owners


def review_action(action: str, uid: str, lang: str, value: str, owners: dict) -> str:
    """적용 예정 이름이 화면 품질 검사에 걸리면 검토 action 으로 바꾼다."""
    if action not in APPLIED_ACTIONS:
        return action
    if lang in LATIN_CHECK_LANGS and LATIN_RE.search(value):
        return "latin_review"
    # QID 가 같아도 검토로 보낸다 — ROR 이 통합 전 학교를 별칭으로 품고 있으면
    # 다른 학교 행이 같은 QID 로 매칭된다(예: 밀양대 → 부산대 레코드).
    if any(o_id != uid for o_id in owners.get((lang, value), ())):
        return "duplicate_review"
    return action


def sql_str(v: object) -> str:
    return "NULL" if v is None or v == "" else "'" + str(v).replace("'", "''") + "'"


def sql_num(v: object) -> str:
    return "NULL" if v is None or v == "" else str(v)


def write_apply_sql(path: Path, gu_updates: list[dict], name_rows: list[dict], meta: dict) -> None:
    lines = [
        "-- global_universities ROR/Wikidata 보강 (generated by scripts/enrich_ror_wikidata.py)",
        f"-- ROR dump: {meta['ror_dump']} · snapshot rows: {meta['rows']} · generated: {meta['generated']}",
        "-- 실행 전: global_universities·global_university_names 를 pg_dump 로 백업하고(GLOBAL-PIT-063),",
        "--         V31 마이그레이션(ror_id 등 컬럼)이 적용됐는지 확인한다. 아래 가드가 없으면 즉시 실패한다.",
        "\\set ON_ERROR_STOP on",
        "BEGIN;",
        "DO $$ BEGIN",
        "  IF NOT EXISTS (SELECT 1 FROM information_schema.columns",
        "                 WHERE table_name='global_universities' AND column_name='ror_id') THEN",
        "    RAISE EXCEPTION 'V31 미적용: global_universities.ror_id 없음';",
        "  END IF;",
        "END $$;",
        "",
        "-- 1) 외부 ID · ROR 상태 · 좌표 (이미 값이 있으면 건드리지 않는다)",
    ]
    for i in range(0, len(gu_updates), 500):
        chunk = gu_updates[i:i + 500]
        values = ",\n  ".join(
            f"({u['id']}, {sql_str(u['ror_id'])}, {sql_str(u['qid'])}, {sql_num(u['geonames_id'])}, "
            f"{sql_str(u['ror_status'])}, {sql_num(u['lat'])}::float8, {sql_num(u['lon'])}::float8)"
            for u in chunk)
        lines += [
            "UPDATE global_universities g SET",
            "  ror_id       = COALESCE(g.ror_id, v.ror_id),",
            "  wikidata_qid = COALESCE(g.wikidata_qid, v.qid),",
            "  geonames_id  = COALESCE(g.geonames_id, v.geonames_id),",
            "  ror_status   = v.ror_status,",
            "  latitude     = CASE WHEN g.latitude IS NULL AND v.lat IS NOT NULL THEN v.lat ELSE g.latitude END,",
            "  longitude    = CASE WHEN g.latitude IS NULL AND v.lat IS NOT NULL THEN v.lon ELSE g.longitude END,",
            "  coord_source = CASE WHEN g.latitude IS NULL AND v.lat IS NOT NULL THEN 'wikidata' ELSE g.coord_source END",
            "FROM (VALUES",
            "  " + values,
            ") AS v(id, ror_id, qid, geonames_id, ror_status, lat, lon)",
            "WHERE g.id = v.id;",
            "",
        ]
    lines.append("-- 2) 다국어 이름 (보호 출처·다른 QID 로 들어간 기존 값은 DB 쪽 가드로도 막는다)")
    for i in range(0, len(name_rows), 1000):
        chunk = name_rows[i:i + 1000]
        values = ",\n  ".join(
            f"({r['id']}, {sql_str(r['lang'])}, {sql_str(r['name'])}, '{NAME_SOURCE}', {sql_str(r['qid'])})"
            for r in chunk)
        lines += [
            "INSERT INTO global_university_names (global_univ_id, lang, name, source, qid) VALUES",
            "  " + values,
            "ON CONFLICT (global_univ_id, lang) DO UPDATE",
            "  SET name = EXCLUDED.name, source = EXCLUDED.source, qid = EXCLUDED.qid",
            "  WHERE global_university_names.source = 'wikidata_check'",
            f"     OR (global_university_names.source IN ('wikidata', '{NAME_SOURCE}')",
            "         AND global_university_names.qid IS NOT DISTINCT FROM EXCLUDED.qid);",
            "",
        ]
    lines += [
        "-- 3) 사후 확인 — 기대값에 못 미치면 예외로 트랜잭션을 중단시킨다(COMMIT 불가).",
        "--    ON CONFLICT 가드에 걸려 조용히 건너뛴 이름 행(스냅샷이 낡은 경우)도 여기서 드러난다.",
        "DO $$ DECLARE n_ror bigint; n_names bigint; BEGIN",
        "  SELECT count(ror_id) INTO n_ror FROM global_universities;",
        f"  SELECT count(*) INTO n_names FROM global_university_names WHERE source = '{NAME_SOURCE}';",
        f"  IF n_ror < {meta['expect_ror']} OR n_names < {meta['expect_names']} THEN",
        f"    RAISE EXCEPTION '사후 확인 실패: ror_id % (기대 >= {meta['expect_ror']}), "
        f"{NAME_SOURCE} 이름 % (기대 >= {meta['expect_names']})', n_ror, n_names;",
        "  END IF;",
        "END $$;",
        "SELECT count(ror_id) AS with_ror, count(wikidata_qid) AS with_qid, count(latitude) AS with_coords",
        "  FROM global_universities;",
        "SELECT lang, source, count(*) FROM global_university_names GROUP BY lang, source ORDER BY lang, source;",
        "-- 확인 후 수동으로 COMMIT; (기본은 열린 트랜잭션으로 남겨 둔다)",
    ]
    Path(path).write_text("\n".join(lines) + "\n")


def write_csv(path: Path, header: list[str], rows: list[list]) -> None:
    with open(path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(header)
        w.writerows(rows)


# ── main ────────────────────────────────────────────────────────────────

def load_snapshot(gu_path: str, names_path: str) -> tuple[list[dict], dict[tuple[str, str], dict]]:
    with open(gu_path, newline="") as f:
        rows = list(csv.DictReader(f))
    current = {}
    with open(names_path, newline="") as f:
        for r in csv.DictReader(f):
            current[(r["global_univ_id"], r["lang"])] = r
    return rows, current


def build_plan(rows: list[dict], matches: dict[str, dict], current_names: dict, labels: dict,
               wd_extra: dict) -> tuple[list[dict], list[dict], list[list], list[list], Counter]:
    gu_updates, name_rows, name_diff, coord_rows, candidates = [], [], [], [], []
    stats = Counter()
    for row in rows:
        m = matches[row["id"]]
        stats[f"match:{m['method']}"] += 1
        if "ror_id" not in m:
            continue
        qid = m["qid"]
        extra = wd_extra.get(qid, {"coords": [], "ror": set()}) if qid else {"coords": [], "ror": set()}
        if qid and extra["ror"] and m["ror_id"] not in extra["ror"]:
            stats["qid_ror_backref_mismatch"] += 1
            m["qid_flag"] = "ror_backref_mismatch"
        lat = lon = None
        if not row["latitude"] and len(extra["coords"]) == 1:
            coord = extra["coords"][0]
            city = m.get("ror_city_coord")
            dist = round(distance_km(coord, city)) if city else None
            # ROR 도시에서 너무 멀면 QID·좌표 중 하나가 틀렸을 수 있다(분교 캠퍼스일 수도) — 검토로 보낸다.
            if dist is not None and dist > MAX_COORD_DISTANCE_KM:
                stats["coords_far_from_ror_city_review"] += 1
                coord_rows.append([row["id"], row["name_en"], qid, *coord, dist, "review_far"])
            else:
                lat, lon = coord
                coord_rows.append([row["id"], row["name_en"], qid, lat, lon, dist, "apply"])
        elif not row["latitude"] and len(extra["coords"]) > 1:
            stats["coords_multiple_skipped"] += 1
        existing_qids = {current_names[(row["id"], lang)]["qid"] for lang in LANGS
                         if (row["id"], lang) in current_names and current_names[(row["id"], lang)]["qid"]}
        if qid and existing_qids - {qid}:
            # 기존 이름이 다른 엔티티를 가리킨다 — 어느 쪽이 맞는지 사람이 봐야 하므로
            # QID·좌표·이름을 모두 보류하고 ROR ID 만 기록한다.
            stats["university_qid_conflict_review"] += 1
            m["qid_flag"] = "existing_qid_conflict:" + "|".join(sorted(existing_qids))
            gu_updates.append({**m, "id": row["id"], "qid": None, "lat": None, "lon": None})
            if coord_rows and coord_rows[-1][0] == row["id"]:
                coord_rows[-1] = [*coord_rows[-1][:-1], "review_qid_conflict"]
            continue
        gu_updates.append({**m, "id": row["id"], "lat": lat, "lon": lon})
        if not qid or m.get("qid_flag"):
            continue
        ent_labels = labels.get(qid, {})
        for lang in LANGS:
            value, how = pick_label(lang, ent_labels, row["country_code"], row["name_en"])
            if value is None:
                stats[f"label_{how}:{lang}"] += 1
                continue
            cur = current_names.get((row["id"], lang))
            candidates.append((row, lang, cur, value, how, qid, plan_name_change(cur, value, qid)))
    # 중복 검사는 모든 대학의 제안이 모인 뒤에야 할 수 있다.
    owners = name_owners(current_names, [(r["id"], lang, value, action)
                                         for r, lang, _, value, _, _, action in candidates])
    for row, lang, cur, value, how, qid, base_action in candidates:
        action = review_action(base_action, row["id"], lang, value, owners)
        stats[f"name_{action}"] += 1
        name_diff.append([row["id"], row["name_en"], lang, cur["name"] if cur else "",
                          cur["source"] if cur else "", value, how, action])
        if action in APPLIED_ACTIONS:
            name_rows.append({"id": row["id"], "lang": lang, "name": value, "qid": qid})
    return gu_updates, name_rows, name_diff, coord_rows, stats


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--ror-json", required=True)
    ap.add_argument("--snapshot-gu", required=True)
    ap.add_argument("--snapshot-names", required=True)
    ap.add_argument("--out-dir", required=True)
    ap.add_argument("--cache-dir", required=True)
    args = ap.parse_args()
    out, cache = Path(args.out_dir), Path(args.cache_dir)
    out.mkdir(parents=True, exist_ok=True)
    cache.mkdir(parents=True, exist_ok=True)

    rows, current_names = load_snapshot(args.snapshot_gu, args.snapshot_names)
    print(f"snapshot: {len(rows)} universities, {len(current_names)} names", file=sys.stderr)
    by_host, by_name = build_ror_index(args.ror_json)
    matches = match_all(rows, by_host, by_name)

    qids = sorted({m["qid"] for m in matches.values() if m.get("qid")})
    print(f"matched qids: {len(qids)}", file=sys.stderr)
    labels = fetch_labels(qids, cache)
    wd_extra = fetch_coords_and_ror(qids, cache)

    gu_updates, name_rows, name_diff, coord_rows, stats = build_plan(
        rows, matches, current_names, labels, wd_extra)

    ror_dup = Counter(u["ror_id"] for u in gu_updates)
    write_csv(out / "matches.csv",
              ["id", "name_en", "country_code", "domain", "method", "name_sim", "ror_id", "ror_name",
               "ror_status", "qid", "qid_flag", "shared_ror_count"],
              [[r["id"], r["name_en"], r["country_code"], r["domain"], matches[r["id"]]["method"],
                matches[r["id"]].get("name_sim", ""), matches[r["id"]].get("ror_id", ""),
                matches[r["id"]].get("ror_name", ""), matches[r["id"]].get("ror_status", ""),
                matches[r["id"]].get("qid", ""), matches[r["id"]].get("qid_flag", ""),
                ror_dup.get(matches[r["id"]].get("ror_id"), "")] for r in rows])
    write_csv(out / "names_diff.csv",
              ["id", "name_en", "lang", "current_name", "current_source", "proposed_name", "wd_lang", "action"],
              name_diff)
    write_csv(out / "coords.csv", ["id", "name_en", "qid", "lat", "lon", "km_from_ror_city", "action"], coord_rows)

    stats["universities"] = len(rows)
    stats["gu_rows_to_update"] = len(gu_updates)
    stats["name_rows_to_write"] = len(name_rows)
    stats["coords_to_fill"] = sum(1 for c in coord_rows if c[-1] == "apply")
    stats["ror_shared_by_multiple_rows"] = sum(1 for c in ror_dup.values() if c > 1)
    write_apply_sql(out / "apply.sql", gu_updates, name_rows, {
        "ror_dump": Path(args.ror_json).name, "rows": len(rows),
        "generated": time.strftime("%Y-%m-%d %H:%M:%S"),
        "expect_ror": len(gu_updates), "expect_names": len(name_rows)})
    (out / "summary.json").write_text(json.dumps(dict(sorted(stats.items())), ensure_ascii=False, indent=2))
    print(json.dumps(dict(sorted(stats.items())), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
