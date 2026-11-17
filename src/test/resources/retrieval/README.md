# Synthetic office retrieval fixture

28 documents with seven language versions; 70 labeled questions (10 per language).
No production text, user identifiers, paid APIs or model calls are used. The business date is fixed to 2027-03-02.
The titles form deliberately overlapping topic pairs. The final safety guide is longer than 3,000 characters in every language;
its goggles location is at the end. Paraphrases use synonyms absent from the topic labels. Follow-ups prepend the previous question
using the same last-two-questions rule as InquiryCommandService. These are controlled retrieval cases, not an estimate of production quality.

Each relevant ID is a stable fixture ID, mapped to the real generated PostgreSQL ID by the runner. PostgreSQL uses actual migrations,
persistence and FTS; the keyword unit runner only substitutes storage, never the search result. All candidates must belong to the target office.

Run `./gradlew test --tests '*KeywordNoticeRetrievalTest'` and
`./gradlew integrationTest --tests '*PostgresNoticeRetrievalIntegrationTest'` (Docker required).
Both write JSON and CSV to `build/reports/retrieval/`; missing rank is 0. Baselines cannot be automatically updated by tests.
A changed fixture hash requires review and a documented new baseline; previously successful cases must continue to retrieve their gold.
