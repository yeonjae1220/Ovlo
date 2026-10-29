# Ovlo university report — regeneration spec (pilot, 2026-10-29)

Goal: a report an exchange student can act on. **Every factual statement must be traceable to a
source.** A shorter report with gaps is better than a long one with invented details. The old
reports were written by an 8B model told to write "150+ words per section with numbers" and are
full of invented facts — do not copy anything from them.

Audience: students considering or preparing an exchange semester at this university. Core user base
is Korean outbound students; the site is shown in 7 languages (en, ko, ja, zh, de, fr, vi).

## Sources (in priority order)

| tag | what | use for |
|---|---|---|
| `C:<CC>.<key>` | `country_facts/<CC>.json` (official government sources, already researched) | visa/permit, fees, work rules, currency, health insurance. Copy values exactly. |
| `O<n>` | official university pages you fetch now (exchange/international office, housing, fees, course catalog) | application requirements, deadlines, dorms and prices, programs, language of instruction |
| `V<n>` | student videos in the source pack (transcripts, 2023–2025) | lived experience: costs students paid, areas, food, transport, social life, tips |
| `W<n>` | other reputable pages (city transit operator, official tourism, well-known guides) | transport passes, city highlights |

Rules for sources:
- Verify each `V` video is really about **this** university (title/transcript mention the university or its city campus). Discard the rest and list them as discarded in `research_log.md`.
- Never use the old report, the `web_exchange_programs` rows or the pipeline summaries as a source on their own — they are leads to check against a transcript or an official page.
- Student-reported numbers are anecdotes: present them as "students reported ~X (year)" with the video year, never as official prices.
- Official numbers: quote the page's value and the academic year it applies to.
- If you cannot verify something, leave the field `null` / omit the sentence. For important missing items (deadlines, GPA, dorm price) write one sentence pointing to the official page instead.
- Prices in the **local currency only**. No conversions to KRW/USD/anything. Keep the currency style of the language (€1,200 / 1.200 € is fine) but never change the amount.
- Do not name restaurants, cafés, clubs, courses or course codes unless a source names them.
- Visa/permit text is written for **Korean passport holders**, and says so ("for Korean citizens"). Take it from `country_facts` only.

## Output files (in `out/<report_id>/`)

1. `research_log.md` — sources used (tag, title, URL, date accessed/published), videos discarded and why, open questions.
2. `claims.jsonl` — one line per factual claim in the en report: `{"section": "...", "claim": "...", "sources": ["O2", "V5"], "quote": "short supporting quote from the source"}`. Every number in the en body/content must appear in some claim.
3. `report.json` — `{"en": {...}, "ko": {...}, "ja": {...}, "zh": {...}, "de": {...}, "fr": {...}, "vi": {...}}`, each language:
   - `title`: `"<university name in that language> Exchange Guide"` (localized phrase). Use the name from `names by language` in the pack when present; otherwise the official English name.
   - `summary`: 2–4 sentences, ≤ 400 chars (en). Only facts that are in the body.
   - `body`: markdown. `##` headings for the sections below, in this order, **skip a section entirely if you have nothing verified for it**. End with `## Sources` (numbered markdown links `[1] [Title](url)`) — inline references like `[1]` after sentences that state facts. Videos are cited as `[n] [Video title](youtube url) (student video, YYYY)`.
     Sections: Academics · Application & requirements · Visa & entry · Housing · Monthly costs · Getting around · Food & daily life · Campus life · Clubs & activities · Travel · Social life · Part-time work · Final tips
   - `content`: same 12 keys as before (schema below). `null` for unknown values, `[]` for unknown lists. Plus `as_of` (`"2026-10"`) and `sources` (`[{"n":1,"title":"...","url":"...","type":"official|video|other","year":"2025"}]`, identical across languages except translated titles are NOT needed — keep titles as published).

```json
{
 "academics": {"overview": null, "course_examples": [], "tips": [], "gpa_req": null, "language_req": null, "deadline": null},
 "visa": {"type": null, "cost": null, "duration": null, "processing_days": null, "required_docs": []},
 "housing": {"dorm_available": null, "dorm_price": null, "dorm_type": null, "dorm_tips": [], "offcampus_price": null, "recommended_areas": []},
 "costs": {"monthly_total": null, "rent": null, "food": null, "transport": null, "entertainment": null, "currency": "<ISO code>", "saving_tips": []},
 "transport": {"main_mode": null, "apps": [], "monthly_pass": null, "tips": []},
 "dining": {"local_specialties": [], "what_students_eat": null, "recommended_restaurants": [], "recommended_cafes": [], "marts_supermarkets": [], "must_try_foods": []},
 "campus": {"key_facilities": [], "study_spots": [], "tips": null},
 "activities": {"clubs": [], "sports": [], "outdoor": [], "official_programs": []},
 "travel": {"must_visit": [], "nearby_cities": [], "best_season": null, "famous_events": [], "local_famous": []},
 "social": {"korean_community": null, "language_exchange": null, "tips": []},
 "work": {"part_time_allowed": null, "legal_limit": null, "common_jobs": [], "avg_hourly_wage": null, "tips": []},
 "application": {"gpa": null, "language": null, "deadline": null, "required_documents": []}
}
```
(`academics.gpa_req/language_req/deadline` mirror `application.*` — the site's info cards read them.)

Card fields are shown as short labels on info cards — keep them ≤ 60 characters, no full sentences:
`visa.type` (e.g. "Residence permit for study (§ 16b AufenthG)", "J-1 Exchange Visitor"), `visa.cost`, `visa.processing_days`,
`costs.monthly_total`, `costs.rent`, `housing.dorm_price`, `housing.dorm_type`, `academics.gpa_req`, `academics.language_req`, `academics.deadline`.
Put explanations in the body instead.

Official fees that a government states in a foreign currency (e.g. a visa fee in USD) are allowed when quoted from `country_facts`; never add conversions.
In the body, video titles in a script the target language cannot read may be replaced by a short description in that language; `content.sources` keeps the published titles.

## Translation rules (ko, ja, zh, de, fr, vi from the final en)
- Translate meaning faithfully. Add nothing, drop nothing.
- Every number, currency and date stays identical (only digit grouping/decimal style may follow the language). No added conversions.
- Proper nouns (places, buildings, programs, apps, shops) stay in their original form; a translation may follow in parentheses only for ko/ja/zh readability.
- Text in a language must not contain another language's script except proper nouns (e.g., no Hangul in de/fr/vi/en/ja/zh).
- vi must use full diacritics. zh = Simplified Chinese.
- `## Sources` list is identical in every language.

## Style and consistency (added after the pilot review, 2026-10-29)
- Register: ko polite (-습니다/-요, not -다), ja です・ます (not だ・である), de "du", fr "vous", vi "bạn". Same in every report.
- Deadlines: translate "by <date>" as inclusive ("spätestens am", "au plus tard le", "chậm nhất ngày", "까지", "までに", "之前（含当日）") — never "before", which moves the deadline a day earlier.
- Keep the strength of instructions: "must"/"required" ≠ "recommended"; "expected to" ≠ "must"; "not guaranteed" stays not guaranteed.
- zh = Simplified for everything, including university names (the names table may hold Traditional forms — convert them).
- University names in ko: use the established Korean name (e.g. 상하이교통대학교, 원지대학교); if unsure, keep the official English name.
- Times: use the local 24-hour style in de/fr/vi (21 Uhr, 21 h, 21:00) and keep the same time.
- Direct quotes of official English text are allowed in translations (keep them in quotation marks).

## Self-check before finishing
- `python3 -I scripts/report_regen/check_report.py <regen>/out/<report_id>/report.json` must report 0 errors.
- Re-read your en body against `claims.jsonl`: any sentence with a fact but no claim → add the claim or delete the sentence.
