You are regenerating one university guide for Ovlo, a study-abroad site. The current guide was
written by an 8B model and contains invented facts (wrong visa types, prices off by 10–1000×,
restaurants from other cities, made-up course codes). Your guide replaces it and will be shown to
real students in 7 languages, so accuracy beats length.

Working dir: $OVLO_AUDIT_DIR/regen (default output/audit-20261029/regen); tools in scripts/report_regen/
Report id: {RID}  ({NAME}, {CC})

Read first:
1. `SPEC.md` — the full contract (sources, output files, schema, translation rules). Follow it exactly.
2. `packs/{RID}/source_pack.md` and `packs/{RID}/meta.json` — student video transcripts, crawled pages, university names per language. The old report is at the end, marked UNTRUSTED.
3. `country_facts/{CC}.json` — visa/work/insurance facts. Use them verbatim for those topics; do not research visas yourself.

Then:
4. Research official university pages with WebSearch/WebFetch: exchange/incoming students page, application requirements and deadlines (for incoming exchange students), housing for exchange students with prices, tuition/fee policy for exchange students, language of instruction / course catalog for exchange students, buddy/orientation programs. Prefer pages for the current or next academic year; note the year.
5. Use the student videos for lived experience only after confirming each is about this university.
6. Write `out/{RID}/research_log.md`, `out/{RID}/claims.jsonl`, `out/{RID}/report.json` per SPEC.
7. Run `python3 -I scripts/report_regen/check_report.py out/{RID}/report.json` and fix until 0 errors.

Do not touch any database, git, or files outside `out/{RID}/`.

Report back in ≤ 15 lines: sections written vs skipped, number of official/video/other sources, videos discarded, the 3 most important facts that differ from the old report, and anything you could not verify.
