"""Regression tests for check_report.py: each attack must be rejected, the clean copy must pass.
Usage: python3 -I gate_regression.py  (uses out/70 and out/59 as fixtures; writes under _gate_tests/)"""
import copy, json, re, shutil, subprocess, sys
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
import audit_stage1  # noqa: E402  (resolves OVLO_AUDIT_DIR)

HERE = audit_stage1.HERE / "regen"
TMP = HERE / "_gate_tests"


def run(rid, mutate, name, expect_error):
    d = TMP / name / str(rid)
    shutil.rmtree(d.parent, ignore_errors=True)
    d.mkdir(parents=True)
    for f in ("report.json", "claims.jsonl", "research_log.md"):
        shutil.copy(HERE / "out" / str(rid) / f, d / f)
    rep = json.loads((d / "report.json").read_text(encoding="utf-8"))
    claims = (d / "claims.jsonl").read_text(encoding="utf-8")
    rep, claims = mutate(copy.deepcopy(rep), claims)
    (d / "report.json").write_text(json.dumps(rep, ensure_ascii=False), encoding="utf-8")
    (d / "claims.jsonl").write_text(claims, encoding="utf-8")
    # check_report resolves packs/<rid> next to itself, so run it on the copied path
    out = subprocess.run([sys.executable, "-I", str(SCRIPTS / "check_report.py"), str(d / "report.json")],
                         capture_output=True, text=True).stdout
    failed = "ERROR" in out
    ok = failed == expect_error
    print(("PASS" if ok else "FAIL"), name, "| expected", "reject" if expect_error else "accept",
          "|", out.strip().splitlines()[-1], ("" if ok else "\n" + out[:600]))
    return ok


def body_sub(lang, old, new):
    def m(rep, claims):
        assert old in rep[lang]["body"], (lang, old)
        rep[lang]["body"] = rep[lang]["body"].replace(old, new, 1)
        return rep, claims
    return m


def en_copied(rep, claims):
    for lang in ("de", "fr", "ko", "ja", "zh"):
        rep[lang] = copy.deepcopy(rep["en"])
    return rep, claims


def cut_sections(rep, claims):
    parts = re.split(r"(?=^## )", rep["de"]["body"], flags=re.M)
    rep["de"]["body"] = "".join(parts[:3] + parts[-1:])
    return rep, claims


def summary_add(rep, claims):
    rep["en"]["summary"] += " Rent is about €999."
    return rep, claims


def null_leaf(rep, claims):
    rep["fr"]["content"]["visa"]["cost"] = None
    return rep, claims


def trad_zh(rep, claims):
    rep["zh"]["summary"] += "（慕尼黑工業大學）"
    return rep, claims


def bogus_tag(rep, claims):
    first = json.loads(claims.splitlines()[0])
    first["sources"] = ["X"]
    return rep, json.dumps(first, ensure_ascii=False) + "\n" + "\n".join(claims.splitlines()[1:]) + "\n"


def f1_everywhere(rep, claims):
    for lang in rep:
        rep[lang]["body"] = rep[lang]["body"].replace("## ", "The F1 Grand Prix weekend is busy.\n\n## ", 1)
    return rep, claims


def foreign_price(tag):
    def m(rep, claims):
        for lang in rep:
            rep[lang]["body"] = rep[lang]["body"].replace("## ", "USD 1,400 dorm.\n\n## ", 1)
        claim = {"section": "Housing", "claim": "Dorm fee USD 1,400 per semester.", "sources": [tag],
                 "quote": "Dormitory fee: USD 1,400 per semester"}
        return rep, claims + json.dumps(claim) + "\n"
    return m


def ko_plain(rep, claims):
    rep["ko"]["body"] = rep["ko"]["body"].replace("## ", "교환학생은 기숙사에 산다. 학기는 3월에 시작한다. 수업은 영어로 한다.\n\n## ", 1)
    return rep, claims


TMP.mkdir(exist_ok=True)
results = [
    run(70, lambda r, c: (r, c), "clean_copy", False),
    run(70, en_copied, "H1_en_copied_into_translations", True),
    run(70, cut_sections, "H2_de_sections_dropped", True),
    run(70, body_sub("de", "992", "1.900"), "H3_de_amount_changed_to_year_like", True),
    run(70, body_sub("de", "## ", "Gebühr 90 USD und 100 £.\n\n## "), "H4_foreign_amounts_matching_fact_numbers", True),
    run(70, summary_add, "H6_unbacked_amount_in_summary", True),
    run(70, body_sub("de", "## ", "<think>plan</think>\n\n## "), "H7_think_tag", True),
    run(70, null_leaf, "H2_content_leaf_nulled", True),
    run(70, trad_zh, "zh_traditional_chars", True),
    run(70, bogus_tag, "claim_bogus_source_tag", True),
    run(70, f1_everywhere, "F4_formula1_is_not_a_visa_term", False),
    run(70, body_sub("de", "Sie ", "du "), "de_informal_du_rejected", True),
    run(70, ko_plain, "ko_plain_register_rejected", True),
    run(70, foreign_price("O1"), "official_page_foreign_price_allowed", False),
    run(70, foreign_price("V1"), "video_foreign_price_rejected", True),
]
print(f"{sum(results)}/{len(results)} passed")
shutil.rmtree(TMP, ignore_errors=True)
sys.exit(0 if all(results) else 1)
