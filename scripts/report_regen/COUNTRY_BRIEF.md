You are building the country fact table for Ovlo university guides. Country: {CC} ({COUNTRY_NAME}).
Every guide for a university in this country copies visa, work, insurance and wage facts from your file
verbatim, so one wrong value spreads to every report for {CC}. Accuracy beats completeness.

Working dir: $OVLO_AUDIT_DIR/regen (default output/audit-20261029/regen). Write only `country_facts/{CC}.json`.
Look at `country_facts/HU.json` and `country_facts/DE.json` first: same keys, same level of detail.

Perspective: a **Korean passport holder** going for one or two semesters of exchange study (not a degree).

Rules:
- Official sources only: the government immigration/foreign ministry/labour ministry, an embassy or consulate
  (the country's embassy in Seoul preferred), or the Korean Ministry of Foreign Affairs (0404.go.kr, overseas.mofa.go.kr).
  No agencies, blogs, Wikipedia, or university pages (except for health insurance when the government delegates it).
- Copy values as the source states them, with the currency the source uses. No conversions.
- If you cannot verify a value from an official page, set it to `null` and explain in `notes`. Never guess.
- Every block has `sources`: `[{"title", "url", "publisher", "accessed": "YYYY-MM-DD"}]`.
- Fetch the pages (WebFetch); a search snippet alone is not a source.

Keys (all required; null when unverified):
```
country, as_of ("YYYY-MM"), currency (ISO 4217), perspective ("Korean passport holder, 1–2 semester exchange"),
visa: needed_for_korean_citizens (bool), permit_name (official local name), permit_name_en, short_stay_alternative,
      where_to_apply, fee, fee_note, processing_time, required_docs [..], after_arrival, sources
work: allowed (bool|null), limit, conditions, sources
health_insurance: required (bool|null), note, sources
min_wage: amount, effective, note, sources
travel_advisory: mofa_level (Korean MOFA travel alert level for the country or the regions with universities,
      e.g. "1 (여행유의) nationwide; 3 (출국권고) in border regions"), travel_ban (bool — on the Korean travel-ban list,
      which requires an exceptional passport-use permit under the Passport Act), note, sources
notes: what is null and why, anything a student must check (e.g. rules changing next year)
```

Finally run `python3 -I -c "import json;json.load(open('country_facts/{CC}.json'))"` to confirm the file is valid JSON.

Do not touch any database, git, or files other than `country_facts/{CC}.json`.
Report back in ≤ 8 lines: visa needed?, permit name, fee, work limit, MOFA level, which keys are null and why.
