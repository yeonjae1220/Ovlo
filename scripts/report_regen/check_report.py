"""Validate a regenerated report.json (+ sibling claims.jsonl, research_log.md, ../../packs/<rid>/meta.json).

Usage: python3 -I check_report.py out/<rid>/report.json
Prints ERROR/WARN lines; exit 1 if any ERROR (also on malformed input).
"""
import json
import re
import sys
import traceback
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPTS))
import audit_stage1 as a  # noqa: E402

HERE = a.HERE / "regen"  # packs/, country_facts/ of the working data

LANGS = ["en", "ko", "ja", "zh", "de", "fr", "vi"]
CONTENT_KEYS = set(a.SECTIONS) | {"as_of", "sources"}
CARD_FIELDS = [("visa", "type"), ("visa", "cost"), ("visa", "processing_days"), ("costs", "monthly_total"),
               ("costs", "rent"), ("housing", "dorm_price"), ("housing", "dorm_type"),
               ("academics", "gpa_req"), ("academics", "language_req"), ("academics", "deadline")]
CARD_MAX = 60
# characters that exist only in Traditional Chinese (zh must be Simplified)
TRADITIONAL_ONLY = set("學國際語體與為們這個來時會對說關開門東車長點發經過還進動現華區機場術書畫歷愛讓聽雙寫驗變觀實業產務費價買賣錢銀運連達選邊間問題頁館韓臺灣寶電話號碼網絡圖認識請應該當專課與")
RE_SCRIPT_REQUIRED = {"ko": a.RE_HANGUL, "ja": a.RE_KANA, "zh": a.RE_HAN}
MIN_SCRIPT_CHARS = 300
# register (SPEC "Style and consistency"): generated guides are polite/formal in every language
RE_CITATION = re.compile(r"\s*[\[［]\d+(?:\s*[,，、\-–]\s*\d+)*[\]］]")
RE_KO_PLAIN = re.compile(r"(?<!니)(?<![요죠])다[.!]|[해야지어]\.(?=\s|$)")
RE_JA_SENT = re.compile(r"[^。\n]+。")
RE_JA_POLITE = re.compile(r"(です|ます|ません|でした|ました|ください|でしょう|ましょう)[」』）)]*。$")
RE_JA_PLAIN = re.compile(r"(である|だ|だった|する|した|ある|いる|ない|なる|できる|れる)[」』）)]*。$")
RE_INFORMAL = {"de": re.compile(r"\b(?:[Dd]u|[Dd]ich|[Dd]ir|[Dd]ein\w*)\b"),
               "fr": re.compile(r"\b(?:[Tt]u|[Tt]oi|[Tt]on|[Tt]es)\b")}
RE_QUOTED = re.compile(r'"[^"\n]{8,}"|“[^”\n]{8,}”|«[^»\n]{8,}»|„[^“\n]{8,}“|「[^」\n]{8,}」')


def sources_block(body):
    """Text after the last ## heading (the Sources section)."""
    parts = re.split(r"^##\s+.*$", body or "", flags=re.M)
    return parts[-1] if len(parts) > 1 else ""


def main_text(body):
    """Body without its final Sources section."""
    idx = [m.start() for m in re.finditer(r"^##\s+.*$", body or "", flags=re.M)]
    return (body or "")[:idx[-1]] if idx else (body or "")


def leaves(obj, path=""):
    """(path, value) for every leaf, including None and non-strings; lists report their length too."""
    if isinstance(obj, dict):
        for k, v in obj.items():
            yield from leaves(v, f"{path}.{k}" if path else k)
    elif isinstance(obj, list):
        yield f"{path}[]", len(obj)
        for i, v in enumerate(obj):
            yield from leaves(v, f"{path}[{i}]")
    else:
        yield path, obj


def all_numbers(text):
    return a.number_bag(a.RE_STRIP_FOR_NUMBERS.sub(" ", text))


RE_PM = re.compile(r"(\d{1,2})(?::\d{2})?\s?(?:[–\-]\s?(\d{1,2})(?::\d{2})?\s?)?(?:p\.?\s?m\.?)", re.I)


def pm_as_24h(text):
    """en '2–4 pm' / '9 p.m.' → {14, 16, 21}: translations legitimately use the 24-hour clock."""
    out = set()
    for m in RE_PM.finditer(text):
        for g in (m.group(1), m.group(2)):
            if g and 1 <= int(g) <= 11:
                out.add(float(int(g) + 12))
    return out


def content_text(content):
    return json.dumps({k: v for k, v in (content or {}).items() if k != "sources"}, ensure_ascii=False)


def register_issues(lang, text):
    """(count, examples) of sentences not in the polite/formal register required by SPEC."""
    text = RE_CITATION.sub("", RE_QUOTED.sub(" ", text))
    if lang == "ko":
        hits = [text[max(0, m.start() - 12):m.end()] for m in RE_KO_PLAIN.finditer(text)]
    elif lang == "ja":
        hits = [x.strip()[-14:] for x in RE_JA_SENT.findall(text)
                if RE_JA_PLAIN.search(x.strip()) and not RE_JA_POLITE.search(x.strip())]
    elif lang in RE_INFORMAL:
        hits = [m.group(0) for m in RE_INFORMAL[lang].finditer(text)]
    else:
        hits = []
    return len(hits), hits[:3]


def read_claims(path):
    claims, bad = [], []
    if not path.exists():
        return claims, bad
    for i, line in enumerate(path.read_text(encoding="utf-8").splitlines()):
        if not line.strip():
            continue
        try:
            claims.append(json.loads(line))
        except json.JSONDecodeError as ex:
            bad.append(f"claims.jsonl line {i + 1}: invalid JSON ({ex})")
    return claims, bad


def check(p):
    rid = p.parent.name
    rep = json.loads(p.read_text(encoding="utf-8"))
    meta = json.loads((HERE / "packs" / rid / "meta.json").read_text(encoding="utf-8"))
    cc = (meta.get("country_code") or "").upper()
    local = set(a.CURRENCY_DATA["countries"].get(cc, []))
    errs, warns = [], []
    E, W = errs.append, warns.append
    if not local:
        E(f"no currency data for country {cc!r} — local-currency checks cannot run")

    facts_path = HERE / "country_facts" / f"{cc}.json"
    facts = json.loads(facts_path.read_text(encoding="utf-8")) if facts_path.exists() else {}
    if not facts:
        W(f"no country_facts/{cc}.json")
    # official foreign-currency amounts: (currency family, value) pairs stated in country_facts or quoted
    # verbatim from an official page (claim sourced by an O tag) — e.g. a dorm fee a university lists in USD
    claims, bad_claim_lines = read_claims(p.parent / "claims.jsonl")
    official_text = json.dumps(facts, ensure_ascii=False) + "\n" + "\n".join(
        str(c.get("quote") or "") for c in claims if any(re.fullmatch(r"O\d+", str(t)) for t in c.get("sources") or []))
    official = {(fam, v) for c, v, *_ in a.money_tokens(official_text) for fam in a.cur_family(c).split("|")}

    for lang in LANGS:
        if not isinstance(rep.get(lang), dict):
            E(f"missing language {lang}")
    if errs and not isinstance(rep.get("en"), dict):
        return errs, warns
    en = rep["en"]
    en_body = en.get("body") or ""
    en_content = en.get("content") or {}
    en_heads = re.findall(r"^##\s+.*$", en_body, flags=re.M)
    en_all_text = en.get("summary", "") + "\n" + main_text(en_body) + "\n" + content_text(en_content)
    en_sig = a.significant_numbers(en_all_text)
    en_any = all_numbers(en_all_text) | pm_as_24h(en_all_text)
    en_sources = json.dumps(en_content.get("sources"), sort_keys=True)
    en_links = re.findall(r"\]\((http[^)]+)\)", sources_block(en_body))
    en_leaves = dict(leaves({k: v for k, v in en_content.items() if k != "sources"}))
    if not en_content.get("sources"):
        E("en: content.sources is empty")
    if len(en_links) != len(en_content.get("sources") or []):
        E(f"en: Sources section has {len(en_links)} links but content.sources has {len(en_content.get('sources') or [])}")

    for lang, t in rep.items():
        if not isinstance(t, dict):
            continue
        for k in ("title", "summary", "body", "content"):
            if not t.get(k):
                E(f"{lang}: empty {k}")
        content = t.get("content") if isinstance(t.get("content"), dict) else {}
        body = t.get("body") or ""
        missing = CONTENT_KEYS - set(content)
        if missing:
            E(f"{lang}: content missing keys {sorted(missing)}")
        if json.dumps(content.get("sources"), sort_keys=True) != en_sources:
            E(f"{lang}: content.sources differs from en")
        links = re.findall(r"\]\((http[^)]+)\)", sources_block(body))
        if not links:
            E(f"{lang}: body has no Sources section with links at the end")
        elif links != en_links:
            E(f"{lang}: Sources links differ from en")

        # card fields are short labels
        for sec, key in CARD_FIELDS:
            v = (content.get(sec) or {}).get(key) if isinstance(content.get(sec), dict) else None
            if isinstance(v, str) and len(v) > CARD_MAX:
                E(f"{lang} content.{sec}.{key}: card value longer than {CARD_MAX} chars ({len(v)})")

        texts = [("title", t.get("title") or ""), ("summary", t.get("summary") or ""), ("body", body)]
        texts += [(f"content.{q}", s) for q, s in a.walk_strings({k: v for k, v in content.items() if k != "sources"})]
        joined = "\n".join(s for _, s in texts)
        for field, s in texts:
            if lang != "ko" and a.RE_HANGUL.search(s):
                E(f"{lang} {field}: Hangul in non-ko text: {s[:80]!r}")
            if lang in ("en", "de", "fr", "vi") and (a.RE_KANA.search(s) or a.RE_HAN.search(s)):
                E(f"{lang} {field}: CJK in Latin-script text: {s[:80]!r}")
            if lang == "zh" and a.RE_KANA.search(s):
                E(f"{lang} {field}: kana in zh: {s[:80]!r}")
            if lang == "zh":
                trad = sorted({ch for ch in (main_text(s) if field == "body" else s) if ch in TRADITIONAL_ONLY})
                if trad:
                    E(f"zh {field}: Traditional characters {''.join(trad)} (zh must be Simplified)")
            if (a.RE_CYR.search(s) or a.RE_THAI.search(s)) and cc not in ("RU", "UA", "BY", "KZ", "TH", "BG", "RS"):
                E(f"{lang} {field}: Cyrillic/Thai script: {s[:80]!r}")
            for rx, kind in a.ARTIFACT_PATTERNS:
                m = re.search(rx, s, re.I | re.M)
                if m:
                    E(f"{lang} {field}: {kind}: {a.snippet(s, m.start(), m.end(), 30)!r}")
            if local:
                for cur, val, s0, e0 in a.money_tokens(s):
                    fams = set(a.cur_family(cur).split("|"))
                    if cur == "USD?" and local & {"USD", "AUD", "CAD", "NZD", "SGD", "HKD", "TWD", "MXN", "CLP", "ARS", "COP"}:
                        continue
                    if fams & local or any((f, val) in official for f in fams):
                        continue
                    E(f"{lang} {field}: non-local currency {cur} {val:g} (country {cc}): {a.snippet(a.normalize_numerals(s), s0, e0, 30)!r}")
            for rx, valid in a.VISA_COUNTRY:
                m = rx.search(s)
                if m and cc not in valid:
                    E(f"{lang} {field}: visa term {m.group(0)!r} invalid for {cc}")

        n_informal, examples = register_issues(lang, (rep[lang].get("summary") or "") + "\n" + main_text(body)
                                               + "\n" + content_text(rep[lang].get("content") or {}))
        if n_informal >= {"ko": 3, "ja": 5}.get(lang, 1):
            E(f"{lang}: {n_informal} informal markers, guides must be polite (SPEC register): {examples}")
        elif n_informal:
            W(f"{lang}: {n_informal} possibly informal markers: {examples}")

        # the text really is in the target language
        if lang in RE_SCRIPT_REQUIRED and len(RE_SCRIPT_REQUIRED[lang].findall(main_text(body))) < MIN_SCRIPT_CHARS:
            E(f"{lang}: body has fewer than {MIN_SCRIPT_CHARS} {lang} script characters — not translated?")
        if lang != "en":
            for field, s in [("summary", t.get("summary") or "")] + [("body", para) for para in re.split(r"\n\s*\n", main_text(body))]:
                s = RE_QUOTED.sub(" ", s)  # direct quotes of official English text are allowed
                words = [w.lower() for w in a.RE_LATIN_WORD.findall(s)]
                stop_ratio = sum(w in a.EN_STOP for w in words) / max(1, len(words))
                if lang in RE_SCRIPT_REQUIRED:  # CJK: English only if Latin letters outweigh the target script
                    latin = sum(len(w) for w in a.RE_LATIN_WORD.findall(s))
                    native = len(RE_SCRIPT_REQUIRED[lang].findall(s)) + (len(a.RE_HAN.findall(s)) if lang == "ja" else 0)
                    bad = len(words) >= 20 and stop_ratio > 0.18 and latin > native
                else:
                    bad = len(words) >= 25 and stop_ratio > 0.25
                if bad:
                    E(f"{lang} {field}: untranslated English: {s[:100]!r}")
                    break
        if lang == "vi" and len(a.RE_VI_DIACRITIC.findall(body)) < 50:
            E("vi: diacritics missing")
        if lang == "ko" and len(a.RE_HAN.findall(main_text(body))) >= 3:
            W(f"ko: Han characters in body: {''.join(a.RE_HAN.findall(main_text(body))[:15])}")

        if lang != "en":
            # structure and numbers survive translation, in both directions
            heads = re.findall(r"^##\s+.*$", body, flags=re.M)
            if len(heads) != len(en_heads):
                E(f"{lang}: {len(heads)} ## sections vs en {len(en_heads)}")
            t_all_text = (t.get("summary") or "") + "\n" + main_text(body) + "\n" + content_text(content)
            sig, t_any = a.significant_numbers(t_all_text), all_numbers(t_all_text)
            # a significant number on one side must exist somewhere (any context) on the other
            if sig - en_any:
                E(f"{lang}: numbers not present in en: {sorted(sig - en_any)[:10]}")
            if en_sig - t_any:
                E(f"{lang}: en numbers missing in translation: {sorted(en_sig - t_any)[:10]}")
            t_leaves = dict(leaves({k: v for k, v in content.items() if k != "sources"}))
            for path, ev in en_leaves.items():
                tv = t_leaves.get(path, "<missing>")
                if path.endswith("[]") and ev != tv:
                    E(f"{lang} content.{path[:-2]}: list length {tv} vs en {ev}")
                elif (ev is None) != (tv is None) or isinstance(ev, bool) and ev != tv:
                    E(f"{lang} content.{path}: {tv!r} vs en {ev!r}")
            ratio = len(body) / max(1, len(en_body))
            if ratio < {"ko": 0.3, "ja": 0.3, "zh": 0.22}.get(lang, 0.6):
                W(f"{lang}: body much shorter than en (ratio {ratio:.2f})")
        cur_field = ((content.get("costs") or {}).get("currency") if isinstance(content.get("costs"), dict) else None) or ""
        if local and cur_field not in local:
            E(f"{lang}: costs.currency {cur_field!r} not local {sorted(local)}")

    # claims: every significant en number is backed; sources are real tags
    claims_path = p.parent / "claims.jsonl"
    log_path = p.parent / "research_log.md"
    if not claims_path.exists():
        E("claims.jsonl missing")
        return errs, warns
    log = log_path.read_text(encoding="utf-8") if log_path.exists() else ""
    if not log:
        E("research_log.md missing")
    for msg in bad_claim_lines:
        E(msg)
    for i, c in enumerate(claims):
        srcs = c.get("sources") or []
        if not srcs:
            E(f"claim {i} has no sources: {str(c.get('claim'))[:80]!r}")
        for tag in srcs:
            if m := re.fullmatch(r"C:([A-Z]{2})\.([\w.]+)", str(tag)):
                node = json.loads((HERE / "country_facts" / f"{m.group(1)}.json").read_text(encoding="utf-8")) \
                    if (HERE / "country_facts" / f"{m.group(1)}.json").exists() else None
                for part in m.group(2).split("."):
                    node = node.get(part) if isinstance(node, dict) else None
                if node is None:
                    E(f"claim {i}: country_facts tag {tag!r} does not resolve")
            elif re.fullmatch(r"[OVW]\d+", str(tag)):
                if not re.search(rf"(?<![\w]){re.escape(tag)}(?![\w])", log):
                    E(f"claim {i}: source tag {tag!r} not listed in research_log.md")
            else:
                E(f"claim {i}: unknown source tag format {tag!r}")
    claim_bag = a.number_bag(" ".join(str(c.get("claim") or "") + " " + str(c.get("quote") or "") for c in claims))
    unbacked = sorted(v for v in en_sig if v not in claim_bag)
    if unbacked:
        E(f"en: numbers not backed by any claim: {unbacked[:12]}")
    return errs, warns


def main(path):
    try:
        errs, warns = check(Path(path).resolve())
    except Exception:  # malformed input must fail closed with a readable line
        print("ERROR", "validator crashed on malformed input:\n" + traceback.format_exc(limit=3))
        print("1 errors, 0 warnings")
        return 1
    for w in warns:
        print("WARN ", w)
    for e in errs:
        print("ERROR", e)
    print(f"{len(errs)} errors, {len(warns)} warnings")
    return 1 if errs else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
