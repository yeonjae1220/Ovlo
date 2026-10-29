"""Stage 1 — deterministic full audit of university reports and names (read-only, snapshot based).

Input : snapshot/*.jsonl (exported from prod with READ ONLY transactions)
Output: findings.jsonl (one finding per line), summary.md
No network, no LLM, no DB writes.
"""
import collections
import json
import os
import re
import sys
from pathlib import Path

# Working data lives outside git (raw prod text): OVLO_AUDIT_DIR (default output/audit-20261029)
# holds snapshot/ and regen/{packs,out,country_facts,verify}. See docs/report-regeneration.md.
REPO = Path(__file__).resolve().parents[2]
HERE = Path(os.environ.get("OVLO_AUDIT_DIR", REPO / "output" / "audit-20261029")).resolve()
SNAP = HERE / "snapshot"
CURRENCY_DATA = json.load(open(REPO / "scripts" / "report_currency_data.json", encoding="utf-8"))

LANGS = ["en", "ko", "ja", "zh", "de", "fr", "vi"]
SECTIONS = ["academics", "visa", "housing", "costs", "transport", "dining",
            "campus", "activities", "travel", "social", "work", "application"]

# ---------------------------------------------------------------- helpers
def load(name):
    with open(SNAP / f"{name}.jsonl", encoding="utf-8") as f:
        return [json.loads(l) for l in f if l.strip()]


def walk_strings(obj, path=""):
    if isinstance(obj, str):
        yield path, obj
    elif isinstance(obj, dict):
        for k, v in obj.items():
            yield from walk_strings(v, f"{path}.{k}" if path else k)
    elif isinstance(obj, list):
        for i, v in enumerate(obj):
            yield from walk_strings(v, f"{path}[{i}]")


def snippet(text, start, end, pad=60):
    s = max(0, start - pad)
    return text[s:min(len(text), end + pad)].replace("\n", " ")


RE_HANGUL = re.compile(r"[가-힣ᄀ-ᇿ㄰-㆏]")
RE_KANA = re.compile(r"[぀-ヿ]")
RE_HAN = re.compile(r"[一-鿿]")
RE_CYR = re.compile(r"[Ѐ-ӿ]")
RE_THAI = re.compile(r"[฀-๿]")
RE_LATIN_WORD = re.compile(r"\b[A-Za-z]{3,}\b")
RE_VI_DIACRITIC = re.compile(r"[ăâđêôơưạảấầẩẫậắằẳẵặẹẻẽếềểễệỉịọỏốồổỗộớờởỡợụủứừửữựỳỵỷỹ]", re.I)
EN_STOP = {"the", "and", "with", "for", "are", "is", "of", "to", "in", "students", "student",
           "which", "this", "that", "from", "you", "your", "can", "will", "have"}

# ---------------------------------------------------------------- money
SYMBOLS = {"$": "USD?", "US$": "USD", "€": "EUR", "£": "GBP", "¥": "JPY/CNY", "￥": "JPY/CNY", "₩": "KRW", "￦": "KRW",
           "₫": "VND", "₹": "INR", "₺": "TRY", "฿": "THB", "₱": "PHP", "R$": "BRL",
           "A$": "AUD", "AU$": "AUD", "C$": "CAD", "CA$": "CAD", "CAN$": "CAD", "MX$": "MXN", "NZ$": "NZD",
           "HK$": "HKD", "S$": "SGD", "NT$": "TWD", "CHF": "CHF", "kr": "SEK/NOK/DKK", "kr.": "SEK/NOK/DKK",
           "zł": "PLN", "Kč": "CZK", "Ft": "HUF", "RM": "MYR", "Rp": "IDR", "Rs": "INR", "TL": "TRY", "lei": "RON"}
CODE_RE = "RMB|" + "|".join(sorted({a for a in CURRENCY_DATA["aliases"] if a.isalpha() and a.isupper() and len(a) == 3}))
WORD_CUR = {  # localized currency words → ISO (keys lower-case for Latin words)
    "원": "KRW", "won": "KRW", "円": "JPY", "엔": "JPY", "yen": "JPY", "元": "CNY", "위안": "CNY",
    "人民币": "CNY", "yuan": "CNY", "유로": "EUR", "euro": "EUR", "euros": "EUR", "ユーロ": "EUR", "欧元": "EUR",
    "달러": "USD", "dollar": "USD", "dollars": "USD", "ドル": "USD", "美元": "USD", "美金": "USD", "đô": "USD",
    "파운드": "GBP", "ポンド": "GBP", "英镑": "GBP", "pound": "GBP", "pounds": "GBP",
    "rmb": "CNY", "韩元": "KRW", "ウォン": "KRW", "日元": "JPY", "新台币": "TWD", "台币": "TWD", "港币": "HKD", "港元": "HKD",
    "泰铢": "THB", "バーツ": "THB", "바트": "THB", "baht": "THB", "林吉特": "MYR", "링깃": "MYR", "ringgit": "MYR",
    "澳元": "AUD", "加元": "CAD", "瑞士法郎": "CHF", "福林": "HUF", "forint": "HUF", "엔화": "JPY",
    "đồng": "VND", "vnđ": "VND", "đ": "VND", "rupee": "INR", "rupees": "INR", "lira": "TRY",
    "peso": "PESO?", "pesos": "PESO?", "zloty": "PLN", "złoty": "PLN", "koruna": "CZK", "korun": "CZK",
    "krona": "SEK/NOK/DKK", "kronor": "SEK", "kroner": "SEK/NOK/DKK", "krone": "SEK/NOK/DKK",
}
# separators inside a number: , . nbsp narrow-nbsp thin-space apostrophes (Swiss) and plain space before 3 digits
NUM = r"\d{1,3}(?:[,.\u00a0\u202f\u2009’' ]\d{3}(?!\d))+(?:[.,]\d+)?|\d+(?:[.,]\d+)?"
MULT = r"(?:\s?(?:[kKM](?![\w\-])|천만|千万|백만|百万|만|万|천|千|억|亿|億|millions?|Millionen|Mio\.?|triệu|nghìn|ngàn))?"
SYM_ALT = "|".join(re.escape(s) for s in sorted(SYMBOLS, key=len, reverse=True))
_LATIN_WORDS = sorted((k for k in WORD_CUR if k.isascii() or k in ("đồng", "đô", "vnđ", "đ", "złoty")), key=len, reverse=True)
_OTHER_WORDS = sorted((k for k in WORD_CUR if k not in _LATIN_WORDS), key=len, reverse=True)
WORD_ALT = ("(?i:(?:" + "|".join(re.escape(w) for w in _LATIN_WORDS) + r"))(?![A-Za-zÀ-ỹ])|"
            + "|".join(re.escape(w) for w in _OTHER_WORDS))
_SYM = rf"(?<![A-Za-z])(?:{SYM_ALT})"
_CODE = rf"(?<![A-Za-z])(?:{CODE_RE})(?![A-Za-z])"
RE_MONEY_PREFIX = re.compile(rf"(?P<cur>{_SYM}|{_CODE})\s?(?P<num>{NUM})(?P<mult>{MULT})")
RE_MONEY_SUFFIX = re.compile(rf"(?P<num>{NUM})(?P<mult>{MULT})\s?(?P<cur>{_SYM}(?![A-Za-z])|{_CODE}|{WORD_ALT})")
MULTIPLIER = {"k": 1e3, "K": 1e3, "천": 1e3, "千": 1e3, "nghìn": 1e3, "ngàn": 1e3, "만": 1e4, "万": 1e4,
              "천만": 1e7, "千万": 1e7, "M": 1e6, "억": 1e8, "亿": 1e8, "億": 1e8, "million": 1e6, "millions": 1e6,
              "Millionen": 1e6, "백만": 1e6, "百万": 1e6, "Mio": 1e6, "Mio.": 1e6, "triệu": 1e6}

# compound CJK numerals (1억 5천만, 1億5千万) → plain digits before tokenizing
RE_CJK_COMPOUND = re.compile(r"(\d+(?:\.\d+)?)\s?(억|亿|億)\s?(?:(\d{1,3}(?:,\d{3})+|\d+(?:\.\d+)?)\s?(천만|千万|백만|百万|만|万))?")


def normalize_numerals(text):
    def rep(m):
        v = float(m.group(1)) * 1e8 + (float(m.group(3).replace(",", "")) * MULTIPLIER[m.group(4)] if m.group(3) else 0)
        return f"{v:.0f}"
    return RE_CJK_COMPOUND.sub(rep, text)


def parse_num(raw):
    s = re.sub(r"[\u00a0\u202f\u2009’' ]", "", raw)
    if "," in s and "." in s:  # the last separator is the decimal mark
        dec = "," if s.rfind(",") > s.rfind(".") else "."
        grp = "." if dec == "," else ","
        return float(s.replace(grp, "").replace(dec, "."))
    for sep in (",", "."):
        if sep in s:
            parts = s.split(sep)
            if len(parts) > 2 or (len(parts[-1]) == 3 and len(parts[0]) <= 3):
                return float(s.replace(sep, ""))  # 1.200 / 1,200 / 1.200.000 → grouping
            return float(s.replace(sep, "."))      # 12,50 / 12.5 → decimal
    return float(s)


def norm_cur(c):
    c = c.strip()
    if c in SYMBOLS:
        return SYMBOLS[c]
    if c.lower() in WORD_CUR:
        return WORD_CUR[c.lower()]
    if c in WORD_CUR:
        return WORD_CUR[c]
    if c == "RMB":
        return "CNY"
    return CURRENCY_DATA["aliases"].get(c, c)


def money_tokens(text):
    """(currency, value, start, end) per amount; positions refer to normalize_numerals(text)."""
    text = normalize_numerals(text)
    out = []
    for rx in (RE_MONEY_PREFIX, RE_MONEY_SUFFIX):
        for m in rx.finditer(text):
            try:
                v = parse_num(m.group("num"))
            except ValueError:
                continue
            v *= MULTIPLIER.get((m.group("mult") or "").strip(), 1)
            out.append((norm_cur(m.group("cur")), round(v, 2), m.start(), m.end()))
    # a range shares its currency: "$2,000–20,000" → also 20000, "400–600 €" → also 400
    for m in RE_RANGE.finditer(text):
        for c, v, a, b in list(out):
            if a <= m.start("a") < b or a <= m.start("b") < b:
                for g, mg in (("a", "ma"), ("b", "mb")):
                    try:
                        val = parse_num(m.group(g)) * MULTIPLIER.get((m.group(mg) or m.group("mb") or "").strip(), 1)
                    except ValueError:
                        continue
                    if not any(o[1] == round(val, 2) and o[0] == c for o in out):
                        out.append((c, round(val, 2), m.start(), m.end()))
                break
    return out


def cur_family(c):
    return {"USD?": "USD", "JPY/CNY": "JPY|CNY", "SEK/NOK/DKK": "SEK|NOK|DKK|ISK",
            "PESO?": "MXN|CLP|ARS|COP|PHP|UYU|CUP|DOP"}.get(c, c)


RANGE_SEP = r"\s?(?:[–—−‑\-~〜～至到]|から|à|to|bis|đến|부터)\s?"
RE_RANGE = re.compile(rf"(?P<a>{NUM})(?P<ma>{MULT}){RANGE_SEP}[^\d\s]{{0,3}}(?P<b>{NUM})(?P<mb>{MULT})")
RE_NUM_MULT = re.compile(rf"(?P<num>{NUM})(?P<mult>{MULT})")


def number_bag(text):
    """All numeric values in text; a range's trailing multiplier replaces its bare head (40-60万 → 400000, 600000)."""
    text = normalize_numerals(text)
    vals = {}
    for m in RE_NUM_MULT.finditer(text):
        try:
            vals[m.start()] = round(parse_num(m.group("num")) * MULTIPLIER.get((m.group("mult") or "").strip(), 1), 2)
        except ValueError:
            pass
    for m in RE_RANGE.finditer(text):
        mb = (m.group("mb") or "").strip()
        if mb and not (m.group("ma") or "").strip() and m.start("a") in vals:
            try:
                vals[m.start("a")] = round(parse_num(m.group("a")) * MULTIPLIER.get(mb, 1), 2)
            except ValueError:
                pass
    return set(vals.values())


RE_UNIT_AFTER = re.compile(r"\s?(?:h\b|hours?|Std\.?|Stunden|heures?|giờ|시간|時間|小时|小時|%|ECTS|credits?|crédits?|"
                           r"학점|単位|学分)", re.I)
# links, citation markers and numeric dates (2026.11.27, 2026-11-27, 27.11.2026, 11/27/2026) are not compared
RE_STRIP_FOR_NUMBERS = re.compile(r"\]\([^)]*\)|https?://\S+|\[\d+\]|\b\d{4}[./-]\d{1,2}(?:[./-]\d{1,2})?\.?(?!\d)|"
                                  r"\b\d{1,2}[./]\d{1,2}[./]\d{2,4}\b")


def significant_numbers(text):
    """Numbers that must survive translation: amounts, decimals, integers ≥ 32 (not years), and
    small integers with a unit (hours, %, ECTS, weeks, months). Dates/day counts ≤ 31 are skipped
    because their localized forms (10月15日, 15일, tháng 11) differ across languages."""
    text = normalize_numerals(RE_STRIP_FOR_NUMBERS.sub(" ", text))
    out = {v for _, v, *_ in money_tokens(text)}
    for m in RE_NUM_MULT.finditer(text):
        raw = m.group("num")
        try:
            v = round(parse_num(raw) * MULTIPLIER.get((m.group("mult") or "").strip(), 1), 2)
        except ValueError:
            continue
        is_int = float(v).is_integer()
        if is_int and 1900 <= v <= 2100 and not m.group("mult"):
            continue
        if not is_int or v >= 32 or RE_UNIT_AFTER.match(text, m.end()):
            out.add(v)
    return out


RE_CONVERSION_CUE = re.compile(r"[(（]\s*[^)）]{0,12}$|(?:约合|約|약|khoảng|about|approx\.?|environ|etwa|ca\.|≈|~)\s*$", re.I)


# rough USD rates for plausibility only (never shown to users)
USD = {"USD": 1, "EUR": 1.08, "GBP": 1.27, "JPY": 0.0067, "CNY": 0.14, "KRW": 0.00073, "AUD": 0.66,
       "CAD": 0.73, "CHF": 1.12, "SEK": 0.095, "NOK": 0.093, "DKK": 0.145, "PLN": 0.25, "CZK": 0.043,
       "HUF": 0.0027, "TWD": 0.031, "HKD": 0.128, "SGD": 0.74, "NZD": 0.6, "INR": 0.012, "THB": 0.028,
       "VND": 0.00004, "MYR": 0.21, "IDR": 0.000063, "PHP": 0.017, "TRY": 0.03, "BRL": 0.18,
       "MXN": 0.055, "RUB": 0.011, "CLP": 0.00105, "ARS": 0.001, "COP": 0.00025, "ZAR": 0.054,
       "ILS": 0.27, "AED": 0.27, "ISK": 0.0072, "RON": 0.22}

# ---------------------------------------------------------------- visa rules
# a visa code only counts when followed (within 25 chars) by a visa/status word; codes are case-sensitive
_VISA_WORDS = r"(?i:visa|visum|vize|비자|ビザ|签证|簽證|thị thực|status|student|학생|学生|체류|permit|titre)"
_VISA_CTX = rf"(?=[^\n]{{0,25}}?{_VISA_WORDS})"


def _visa_code(code):
    """code followed by a visa word, or a visa word followed by the code ("visa D-2", "thị thực F-1")."""
    return re.compile(rf"(?<![\w-]){code}(?![\w-]){_VISA_CTX}|{_VISA_WORDS}[^\n]{{0,25}}?(?<![\w-]){code}(?![\w-])")
VISA_COUNTRY = [  # (compiled pattern, countries where it is valid)
    (_visa_code(r"D-?[24](?:-\d+)?"), {"KR"}),
    (_visa_code(r"[FJ]-?1"), {"US"}),
    (re.compile(r"\bTier 4\b|Student Route|Student visa \(UK\)"), {"GB"}),
    (_visa_code(r"X[12]"), {"CN"}),
    (re.compile(r"College Student visa"), {"JP"}),
    (re.compile(r"\bsubclass 500\b", re.I), {"AU"}),
    (re.compile(r"\bVLS-TS\b"), {"FR"}),
]

ARTIFACT_PATTERNS = [
    (r"reference data|the data provided|provided data|참고 데이터|参考データ|参考数据|Referenzdaten|données de référence", "prompt_leak"),
    (r"(?-i:\b[Aa]s an AI\b|\bI cannot\b|\bI'm sorry\b|\bI am unable\b)|AI 언어 모델|作为一个?AI", "prompt_leak"),
    (r"```|<think>|</think>", "format_leak"),
    (r"\{\s*\"title\"|\"content\"\s*:", "json_leak"),
    (r"\(not the\s+mentioned|\s{2,}mentioned in", "prompt_leak"),
    (r"\bN/A\b|\bTBD\b|\.\.\.\"|^\s*\.\.\.\s*$", "placeholder"),
    (r"\[University\]|\[Name\]|\{univ|\{name\}", "placeholder"),
    (r"minimum 150 words|2,500\+ characters|Target length", "prompt_leak"),
]
RE_COURSE_CODE = re.compile(r"\b[A-Z]{2,4}\s?\d{3,4}[A-Z]?\b")
RE_PERCENT = re.compile(r"\d{1,3}\s?%")
RE_HEADING = re.compile(r"^\s*(?:#{1,3}\s+.+|\*\*[^*\n]{2,60}\*\*:?)\s*$", re.M)


# ---------------------------------------------------------------- main
def main():
    reports = {r["report_id"]: r for r in load("reports")}
    trs = load("translations")
    names = load("names")
    gu = {g["id"]: g for g in load("global_univ")}
    findings = []

    def add(check, sev, rid=None, lang=None, field=None, detail="", gid=None):
        findings.append({"check": check, "severity": sev, "report_id": rid, "global_univ_id": gid,
                         "lang": lang, "field": field, "detail": detail[:400]})

    by_report = collections.defaultdict(dict)
    for t in trs:
        by_report[t["report_id"]][t["lang"]] = t

    names_by = collections.defaultdict(dict)
    for n in names:
        names_by[n["global_univ_id"]][n["lang"]] = n["name"]

    # ---- per translation ---------------------------------------------------
    sentence_owner = collections.defaultdict(set)
    for rid, langs in by_report.items():
        rep = reports[rid]
        cc = (rep.get("country_code") or "").upper()
        local = set(CURRENCY_DATA["countries"].get(cc, []))
        for lang, t in langs.items():
            fields = [("title", t["title"] or ""), ("summary", t["summary"] or ""), ("body", t["body"] or "")]
            fields += [(f"content.{p}", s) for p, s in walk_strings(t["content"])]
            joined = "\n".join(s for _, s in fields)

            # 1. script purity
            for field, s in fields:
                if lang != "ko" and RE_HANGUL.search(s):
                    m = RE_HANGUL.search(s)
                    add("script_hangul_in_nonko", "high", rid, lang, field, snippet(s, m.start(), m.end()))
                if lang in ("en", "de", "fr", "vi") and (RE_KANA.search(s) or RE_HAN.search(s)):
                    m = RE_KANA.search(s) or RE_HAN.search(s)
                    add("script_cjk_in_latin", "high", rid, lang, field, snippet(s, m.start(), m.end()))
                if lang == "zh" and RE_KANA.search(s):
                    m = RE_KANA.search(s)
                    add("script_kana_in_zh", "high", rid, lang, field, snippet(s, m.start(), m.end()))
                if RE_CYR.search(s) or RE_THAI.search(s):
                    m = RE_CYR.search(s) or RE_THAI.search(s)
                    add("script_foreign", "medium", rid, lang, field, snippet(s, m.start(), m.end()))
            if lang == "ko" and RE_HAN.search(joined):
                hits = RE_HAN.findall(joined)
                if len(hits) >= 3:
                    add("script_han_in_ko", "medium", rid, lang, None, "".join(hits[:20]))
            # untranslated English in non-en (paragraph level)
            if lang != "en":
                for para in re.split(r"\n\s*\n", t["body"] or ""):
                    words = [w.lower() for w in RE_LATIN_WORD.findall(para)]
                    if len(words) >= 25 and sum(w in EN_STOP for w in words) / len(words) > 0.18:
                        add("untranslated_english", "high", rid, lang, "body", para[:200])
                        break
            if lang == "vi" and len(RE_VI_DIACRITIC.findall(t["body"] or "")) < 50:
                add("vi_missing_diacritics", "high", rid, lang, "body", (t["body"] or "")[:150])

            # 2. artifacts / prompt leaks
            for field, s in fields:
                for rx, kind in ARTIFACT_PATTERNS:
                    m = re.search(rx, s, re.I | re.M)
                    if m:
                        add(f"artifact_{kind}", "high" if kind != "placeholder" else "medium",
                            rid, lang, field, snippet(s, m.start(), m.end()))

            # 3. structure
            heads = RE_HEADING.findall(t["body"] or "")
            if len(heads) < 10:
                add("body_missing_sections", "medium", rid, lang, "body", f"{len(heads)} headings")
            for sec in SECTIONS:
                if sec not in (t["content"] or {}):
                    add("content_missing_section", "medium", rid, lang, f"content.{sec}", "")
            if len(t["body"] or "") < 1200:
                add("body_too_short", "medium", rid, lang, "body", f"{len(t['body'] or '')} chars")

            # 4. currency vs country
            if local:
                foreign = collections.Counter()
                ex = {}
                for field, s in fields:
                    for cur, val, a, b in money_tokens(s):
                        fam = cur_family(cur)
                        fams = set(fam.split("|"))
                        if fam == "USD" and cur == "USD?" and local & {"USD", "AUD", "CAD", "NZD", "SGD", "HKD", "TWD", "MXN", "CLP", "ARS", "COP"}:
                            continue  # bare $ is ambiguous for dollar/peso countries
                        if fams & local:
                            continue
                        kind = "currency_conversion_leftover" if RE_CONVERSION_CUE.search(s[max(0, a - 20):a]) else "currency_not_local"
                        foreign[(kind, fam)] += 1
                        ex.setdefault((kind, fam), (field, snippet(s, a, b, 40)))
                for (kind, fam), n in foreign.items():
                    sev = "high" if kind == "currency_not_local" else "medium"
                    add(kind, sev, rid, lang, ex[(kind, fam)][0],
                        f"country={cc} local={sorted(local)} found={fam}×{n}: {ex[(kind, fam)][1]}")

            # 5. visa type vs country
            for rx, valid in VISA_COUNTRY:
                m = rx.search(joined)
                if m and cc and cc not in valid:
                    add("visa_type_wrong_country", "high", rid, lang, None,
                        f"country={cc} mentions {m.group(0)!r} (valid for {sorted(valid)}): {snippet(joined, m.start(), m.end(), 50)}")

            # 6. hallucination markers (en only — pivot)
            if lang == "en":
                codes = set(RE_COURSE_CODE.findall(t["body"] or "")) - {"COVID", "IELTS", "TOEFL"}
                codes = {c for c in codes if not re.fullmatch(r"(?:B1|B2|C1|C2|ISO|EUR|USD|GBP|JPY|KRW|CNY|AUD|CAD|CHF)\s?\d+", c)}
                if len(codes) >= 2:
                    add("specific_course_codes", "medium", rid, lang, "body", ", ".join(sorted(codes)[:8]))
                for sent in re.split(r"(?<=[.!?])\s+", t["body"] or ""):
                    s = re.sub(r"\s+", " ", sent.strip())
                    if 60 <= len(s) <= 300:
                        key = re.sub(re.escape(rep.get("name_en") or "\x00"), "<U>", s)
                        sentence_owner[key].add(rid)

            # 7. title mentions the university (by any known name)
            known = {v for v in names_by.get(rep["global_univ_id"], {}).values()}
            known |= {rep.get("name_en") or "", rep.get("ex_name_en") or "", rep.get("ex_name_ko") or ""}
            known = {k for k in known if k}
            if not any(k.lower() in (t["title"] or "").lower() for k in known):
                exp = names_by.get(rep["global_univ_id"], {}).get(lang) or rep.get("name_en")
                add("title_name_mismatch", "medium", rid, lang, "title", f"title={t['title']!r} expected~{exp!r}")

        # ---- cross-language consistency ----------------------------------
        en = langs.get("en")
        if not en:
            add("missing_en", "high", rid)
            continue
        for lang in LANGS:
            if lang not in langs:
                add("missing_lang", "high", rid, lang)
        en_vals = number_bag((en["body"] or "") + "\n" + (en["summary"] or "") + "\n" + json.dumps(en["content"], ensure_ascii=False))
        for lang, t in langs.items():
            if lang == "en":
                continue
            # body/summary amounts that appear nowhere in en
            seen = set()
            for c, v, a, b in money_tokens((t["body"] or "") + "\n" + (t["summary"] or "")):
                fam = cur_family(c)
                if v in en_vals or v < 5 or (fam, v) in seen:
                    continue
                seen.add((fam, v))
                txt = (t["body"] or "") + "\n" + (t["summary"] or "")
                add("xlang_amount_not_in_en", "high", rid, lang, "body",
                    f"{fam} {v:g} not in en: {snippet(txt, a, b, 40)}")
            # content leaves: numeric fingerprint must equal en's
            en_leaf = dict(walk_strings(en["content"]))
            for path, s in walk_strings(t["content"]):
                es = en_leaf.get(path)
                if es is None:
                    continue
                nums_t = {v for v in number_bag(s) if v >= 5}
                nums_e = {v for v in number_bag(es) if v >= 5}
                if money_tokens(es) and nums_e and nums_t and nums_t != nums_e:
                    add("xlang_content_amount_diff", "high", rid, lang, f"content.{path}", f"en={es!r} {lang}={s!r}")
                curs_t = {x for c, *_ in money_tokens(s) for x in cur_family(c).split("|")}
                curs_e = {x for c, *_ in money_tokens(es) for x in cur_family(c).split("|")}
                if curs_e and curs_t and not (curs_t & curs_e):
                    add("xlang_currency_diff", "high", rid, lang, f"content.{path}", f"en={es!r} {lang}={s!r}")
            en_paths, t_paths = {p for p, _ in walk_strings(en["content"])}, {p for p, _ in walk_strings(t["content"])}
            if len(en_paths ^ t_paths) > 3:
                add("xlang_content_shape_diff", "medium", rid, lang, "content",
                    f"en-only={len(en_paths - t_paths)} {lang}-only={len(t_paths - en_paths)}")
            ratio = len(t["body"] or "") / max(1, len(en["body"] or ""))
            floor = {"ko": 0.3, "ja": 0.3, "zh": 0.22}.get(lang, 0.7)
            if ratio < floor:
                add("xlang_body_truncated", "high", rid, lang, "body", f"len ratio vs en = {ratio:.2f}")

        # ---- plausibility of monthly cost (en content) ---------------------
        costs = (en["content"] or {}).get("costs") or {}
        housing = (en["content"] or {}).get("housing") or {}
        mt = [(c, v) for c, v, *_ in money_tokens(str(costs.get("monthly_total") or ""))]
        dp = [(c, v) for c, v, *_ in money_tokens(str(housing.get("dorm_price") or ""))]
        cc = (rep.get("country_code") or "").upper()
        local = CURRENCY_DATA["countries"].get(cc, [])
        def to_usd(c, v):
            if c == "USD?":
                if local and local[0] in ("MXN", "CLP", "ARS", "COP"):
                    return None  # bare $ is the peso sign there
                c = local[0] if local and local[0] in ("AUD", "CAD", "NZD", "SGD", "HKD", "TWD") else "USD"
            elif c == "JPY/CNY":
                c = local[0] if local and local[0] in ("JPY", "CNY") else "JPY"
            return v * USD[c] if c in USD else None
        mt_usd = [u for u in (to_usd(c, v) for c, v in mt) if u]
        dp_usd = [u for u in (to_usd(c, v) for c, v in dp) if u]
        if mt_usd and (max(mt_usd) > 6000 or max(mt_usd) < 150):
            add("cost_implausible", "high", rid, "en", "content.costs.monthly_total",
                f"{costs.get('monthly_total')!r} ≈ USD {min(mt_usd):.0f}-{max(mt_usd):.0f} (country {cc})")
        if mt_usd and dp_usd and min(dp_usd) > max(mt_usd) * 1.05:
            add("cost_dorm_gt_total", "medium", rid, "en", "content.housing.dorm_price",
                f"dorm {housing.get('dorm_price')!r} > monthly total {costs.get('monthly_total')!r}")
        cur_field = str(costs.get("currency") or "").upper()
        cur_names = {"JPY": "YEN", "KRW": "WON", "THB": "BAHT", "CNY": "RMB|YUAN|RENMINBI", "EUR": "EURO", "GBP": "POUND", "TRY": "LIRA|TL", "TWD": "NT\\$|TAIWAN", "USD": "DOLLAR|\\$", "MYR": "RINGGIT|RM", "VND": "DONG", "PLN": "ZLOTY|PLN", "CZK": "KORUN|CZK", "HUF": "FORINT", "CHF": "FRANC", "INR": "RUPEE", "IDR": "RUPIAH", "PHP": "PESO", "MXN": "MEXICAN PESO", "CLP": "CHILEAN PESO", "ARS": "ARGENTINE PESO", "COP": "COLOMBIAN PESO", "BRL": "REAL", "SEK": "KRONA|KRONOR", "NOK": "KRONE|KRONER", "DKK": "KRONE|KRONER", "AUD": "AUSTRALIAN|AUD", "CAD": "CANADIAN|CAD", "NZD": "NEW ZEALAND|NZD", "SGD": "SINGAPORE|SGD", "HKD": "HONG KONG|HKD", "RUB": "RUBLE|ROUBLE", "ZAR": "RAND", "ILS": "SHEKEL|NIS"}
        def cur_ok(field):
            return (any(x in field for x in local) or norm_cur(field) in local
                    or any(re.search(cur_names.get(x, "^$"), field) for x in local))
        if local and cur_field and not cur_ok(cur_field):
            add("cost_currency_field_wrong", "high", rid, "en", "content.costs.currency",
                f"country {cc} expects {local}, field={cur_field!r}")

        # ---- report-level ----------------------------------------------------
        if rep["source_video_count"] != len(rep["source_video_ids"] or []):
            add("source_count_mismatch", "medium", rid, None, None,
                f"source_video_count={rep['source_video_count']} ids={len(rep['source_video_ids'] or [])}")
        if rep.get("canonical_id"):
            add("report_on_hidden_univ", "high", rid, None, None, f"global {rep['global_univ_id']} → canonical {rep['canonical_id']}")
        g = gu.get(rep["global_univ_id"]) or {}
        if rep.get("cost_currency") and local and rep["cost_currency"] not in local:
            add("report_cost_currency_wrong", "medium", rid, None, "cost_currency", f"{rep['cost_currency']} vs {local}")

    # boilerplate sentences shared by many reports
    for sent, owners in sentence_owner.items():
        if len(owners) >= 15:
            add("boilerplate_sentence", "low", None, "en", "body", f"{len(owners)} reports: {sent[:200]}")

    # ---- names table -------------------------------------------------------
    script_ok = {
        "ko": lambda s: bool(RE_HANGUL.search(s)),
        "ja": lambda s: bool(RE_KANA.search(s) or RE_HAN.search(s)),
        "zh": lambda s: bool(RE_HAN.search(s)) and not RE_KANA.search(s) and not RE_HANGUL.search(s),
        "ru": lambda s: bool(RE_CYR.search(s)),
    }
    dup = collections.defaultdict(set)
    for n in names:
        lang, s, gid = n["lang"], n["name"], n["global_univ_id"]
        g = gu.get(gid, {})
        if g.get("canonical_id"):
            continue
        ok = script_ok.get(lang, lambda x: not (RE_HANGUL.search(x) or RE_KANA.search(x) or RE_HAN.search(x) or RE_CYR.search(x)))
        if not ok(s):
            add("name_wrong_script", "high", None, lang, "name", f"{g.get('name_en')!r} → {s!r} [{n['source']}]", gid)
        if lang == "ko" and RE_HAN.search(s):
            add("name_ko_has_han", "medium", None, lang, "name", f"{g.get('name_en')!r} → {s!r}", gid)
        dup[(lang, s.strip().lower())].add(gid)
    for (lang, s), gids in dup.items():
        if len(gids) > 1:
            add("name_shared_by_univs", "medium", None, lang, "name",
                f"{s!r} used by {sorted(gids)[:6]} ({[gu[i].get('name_en') for i in sorted(gids)[:3]]})")

    # ---- write ---------------------------------------------------------------
    with open(HERE / "findings.jsonl", "w", encoding="utf-8") as f:
        for x in findings:
            f.write(json.dumps(x, ensure_ascii=False) + "\n")
    write_summary(findings, reports)
    print(f"{len(findings)} findings → {HERE/'findings.jsonl'}")


def write_summary(findings, reports):
    by_check = collections.defaultdict(list)
    for x in findings:
        by_check[x["check"]].append(x)
    sev_rank = {"high": 0, "medium": 1, "low": 2}
    lines = ["# Stage 1 audit summary", "",
             f"reports={len(reports)}  findings={len(findings)}", "",
             "| check | severity | findings | reports | per-lang |", "|---|---|---|---|---|"]
    for check, xs in sorted(by_check.items(), key=lambda kv: (sev_rank[kv[1][0]["severity"]], -len(kv[1]))):
        rids = {x["report_id"] for x in xs if x["report_id"]}
        pl = collections.Counter(x["lang"] for x in xs if x["lang"])
        lines.append(f"| {check} | {xs[0]['severity']} | {len(xs)} | {len(rids)} | "
                     + " ".join(f"{k}:{v}" for k, v in sorted(pl.items())) + " |")
    lines += ["", "## Samples"]
    for check, xs in sorted(by_check.items()):
        lines.append(f"\n### {check} ({len(xs)})")
        for x in xs[:6]:
            lines.append(f"- r{x['report_id']} g{x['global_univ_id']} {x['lang']} {x['field']}: {x['detail'][:260]}")
    rep_hit = collections.Counter(x["report_id"] for x in findings if x["report_id"] and x["severity"] == "high")
    lines += ["", f"## Reports with ≥1 high finding: {len(rep_hit)} / {len(reports)}"]
    (HERE / "summary.md").write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    sys.exit(main())
