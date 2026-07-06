import urllib.parse, urllib.request, json, time, csv, re, sys
UA={'User-Agent':'OvloReportAudit/1.0 (research; duswokim1220@gmail.com)'}
SD="/private/tmp/claude-501/-Users-kim-yeonjae-Desktop-Project-Ovlo/cba51df1-b3bd-48a5-9a59-22dda715bd7e/scratchpad/"
def get(u):
    for _ in range(3):
        try: return json.load(urllib.request.urlopen(urllib.request.Request(u,headers=UA),timeout=25))
        except Exception: time.sleep(1.0)
    return None
LANGS=["ko","ja","zh","de","fr","es","vi"]
def toks(s): return set(re.findall(r'[a-z0-9]+', (s or '').lower())) - {"the","of","university","de","la","el"}
rows=[l.rstrip('\n').split('\t') for l in open(SD+"univ_list.tsv") if l.strip()]
out=open(SD+"wikidata_names.csv","w",newline='')
w=csv.writer(out); w.writerow(["id","name_en","qid","matched_label","match_flag"]+LANGS)
found=0
for i,r in enumerate(rows):
    uid,name_en=r[0],r[1]
    s=get("https://www.wikidata.org/w/api.php?"+urllib.parse.urlencode(
        {"action":"wbsearchentities","search":name_en,"language":"en","type":"item","limit":1,"format":"json"}))
    if not s or not s.get("search"):
        w.writerow([uid,name_en,"","not_found",""]+[""]*len(LANGS)); continue
    qid=s["search"][0]["id"]
    e=get("https://www.wikidata.org/w/api.php?"+urllib.parse.urlencode(
        {"action":"wbgetentities","ids":qid,"props":"labels","languages":"|".join(LANGS+["en"]),"format":"json"}))
    lab=(e or {}).get("entities",{}).get(qid,{}).get("labels",{}) if e else {}
    mlab=lab.get("en",{}).get("value","")
    flag="ok" if (toks(name_en) & toks(mlab)) else "check"
    if flag=="ok": found+=1
    w.writerow([uid,name_en,qid,mlab,flag]+[lab.get(l,{}).get("value","") for l in LANGS])
    if i%50==0: out.flush(); print(f"{i}/{len(rows)} ok={found}",flush=True)
    time.sleep(0.18)
out.close(); print(f"DONE {len(rows)} rows, name-matched(ok)={found}",flush=True)
