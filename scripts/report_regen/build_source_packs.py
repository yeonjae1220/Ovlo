"""Build one source pack (markdown) per pilot report from the NAS pipeline files + prod snapshot.

Usage: python3 -I build_source_packs.py 70 1 143 ...
Writes regen/packs/<report_id>/source_pack.md and meta.json. Read-only on all inputs.
"""
import csv
import json
import os
import re
import sys
from pathlib import Path

csv.field_size_limit(10**9)
# Working data lives outside git (raw prod text): OVLO_AUDIT_DIR (default output/audit-20261029)
# holds snapshot/ and regen/{packs,out,country_facts,verify}. See docs/report-regeneration.md.
REPO = Path(__file__).resolve().parents[2]
AUDIT = Path(os.environ.get("OVLO_AUDIT_DIR", REPO / "output" / "audit-20261029")).resolve()
HERE = AUDIT / "regen"
SNAP = AUDIT / "snapshot"
# NAS pipeline exports copied locally (\\NAS\data-pipeline → Collected_datas), see docs/report-regeneration.md
SRC = Path(os.environ.get("OVLO_PIPELINE_SRC", Path.home() / "Desktop/Project/Collected_datas/nas_pipeline_20261029"))
MAX_VIDEOS = 25
TRANSCRIPT_CHARS = 3000
WEB_CHARS = 6000


def fence(text):
    """Untrusted transcript/web text goes inside a code fence; neutralise fences inside it."""
    return (text or "").replace("```", "ʼʼʼ")


def jl(path):
    with open(path, encoding="utf-8") as f:
        return [json.loads(l) for l in f if l.strip()]


def rows(name, enc="utf-8"):
    with open(SRC / "exports" / name, encoding=enc) as f:
        return list(csv.DictReader(f))


def main(report_ids):
    reports = {r["report_id"]: r for r in jl(SNAP / "reports.jsonl")}
    gu = {g["id"]: g for g in jl(SNAP / "global_univ.jsonl")}
    eu = {e["id"]: e for e in jl(SNAP / "exchange_univ.jsonl")}
    names = {}
    for n in jl(SNAP / "names.jsonl"):
        names.setdefault(n["global_univ_id"], {})[n["lang"]] = n["name"]
    trs = {}
    for t in jl(SNAP / "translations.jsonl"):
        if t["report_id"] in report_ids and t["lang"] == "en":
            trs[t["report_id"]] = t
    vd_by_url = {v["url"]: v for v in rows("video_data.csv")}
    approved = rows("video_data_approved.csv", "utf-8-sig")
    rematch = rows("video_data_rematch_matched.csv")
    crawled = jl(SRC / "web_cache/crawled.jsonl")
    pdfs = jl(SRC / "web_cache/pdf_crawled.jsonl")
    web_programs = jl(SNAP / "web_programs.jsonl")

    for rid in report_ids:
        r = reports[rid]
        g, e = gu.get(r["global_univ_id"], {}), eu.get(r["exchange_univ_id"], {})
        city = e.get("city") or g.get("city") or ""
        keys = {k.lower() for k in [g.get("name_en"), e.get("name_en"), e.get("name_ko"), *names.get(g.get("id"), {}).values()] if k}

        vids = [v for v in approved if int(v["university_id"]) == r["exchange_univ_id"]]
        vids += [v for v in rematch if v["global_univ_id"] and int(v["global_univ_id"]) == r["global_univ_id"]]
        seen, packed = set(), []
        for v in vids:
            if v["url"] in seen:
                continue
            seen.add(v["url"])
            full = vd_by_url.get(v["url"], {})
            transcript = (full.get("transcript") or "").strip()
            title_hit = any(k in (v["title"] or "").lower() for k in keys)
            packed.append((title_hit, len(transcript), v, transcript))
        packed.sort(key=lambda x: (not x[0], -x[1]))
        packed = packed[:MAX_VIDEOS]

        pages = [c for c in crawled if str(c.get("univ_id")) == str(r["global_univ_id"])]
        pages += [p for p in pdfs if str(p.get("univ_id")) == str(r["global_univ_id"])]
        progs = [w for w in web_programs if w.get("global_univ_id") == r["global_univ_id"]]

        out = HERE / "packs" / str(rid)
        out.mkdir(parents=True, exist_ok=True)
        L = [f"# Source pack — report {rid}", "",
             "## University", "",
             f"- global_univ_id: {r['global_univ_id']} / exchange_univ_id: {r['exchange_univ_id']}",
             f"- name_en: {g.get('name_en')} (exchange: {e.get('name_en')})",
             f"- country_code: {g.get('country_code') or e.get('country_code')} / city: {city}",
             f"- website: {e.get('website') or g.get('domain') or ''}",
             f"- names by language: {json.dumps(names.get(g.get('id'), {}), ensure_ascii=False)}", ""]
        L += [f"## Student videos ({len(packed)} of {len(seen)} matched; ordered: title mentions this university first)",
              "Each video was auto-assigned to this university by the old pipeline. **Verify it is actually about this university before using it.**", ""]
        for i, (hit, _, v, tr) in enumerate(packed, 1):
            L += [f"### V{i}: {v['title']}",
                  f"- url: {v['url']} / channel: {v['channel']} / published: {(v.get('published_at') or '')[:10]} / lang: {v.get('source_lang')} / title_mentions_univ: {hit}",
                  f"- pipeline summary: {(v.get('summary') or '').strip()[:800]}",
                  f"- transcript (first {TRANSCRIPT_CHARS} chars):", "", "```", fence(tr[:TRANSCRIPT_CHARS]) or "(none)", "```", ""]
        L += [f"## Crawled web pages ({len(pages)})", ""]
        for i, p in enumerate(pages, 1):
            L += [f"### W{i}: {p.get('source_url') or p.get('url')}", "", "```", fence((p.get("text") or "")[:WEB_CHARS]), "```", ""]
        L += [f"## web_exchange_programs rows ({len(progs)}) — LLM-extracted from crawled pages, treat as leads", ""]
        for p in progs:
            slim = {k: v for k, v in p.items() if v not in (None, "", [], {}) and k not in ("created_at", "updated_at")}
            L += ["```json", json.dumps(slim, ensure_ascii=False, indent=1)[:4000], "```", ""]
        old = trs.get(rid)
        if old:
            L += ["## OLD report (en) — UNTRUSTED, generated by an 8B model; known to contain invented facts.",
                  "Use only as a checklist of topics, never as a source.", "", "```", fence(old["body"][:6000]), "```"]
        (out / "source_pack.md").write_text("\n".join(L), encoding="utf-8")
        (out / "meta.json").write_text(json.dumps({
            "report_id": rid, "global_univ_id": r["global_univ_id"], "exchange_univ_id": r["exchange_univ_id"],
            "name_en": g.get("name_en"), "country_code": g.get("country_code") or e.get("country_code"), "city": city,
            "names": names.get(g.get("id"), {}), "videos": len(packed), "pages": len(pages), "programs": len(progs),
        }, ensure_ascii=False, indent=1), encoding="utf-8")
        print(rid, g.get("name_en"), f"videos={len(packed)}/{len(seen)} pages={len(pages)} programs={len(progs)}",
              f"{(out / 'source_pack.md').stat().st_size // 1024}KB")


if __name__ == "__main__":
    main([int(x) for x in sys.argv[1:]])
