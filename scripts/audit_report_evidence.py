"""Read-only review queue from public report JSONL; never fetches URLs or rewrites data.

Input: report_id, lang, country_code, as_of, sources, costs, visa, housing, academics.
Presence of a source or a date is deliberately never treated as verification.
"""
import argparse
import calendar
import collections
import datetime as dt
import json
import re
from pathlib import Path
from urllib.parse import urlsplit


def reference_date(value, today):
    if not isinstance(value, str) or not re.fullmatch(r"\d{4}-\d{2}(-\d{2})?", value):
        return None
    try:
        parts = list(map(int, value.split("-")))
        first = dt.date(parts[0], parts[1], parts[2] if len(parts) == 3 else 1)
        if first > today:
            return None
        return first if len(parts) == 3 else dt.date(parts[0], parts[1], calendar.monthrange(parts[0], parts[1])[1])
    except ValueError:
        return None


def source_urls(value):
    urls = []
    for source in value if isinstance(value, list) else []:
        if not isinstance(source, dict) or not isinstance(source.get("url"), str):
            continue
        try:
            parsed = urlsplit(source["url"])
            if parsed.scheme not in ("http", "https") or not parsed.hostname or parsed.username or parsed.password:
                continue
            if source["url"] not in urls:
                urls.append(source["url"])
        except ValueError:
            continue
    return urls


def review(row, today):
    reasons = []
    urls = source_urls(row.get("sources"))
    date = reference_date(row.get("as_of"), today)
    # Syntax only: correct country assignment still requires an authoritative source.
    if not re.fullmatch(r"[A-Z]{2}", str(row.get("country_code", ""))):
        reasons.append("COUNTRY_CODE_SYNTAX")
    if not urls:
        reasons.append("MISSING_SOURCES")
    if date is None:
        reasons.append("MISSING_OR_INVALID_DATE")
    elif (today - date).days > 180:
        reasons.append("OLDER_THAN_180_DAYS")
    claims = []
    for group, fields in {"costs": ("monthly_total", "rent", "currency"), "visa": ("type", "cost", "duration", "processing_days"),
                          "housing": ("dorm_available", "dorm_price"), "academics": ("deadline", "gpa_req", "language_req")}.items():
        values = row.get(group)
        if isinstance(values, dict):
            for field in fields:
                if values.get(field) is not None and values.get(field) != "":
                    claims.append({"field": group + "." + field, "reportedValue": values[field],
                                   "reviewStatus": "UNREVIEWED", "verifiedAt": None, "evidenceUrl": None})
    return {"reportId": row["report_id"], "lang": row["lang"], "countryCode": row.get("country_code"),
            "referenceDate": row.get("as_of"), "reasons": reasons, "sourceUrls": urls,
            "reviewStatus": "UNREVIEWED", "claims": claims}


def build_queue(rows, today):
    seen = set()
    queue = []
    for row in rows:
        key = (row["report_id"], row["lang"])
        if key in seen:
            raise ValueError("Duplicate report/language input")
        seen.add(key)
        queue.append(review(row, today))
    return {"checkedAt": today.isoformat(), "reviewScope": "This audit only; UNREVIEWED does not invalidate earlier reviews. Translated fields are counted separately.",
            "reportCount": len({r["reportId"] for r in queue}),
            "translationCount": len(queue), "reasonCounts": dict(collections.Counter(reason for r in queue for reason in r["reasons"])),
            "unreviewedClaimCount": sum(len(r["claims"]) for r in queue), "queue": queue}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshot", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--as-of", type=dt.date.fromisoformat, required=True)
    args = parser.parse_args()
    if args.snapshot.resolve() == args.output.resolve():
        parser.error("Output must not overwrite the input snapshot")
    with args.snapshot.open(encoding="utf-8") as stream:
        result = build_queue((json.loads(line) for line in stream if line.strip()), args.as_of)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in result.items() if k != "queue"}, ensure_ascii=False))


if __name__ == "__main__":
    main()
