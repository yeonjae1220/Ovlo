"""Generate review/apply/rollback SQL that publishes the regenerated pilot reports.

Usage:
  python3 -I gen_apply_sql.py <current_dir> <out_dir> --run-id r20261029_pilot --reports 70,1,... --unpublish 1050

<current_dir> holds a fresh READ ONLY export taken right before generation:
  translations.jsonl  rows of university_report_translation for the pilot reports (row_to_json)
  reports.jsonl       all rows of university_report (row_to_json)

What the SQL does in one transaction (apply.sql ends with COMMIT, review.sql with ROLLBACK):
  1. pilot translations (7 languages each) ← out/<id>/report.json (title, summary, body, content)
  2. pilot report rows: source counts from content.sources, source_video_ids cleared, cost fields from en,
     aggregate_stats cleared (it held the old LLM ratings)
  3. every report: avg_rating / recommend_ratio / avg_difficulty → NULL and rating keys dropped from aggregate_stats
     (they were assigned by the old 8B pipeline, not measured)
  4. --unpublish reports → status DRAFT (duplicates of a canonical university)
Guards: aborts if any touched row differs from the export (someone edited it meanwhile), backs up every
touched row before/after into report_regen_backup_<run>_tr / _ur, and re-reads the stored rows to
compare with the intended values before finishing. Free text is never regex-replaced in the DB.
"""
import argparse
import json
import re
import sys
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
import audit_stage1  # noqa: E402  (resolves OVLO_AUDIT_DIR)

HERE = audit_stage1.HERE / "regen"
LANGS = ["en", "ko", "ja", "zh", "de", "fr", "vi"]
RATING_KEYS = ["avg_rating", "recommend_ratio", "rating_dist", "avg_difficulty"]
# columns compared by the snapshot guard (updated_at is excluded — the trigger changes it)
REPORT_GUARD = ["status", "source_video_count", "source_web_count", "source_video_ids", "avg_rating",
                "recommend_ratio", "avg_difficulty", "avg_cost_monthly", "cost_currency", "aggregate_stats",
                "supported_langs", "global_univ_id", "exchange_univ_id"]


def literal(value):
    if value is None:
        return "NULL"
    if not isinstance(value, str):
        value = json.dumps(value, ensure_ascii=False)
    return "'" + value.replace("'", "''") + "'"


def jl(path):
    with open(path, encoding="utf-8") as f:
        return [json.loads(line) for line in f if line.strip()]


def check_reports_pass(report_ids):
    import subprocess
    for rid in report_ids:
        out = subprocess.run([sys.executable, "-I", str(SCRIPTS / "check_report.py"), str(HERE / "out" / str(rid) / "report.json")],
                             capture_output=True, text=True).stdout
        if not out.strip().splitlines()[-1].startswith("0 errors"):
            raise SystemExit(f"report {rid} does not pass check_report.py:\n{out}")


def report_updates(report, regen):
    """Intended values for one university_report row."""
    new = {"avg_rating": None, "recommend_ratio": None, "avg_difficulty": None}
    stats = report.get("aggregate_stats")
    if isinstance(stats, dict):
        stats = {k: v for k, v in stats.items() if k not in RATING_KEYS}
    new["aggregate_stats"] = stats
    if regen is not None:
        en = regen["en"]["content"]
        sources = en.get("sources") or []
        new["source_video_count"] = sum(1 for s in sources if s.get("type") == "video")
        new["source_web_count"] = sum(1 for s in sources if s.get("type") != "video")
        new["source_video_ids"] = []
        monthly = (en.get("costs") or {}).get("monthly_total")
        new["avg_cost_monthly"] = monthly[:100] if isinstance(monthly, str) else None
        new["cost_currency"] = (en.get("costs") or {}).get("currency")
        new["aggregate_stats"] = None
    return new


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("current_dir")
    ap.add_argument("out_dir")
    ap.add_argument("--run-id", required=True)
    ap.add_argument("--reports", required=True)
    ap.add_argument("--unpublish", default="")
    args = ap.parse_args()
    if not re.fullmatch(r"[a-z][a-z0-9_]{0,30}", args.run_id):
        raise SystemExit("invalid run id")
    pilot = [int(x) for x in args.reports.split(",") if x]
    unpublish = [int(x) for x in args.unpublish.split(",") if x]
    check_reports_pass(pilot)

    cur = Path(args.current_dir)
    current_tr = {(t["report_id"], t["lang"]): t for t in jl(cur / "translations.jsonl")}
    reports = {r["id"]: r for r in jl(cur / "reports.jsonl")}
    regen = {rid: json.loads((HERE / "out" / str(rid) / "report.json").read_text(encoding="utf-8")) for rid in pilot}

    for rid in pilot + unpublish:
        if rid not in reports:
            raise SystemExit(f"report {rid} not in export")
    for rid in pilot:
        for lang in LANGS:
            if (rid, lang) not in current_tr:
                raise SystemExit(f"translation {rid}/{lang} not in export")
            if lang not in regen[rid]:
                raise SystemExit(f"regenerated report {rid} lacks {lang}")
        if set(reports[rid]["supported_langs"]) != set(LANGS):
            raise SystemExit(f"report {rid} supported_langs {reports[rid]['supported_langs']} != {LANGS}")

    tr_rows, ur_rows = [], []
    for rid in pilot:
        for lang in LANGS:
            t = current_tr[(rid, lang)]
            expected = {k: t[k] for k in ("title", "summary", "body", "content")}
            n = regen[rid][lang]
            replacement = {"title": n["title"], "summary": n["summary"], "body": n["body"], "content": n["content"]}
            tr_rows.append((rid, lang, expected, replacement))
    for rid, r in sorted(reports.items()):
        expected = {k: r.get(k) for k in REPORT_GUARD}
        new = dict(expected)
        new.update(report_updates(r, regen.get(rid)))
        if rid in unpublish:
            new["status"] = "DRAFT"
        if new != expected:
            ur_rows.append((rid, expected, new))

    bt, bu = f"report_regen_backup_{args.run_id}_tr", f"report_regen_backup_{args.run_id}_ur"
    guard_cols = ", ".join(f"'{c}', r.{c}" for c in REPORT_GUARD)
    sql = ["\\set ON_ERROR_STOP on", "BEGIN;", "SET LOCAL lock_timeout='5s';", "SET LOCAL statement_timeout='120s';",
           "SET LOCAL standard_conforming_strings=on;",
           "CREATE TEMP TABLE regen_tr (report_id bigint, lang text, expected jsonb, replacement jsonb, PRIMARY KEY(report_id, lang)) ON COMMIT DROP;",
           "CREATE TEMP TABLE regen_ur (report_id bigint PRIMARY KEY, expected jsonb, replacement jsonb) ON COMMIT DROP;"]
    for rid, lang, e, rpl in tr_rows:
        sql.append(f"INSERT INTO regen_tr VALUES ({rid}, {literal(lang)}, {literal(e)}::jsonb, {literal(rpl)}::jsonb);")
    for rid, e, rpl in ur_rows:
        sql.append(f"INSERT INTO regen_ur VALUES ({rid}, {literal(e)}::jsonb, {literal(rpl)}::jsonb);")
    sql += [
        "LOCK TABLE university_report, university_report_translation IN SHARE ROW EXCLUSIVE MODE;",
        f"""DO $$ BEGIN
 IF (SELECT count(*) FROM regen_tr) <> {len(tr_rows)} OR (SELECT count(*) FROM regen_ur) <> {len(ur_rows)} THEN
   RAISE EXCEPTION 'staging row count mismatch'; END IF;
 IF EXISTS (SELECT 1 FROM regen_tr e LEFT JOIN university_report_translation t USING (report_id, lang)
            WHERE t.report_id IS NULL
               OR jsonb_build_object('title', t.title, 'summary', t.summary, 'body', t.body, 'content', t.content) IS DISTINCT FROM e.expected) THEN
   RAISE EXCEPTION 'translation snapshot changed: export again and regenerate'; END IF;
 IF EXISTS (SELECT 1 FROM regen_ur e LEFT JOIN university_report r ON r.id = e.report_id
            WHERE r.id IS NULL OR jsonb_build_object({guard_cols}) IS DISTINCT FROM e.expected) THEN
   RAISE EXCEPTION 'report snapshot changed: export again and regenerate'; END IF;
END $$;""",
        f"CREATE TABLE {bt} (report_id bigint, lang text, before_row jsonb NOT NULL, after_row jsonb, PRIMARY KEY (report_id, lang));",
        f"CREATE TABLE {bu} (report_id bigint PRIMARY KEY, before_row jsonb NOT NULL, after_row jsonb);",
        f"INSERT INTO {bt} (report_id, lang, before_row) SELECT t.report_id, t.lang, to_jsonb(t) FROM university_report_translation t JOIN regen_tr e USING (report_id, lang);",
        f"INSERT INTO {bu} (report_id, before_row) SELECT r.id, to_jsonb(r) FROM university_report r JOIN regen_ur e ON e.report_id = r.id;",
        """UPDATE university_report_translation t
   SET title = e.replacement->>'title', summary = e.replacement->>'summary', body = e.replacement->>'body',
       content = e.replacement->'content', updated_at = now()
  FROM regen_tr e WHERE t.report_id = e.report_id AND t.lang = e.lang;""",
        """UPDATE university_report r
   SET status = e.replacement->>'status',
       source_video_count = (e.replacement->>'source_video_count')::int,
       source_web_count = (e.replacement->>'source_web_count')::int,
       source_video_ids = ARRAY(SELECT jsonb_array_elements_text(e.replacement->'source_video_ids'))::bigint[],
       avg_rating = (e.replacement->>'avg_rating')::numeric,
       recommend_ratio = (e.replacement->>'recommend_ratio')::numeric,
       avg_difficulty = (e.replacement->>'avg_difficulty')::numeric,
       avg_cost_monthly = e.replacement->>'avg_cost_monthly',
       cost_currency = e.replacement->>'cost_currency',
       aggregate_stats = CASE WHEN jsonb_typeof(e.replacement->'aggregate_stats') = 'null' THEN NULL ELSE e.replacement->'aggregate_stats' END,
       updated_at = now()
  FROM regen_ur e WHERE r.id = e.report_id;""",
        f"""DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM regen_tr e JOIN university_report_translation t USING (report_id, lang)
            WHERE jsonb_build_object('title', t.title, 'summary', t.summary, 'body', t.body, 'content', t.content) IS DISTINCT FROM e.replacement) THEN
   RAISE EXCEPTION 'stored translations differ from the reviewed replacement'; END IF;
 IF EXISTS (SELECT 1 FROM regen_ur e JOIN university_report r ON r.id = e.report_id
            WHERE jsonb_build_object({guard_cols}) IS DISTINCT FROM e.replacement) THEN
   RAISE EXCEPTION 'stored report rows differ from the intended values'; END IF;
END $$;""",
        f"UPDATE {bt} b SET after_row = to_jsonb(t) FROM university_report_translation t WHERE t.report_id = b.report_id AND t.lang = b.lang;",
        f"UPDATE {bu} b SET after_row = to_jsonb(r) FROM university_report r WHERE r.id = b.report_id;",
        f"SELECT (SELECT count(*) FROM {bt}) AS translations_backed_up, (SELECT count(*) FROM {bu}) AS reports_backed_up,"
        f" (SELECT count(*) FROM university_report WHERE avg_rating IS NOT NULL OR recommend_ratio IS NOT NULL) AS ratings_left,"
        f" (SELECT string_agg(id || ':' || status, ',') FROM university_report WHERE id IN ({','.join(map(str, unpublish)) or 'NULL'})) AS unpublished;",
        # the DB quality trigger (V28–V30) records script-mix defects; show what it flagged for the new rows
        f"SELECT report_id, lang, defects FROM report_quality_issue WHERE report_id IN ({','.join(map(str, pilot))}) ORDER BY 1, 2;",
    ]
    out = Path(args.out_dir)
    out.mkdir(parents=True, exist_ok=True)
    (out / "review.sql").write_text("\n".join(sql + ["ROLLBACK;"]) + "\n", encoding="utf-8")
    (out / "apply.sql").write_text("\n".join(sql + ["COMMIT;"]) + "\n", encoding="utf-8")
    (out / "rollback.sql").write_text(f"""\\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout='5s';
LOCK TABLE university_report, university_report_translation IN SHARE ROW EXCLUSIVE MODE;
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM {bt} b LEFT JOIN university_report_translation t USING (report_id, lang)
            WHERE t.report_id IS NULL OR to_jsonb(t) IS DISTINCT FROM b.after_row)
 OR EXISTS (SELECT 1 FROM {bu} b LEFT JOIN university_report r ON r.id = b.report_id
            WHERE r.id IS NULL OR to_jsonb(r) IS DISTINCT FROM b.after_row) THEN
   RAISE EXCEPTION 'rows were edited after apply: do not overwrite'; END IF;
END $$;
UPDATE university_report_translation t
   SET title = b.before_row->>'title', summary = b.before_row->>'summary', body = b.before_row->>'body',
       content = b.before_row->'content', updated_at = (b.before_row->>'updated_at')::timestamptz
  FROM {bt} b WHERE t.report_id = b.report_id AND t.lang = b.lang;
UPDATE university_report r
   SET status = b.before_row->>'status',
       source_video_count = (b.before_row->>'source_video_count')::int,
       source_web_count = (b.before_row->>'source_web_count')::int,
       source_video_ids = ARRAY(SELECT jsonb_array_elements_text(b.before_row->'source_video_ids'))::bigint[],
       avg_rating = (b.before_row->>'avg_rating')::numeric,
       recommend_ratio = (b.before_row->>'recommend_ratio')::numeric,
       avg_difficulty = (b.before_row->>'avg_difficulty')::numeric,
       avg_cost_monthly = b.before_row->>'avg_cost_monthly',
       cost_currency = b.before_row->>'cost_currency',
       aggregate_stats = CASE WHEN jsonb_typeof(b.before_row->'aggregate_stats') = 'null' THEN NULL ELSE b.before_row->'aggregate_stats' END,
       updated_at = (b.before_row->>'updated_at')::timestamptz
  FROM {bu} b WHERE r.id = b.report_id;
COMMIT;
""", encoding="utf-8")
    summary = {"run_id": args.run_id, "translations": len(tr_rows), "report_rows": len(ur_rows),
               "pilot": pilot, "unpublish": unpublish, "backup_tables": [bt, bu]}
    (out / "summary.json").write_text(json.dumps(summary, indent=1), encoding="utf-8")
    print(json.dumps(summary))


if __name__ == "__main__":
    main()
