import datetime as dt
import unittest
from audit_report_evidence import build_queue, reference_date, source_urls


class ReportEvidenceTests(unittest.TestCase):
    today = dt.date(2026, 11, 24)

    def test_absent_evidence_is_not_verification(self):
        result = build_queue([{"report_id": 1, "lang": "en"}], self.today)
        row = result["queue"][0]
        self.assertEqual(row["reviewStatus"], "UNREVIEWED")
        self.assertEqual(set(row["reasons"]), {"MISSING_SOURCES", "MISSING_OR_INVALID_DATE", "COUNTRY_CODE_SYNTAX"})

    def test_source_presence_does_not_verify_costs(self):
        result = build_queue([{"report_id": 1, "lang": "en", "country_code": "DE", "as_of": "2026-10",
                               "sources": [{"url": "https://university.example"}], "costs": {"rent": "EUR 500"}}], self.today)
        self.assertEqual(result["queue"][0]["reasons"], [])
        self.assertEqual(result["queue"][0]["claims"][0]["reviewStatus"], "UNREVIEWED")
        self.assertIsNone(result["queue"][0]["claims"][0]["verifiedAt"])

    def test_month_precision_and_leap_day(self):
        self.assertEqual(reference_date("2024-02", self.today), dt.date(2024, 2, 29))
        for value in ("2026-02-30", "2027-01", "2026-13", None, 2026):
            self.assertIsNone(reference_date(value, self.today))

    def test_old_reference_and_invalid_country_need_review(self):
        row = build_queue([{"report_id": 1, "lang": "en", "as_of": "2025-01", "country_code": "kr"}], self.today)["queue"][0]
        self.assertIn("OLDER_THAN_180_DAYS", row["reasons"])
        self.assertIn("COUNTRY_CODE_SYNTAX", row["reasons"])

    def test_non_http_and_credential_links_are_not_sources(self):
        self.assertEqual(source_urls([{"url": s} for s in ["javascript:alert(1)", "//example.com", "https://u:p@example.com", "http://[bad"]]), [])

    def test_false_is_a_claim_and_null_is_unknown(self):
        row = build_queue([{"report_id": 1, "lang": "en", "housing": {"dorm_available": False, "dorm_price": None}}], self.today)["queue"][0]
        self.assertEqual(len(row["claims"]), 1)
        self.assertIs(row["claims"][0]["reportedValue"], False)

    def test_duplicates_fail_instead_of_distorting_coverage(self):
        with self.assertRaises(ValueError):
            build_queue([{"report_id": 1, "lang": "en"}] * 2, self.today)
