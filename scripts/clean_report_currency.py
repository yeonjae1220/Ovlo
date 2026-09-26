#!/usr/bin/env python3
"""Offline, conservative currency cleanup. No database/network calls.

Only exact changed string values are written to SQL; visa sections and facts are
never removed. Ambiguous costs are retained and exported for focused review.
"""
import argparse
import copy
import json
import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

DATA = json.loads(Path(__file__).with_name('report_currency_data.json').read_text(encoding='utf-8'))
ALIASES = {k.casefold(): v for k, v in DATA['aliases'].items()}
def alias_pattern(words):
    tree={}
    for word in words:
        node=tree
        for char in word.casefold(): node=node.setdefault(char,{})
        node['']=None
    def emit(node):
        branches=[re.escape(char)+emit(child) for char,child in node.items() if char]
        tail='(?:'+'|'.join(branches)+')' if branches else ''
        return tail+'?' if '' in node and tail else tail
    return emit(tree)

CURRENCY = alias_pattern(list(ALIASES)+['$', '¥', '￥'])

UNIT = r'(?:[万萬億亿千百十만천억]|백만|(?:[kKmMbB]|million(?:s)?|Millionen|billion(?:s)?|thousand|tausend|mille|Mio\.|triệu|nghìn)(?![A-Za-zÀ-ž]))'
NUMBER = r'\d(?:[\d.,\u00a0 ]*\d)?'
VALUE = NUMBER + r'(?:\s*' + UNIT + r')*(?:\d[\d.,]*(?:' + UNIT + r')+)*'
AMOUNT = VALUE + r'(?:\s*(?:[-–—〜～~]|to|bis|à|đến|至|到)\s*' + VALUE + r')?'
# Number and currency must be adjacent except for scale words and French "de".
MONEY = re.compile(r'(?<![A-Za-z])(?P<pre>' + CURRENCY + r')\s*(?P<pnum>' + AMOUNT + r')|(?P<snum>' + AMOUNT + r')\s*(?:de\s+)?(?P<post>' + CURRENCY + r')(?![A-Za-z])', re.I)
APPROX = re.compile(r'^(?:\s|[≈~≃=*]|approximately\b|approx\.?\b|around\b|about\b|roughly\b|equivalent to\b|ca\.|etwa\b|um\b|environ\b|khoảng\b|약|約|约|대략|대략적으로|およそ)+', re.I)
UNITS = re.compile(r'^[\s*_/.,:;，、：()（）\[\]~≈–—-]*(?:(?:per|a|each|pro|par|mỗi)\s*)?(?:month|mo|Monat|Monate|Monaten|mois|tháng|monthly|week|weeks|year|annually|hour|hr|월|개월|달|月|年|년|주|시간|日|일|시간당|每月|毎月|月額)?[\s*_/.,:;，、：()（）\[\]~≈–—-]*$', re.I)
CONVERSION = re.compile(r'(?:equivalent\s+to|approximately|approx\.?|around|about|roughly|converted\s+to|environ|etwa|entspricht|umgerechnet|khoảng|tương đương|약|約|约|相当|换算|換算|환산|≈)', re.I)
D2 = re.compile(r'(?<![A-Za-z0-9])D[\s‐‑–—-]*2(?![0-9])', re.I)
# Used for auditing only, never a license to delete a sentence or visa section.
KRW_WORD = re.compile(r'\bKRW\b|₩|\bwons?\b|ウォン|韩元|韓元|\d\s*원', re.I)
FIELDS = ('title', 'summary', 'body', 'content')
COST_KEYS = {'cost','price','monthly_total','rent','food','transport','entertainment','dorm_price','offcampus_price','monthly_pass','avg_hourly_wage'}


@dataclass
class Money:
    start: int
    end: int
    currency: str | None
    text: str


def monies(text, local):
    return _monies(text,tuple(local))


@lru_cache(maxsize=32768)
def _monies(text, local):
    if not re.search(r'\d', text): return []
    result = []
    for m in MONEY.finditer(text):
        token = (m['pre'] or m['post']).casefold()
        currency = ALIASES.get(token)
        if token in ('¥', '￥'):
            currency = next((c for c in local if c in ('JPY','CNY')), None)
        if token == '$':
            currency = next((c for c in local if c in ('USD','CAD','AUD','NZD','SGD','HKD','TWD','MXN','ARS','CLP','COP','UYU')), None)
        end=m.end()
        # An explicit code disambiguates a bare symbol (e.g. "$50 USD").
        explicit=re.match(r'\s*(USD|CAD|AUD|NZD|SGD|HKD|TWD|JPY|CNY)\b',text[end:],re.I)
        if explicit and token in ('$', '¥', '￥'):
            currency=explicit[1].upper();end+=explicit.end()
        item = Money(m.start(), end, currency, text[m.start():end])
        if explicit and token=='$' and result and re.fullmatch(r'\$[\d., ]+',result[-1].text) and re.fullmatch(r'\s*[-–—~]\s*',text[result[-1].end:item.start]):
            result[-1]=Money(result[-1].start,result[-1].end,currency,result[-1].text)
        if result and item.currency == result[-1].currency and re.fullmatch(r'\s*(?:[-–—〜～~]|to|bis|à|至|到)\s*',text[result[-1].end:item.start]):
            last = result.pop(); item = Money(last.start,item.end,currency,text[last.start:item.end])
        result.append(item)
    return result


def parens(text):
    """Balanced, single-line literal delimiters; no greedy block regex."""
    stack = []
    pairs = []
    for i, c in enumerate(text):
        if c in '\r\n': stack.clear()
        elif c in '(（': stack.append((i,c))
        elif c in ')）' and stack:
            start, opening = stack.pop()
            pairs.append((start,i+1))
    return pairs


def price_only(text, local):
    spans = monies(text,local)
    if not spans: return False
    for m in reversed(spans): text = text[:m.start] + text[m.end:]
    text=APPROX.sub('',text).strip()
    text=re.sub(r'(?i)monthly|monatlich|mensuels?|per month|par mois|mỗi tháng|월간|월|月額|每月|毎月|정도|相当|정도|約|约|약|coréens?|Hàn Quốc|相当于','',text)
    return bool(UNITS.fullmatch(text.strip()))


def trim_approx(text, start):
    # Include only a contiguous approximation prefix, not preceding prose.
    prefix = text[:start]
    found = re.search(r'(?:[≈~≃]|\bapproximately|\bapprox\.?|\baround|\babout|\benviron|\betwa|\bkhoảng|약|約|约)\s*[*_]*\s*$',prefix,re.I)
    return found.start() if found else start


# Narrow fee clauses preserve the visa/processing sentence around them.
FEE_PREFIX = re.compile(
    r"(?:,\s*(?:with monthly prices ranging from|mit monatlichen Preisen zwischen|avec des prix mensuels variant de|which costs|costing|at a cost of|dont le coût[^,.;。]*|avec des frais de|với chi phí|chi phí|das etwa|die etwa)|"
    r"(?:[，、,]\s*|(?<=[。！？])|^)(?:월별 가격은|월간 가격은|月額料金は|月租金范围为|비용은|비용이|수수료는|费用(?:约为|为|约)?|費用(?:は|が)|費用約|料金は))"
    r"[^\d\n.;。]*$", re.I)
FEE_SUFFIX = re.compile(r"^[*_]*(?:\s*(?:kostet|입니다|이며[,，]?|で[、,]|です))?", re.I)


def remove_fee_clauses(text, local):
    if 'KRW' in local: return text
    for m in reversed(monies(text,local)):
        if m.currency!='KRW': continue
        # Prefix search is bounded to the current line and 100 preceding chars.
        start=max(text.rfind('\n',0,m.start)+1,m.start-100)
        prefix=FEE_PREFIX.search(text[start:m.start])
        if not prefix: continue
        left=start+prefix.start()
        suffix=FEE_SUFFIX.match(text[m.end:])
        right=m.end+suffix.end()
        if left==0 and right<len(text) and text[right] in ',，、':right+=1
        text=text[:left]+text[right:]
    return re.sub(r'해야 하며(?=\.)','해야 합니다',text)


COST_WORD = re.compile(r'cost|price|rent|fee|wage|expense|budget|adds|Kosten|Preis|Miete|Gebühr|Stundenlohn|Ausgaben|coût|élèvent|prix|loyer|frais|salaire|dépense|chi phí|thêm khoảng|giá|lương|tiền thuê|지출|비용|가격|월세|요금|임대료|시급|식비|교통비|开支|支出|增加|费用|价格|月租|租金|票价|时薪|費用|料金|家賃|価格|時給|月額', re.I)
NON_COST = re.compile(r'visa|visum|비자|ビザ|签证|簽證|thị thực|processing|process takes|documents|passport|required|permit|allowed|legal|availability|\blimited\b|apply|application|proof|funds|insurance|필요|신청|서류|여권|처리|허가|가능|제한|공급|수락|보험|証明|申請|必要|書類|処理|許可|手続|空き|保険|证明|申请|需要|必须|材料|办理|允许|有限|供应|保险|verfügbar|begrenzt|erforderlich|Dokument|Antrag|genehmigt|Versicherung|disponib|limité|exigé|demande|documents|assurance|cần|bắt buộc|hồ sơ|xử lý|giấy|hạn chế|bảo hiểm', re.I)
SENTENCE_BREAK = re.compile(r'(?<=[.!?])(?<!Mio\.)(?<!ca\.)(?<!approx\.)([ \t]+)(?=[A-Za-zÀ-ž가-힣*_])|(?<=[。！？])|[\r\n]+')


def remove_cost_sentences(text,local):
    # Only pure cost statements with no local price and no procedural/housing
    # availability/permission information. Everything else remains a review item.
    if 'KRW' in local:return text
    cuts=[];start=0
    for boundary in list(SENTENCE_BREAK.finditer(text))+[None]:
        end=boundary.start() if boundary else len(text)
        sentence=text[start:end];ms=monies(sentence,local)
        # A heading, named place, or mixed clause is not a pure cost statement.
        # Fail closed instead of deleting useful prose based on keyword absence.
        remainder=sentence
        for m in reversed(ms): remainder=remainder[:m.start]+remainder[m.end:]
        mixed=re.search(r'[,，、:：;；#*]|\b(?:and|but|while|though|with|including|if|und|aber|mit|avec|mais|tandis|với|nhưng|tuy nhiên)\b',remainder,re.I)
        if ms and all(m.currency=='KRW' for m in ms) and COST_WORD.search(sentence) and not NON_COST.search(sentence) and not mixed:
            cuts.append((start,boundary.end() if boundary and '\n' not in boundary[0] else end))
        start=boundary.end() if boundary else len(text)
    for a,b in reversed(cuts):text=text[:a]+text[b:]
    return text


def clean_string(value, local, structured=False):
    return _clean_string(value,tuple(local),structured)


@lru_cache(maxsize=32768)
def _clean_string(value, local, structured=False):
    if not value or not local or not re.search(r"\d",value): return value
    if not re.search(r"[()（）/≈=]|KRW|won|원|ウォン|韩元|韓元",value,re.I) and not CONVERSION.search(value): return value
    text = value
    # Process inner parentheses first; restart after an edit to keep spans exact.
    while True:
        changed = False
        for start,end in parens(text):
            inner = text[start+1:end-1]
            inside = monies(inner,local)
            outside = monies(text[:start],local)
            previous = outside[-1] if outside and re.fullmatch(r'[\s*_]*',text[outside[-1].end:start]) else None
            if not inside: continue
            local_inside = [m for m in inside if m.currency in local]
            if local_inside and previous and previous.currency not in local and previous.currency is not None and price_only(inner,local):
                # Local amount already present: retain its exact digits/symbols.
                closing=text[previous.end:start].strip()
                replacement = APPROX.sub('',inner).strip()
                if closing: replacement=replacement.strip('*_')+closing
                text = text[:previous.start] + replacement + text[end:]
                changed=True; break
            if (previous and previous.currency in local) or (all(m.currency=='KRW' for m in inside) and 'KRW' not in local):
                foreign = [m for m in inside if m.currency and m.currency not in local]
                if not foreign: continue
                if len(foreign)==len(inside) and price_only(inner,local):
                    cut = start
                    while cut > (previous.end if previous else 0) and text[cut-1] in ' \t': cut-=1
                    text=text[:cut]+text[end:];changed=True;break
                modified=inner
                for m in reversed(foreign):
                    modified=modified[:trim_approx(modified,m.start)] + modified[m.end:]
                modified=modified.strip(' \t,;，、：:')
                # Mixed parentheses preserve their non-price annotation exactly.
                text=text[:start]+(text[start]+modified+text[end-1] if modified else '')+text[end:]
                changed=True;break
        if not changed: break
    # Explicit adjacent conversion outside parentheses. Do not treat arbitrary
    # foreign prices elsewhere in a sentence as conversions.
    spans=monies(text,local)
    for i in range(len(spans)-1,0,-1):
        left,right=spans[i-1],spans[i]
        gap=text[left.end:right.start]
        if '\n' in gap or len(gap)>65: continue
        connector=bool(re.fullmatch(r'\s*[/≈=]\s*',gap) or re.fullmatch(r'\s*[,/≈=]?\s*(?:equivalent to|approximately|approx\.?|around|about|roughly|converted to|environ|etwa|entspricht|umgerechnet|khoảng|tương đương|약|約|约|相当|换算|換算|환산)\s*',gap,re.I))
        if not connector: continue
        if left.currency in local and right.currency and right.currency not in local:
            text=text[:left.end]+text[right.end:]
        elif right.currency in local and left.currency and left.currency not in local:
            text=text[:left.start]+text[right.start:]
    if not structured:
        text=remove_cost_sentences(remove_fee_clauses(text,local),local)
    spans=monies(text,local)
    if structured and spans and all(m.currency=='KRW' for m in spans) and 'KRW' not in local:
        return None
    return text


def walk(value,local,path,exceptions):
    if isinstance(value,dict):
        result={}
        for key,item in value.items():
            transformed=walk(item,local,path+[key],exceptions)
            if transformed is not None or item is None: result[key]=transformed
        return result
    if isinstance(value,list):
        return [walk(item,local,path+[str(i)],exceptions) for i,item in enumerate(value)]
    if not isinstance(value,str): return value
    structured=path[0]=='content' and path[-1] in COST_KEYS
    new=clean_string(value,local,structured)
    if new:
        remaining=[m for m in monies(new,local) if m.currency=='KRW' and 'KRW' not in local]
        for m in remaining:
            exceptions.append({'path':'.'.join(path),'reason':'standalone_or_ambiguous_krw','fragment':new[max(0,m.start-90):min(len(new),m.end+90)]})
    return new


def clean_row(row):
    result=copy.deepcopy(row); issues=[]
    local=DATA['countries'].get(row.get('country_code'),[])
    if not local:
        return result,[{'path':'country_code','reason':'unknown_country','fragment':str(row.get('country_code'))}]
    for key in ('summary','body','content'):
        result[key]=walk(row.get(key),local,[key],issues)
    costs=result['content'].get('costs') if isinstance(result['content'],dict) else None
    if isinstance(costs,dict):
        label=costs.get('currency')
        # Only reconcile a recognized label, never relabel a numerical price.
        currency=ALIASES.get(str(label).casefold()) if label else None
        if currency and currency not in local:
            actual={m.currency for k,v in costs.items() if k in COST_KEYS and isinstance(v,str) for m in monies(v,local)}
            if actual and actual.issubset(set(local)): costs['currency']=next(iter(actual))
            elif currency=='KRW' or not actual: costs.pop('currency',None)
            else: issues.append({'path':'content.costs.currency','reason':'nonlocal_currency_retained','fragment':str(label)})

    return result,issues


def literal(value):
    if value is None: return 'NULL'
    if not isinstance(value,str): value=json.dumps(value,ensure_ascii=False)
    return "'"+value.replace("'","''")+"'"


def sql_files(changes,directory,run_id):
    if not re.fullmatch(r'[a-z][a-z0-9_]{0,39}',run_id): raise ValueError('Invalid run id')
    backup='report_currency_backup_'+run_id
    sql=['\\set ON_ERROR_STOP on','BEGIN;',"SET LOCAL lock_timeout='5s';","SET LOCAL statement_timeout='60s';","SET LOCAL standard_conforming_strings=on;",
         'CREATE TEMP TABLE currency_expected (report_id bigint,lang text,expected jsonb,replacement jsonb,PRIMARY KEY(report_id,lang));']
    for old,new in changes:
        sql.append(f"INSERT INTO currency_expected VALUES ({int(old['report_id'])},{literal(old['lang'])},{literal({k:old[k] for k in FIELDS})}::jsonb,{literal({k:new[k] for k in FIELDS})}::jsonb);")
    sql += ['LOCK TABLE university_report_translation IN SHARE ROW EXCLUSIVE MODE;',"""
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM currency_expected e LEFT JOIN university_report_translation t USING(report_id,lang)
 WHERE t.report_id IS NULL OR jsonb_build_object('title',t.title,'summary',t.summary,'body',t.body,'content',t.content) IS DISTINCT FROM e.expected)
 THEN RAISE EXCEPTION 'Snapshot changed: export and review again'; END IF;
END $$;
""",f'CREATE TABLE {backup} (report_id bigint,lang text,before_row jsonb NOT NULL,after_row jsonb,PRIMARY KEY(report_id,lang));',
    f'INSERT INTO {backup}(report_id,lang,before_row) SELECT t.report_id,t.lang,to_jsonb(t) FROM university_report_translation t JOIN currency_expected e USING(report_id,lang);',
    """UPDATE university_report_translation t SET title=e.replacement->>'title',summary=e.replacement->>'summary',body=e.replacement->>'body',content=e.replacement->'content',updated_at=now()
FROM currency_expected e WHERE t.report_id=e.report_id AND t.lang=e.lang;""",
    """DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM currency_expected e JOIN university_report_translation t USING(report_id,lang)
 WHERE jsonb_build_object('title',t.title,'summary',t.summary,'body',t.body,'content',t.content) IS DISTINCT FROM e.replacement)
 THEN RAISE EXCEPTION 'Stored result differs from reviewed replacement'; END IF;
END $$;""",
    f'UPDATE {backup} b SET after_row=to_jsonb(t) FROM university_report_translation t WHERE t.report_id=b.report_id AND t.lang=b.lang;',
    f'SELECT count(*) AS backed_up_rows FROM {backup};']
    (directory/'review.sql').write_text('\n'.join(sql+['ROLLBACK;'])+'\n',encoding='utf-8')
    (directory/'apply.sql').write_text('\n'.join(sql+['COMMIT;'])+'\n',encoding='utf-8')
    rollback=f"""\\set ON_ERROR_STOP on
BEGIN;
SET LOCAL lock_timeout='5s';
LOCK TABLE university_report_translation IN SHARE ROW EXCLUSIVE MODE;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM {backup} b LEFT JOIN university_report_translation t USING(report_id,lang) WHERE t.report_id IS NULL OR to_jsonb(t) IS DISTINCT FROM b.after_row)
 THEN RAISE EXCEPTION 'Post-cleanup edits found: do not overwrite'; END IF;
END $$;
UPDATE university_report_translation t SET title=b.before_row->>'title',summary=b.before_row->>'summary',body=b.before_row->>'body',content=b.before_row->'content',updated_at=(b.before_row->>'updated_at')::timestamptz
FROM {backup} b WHERE t.report_id=b.report_id AND t.lang=b.lang;
COMMIT;
"""
    (directory/'rollback.sql').write_text(rollback,encoding='utf-8')
    return backup


def generate(rows,directory,run_id):
    directory.mkdir(parents=True,exist_ok=True)
    changes=[];after=[];issues=[];edits=[];visa_audit=[]
    for index,row in enumerate(rows):
        if index and index % 1000 == 0: print(f'Processed {index}/{len(rows)} rows', flush=True)
        new,exceptions=clean_row(row)
        for issue in exceptions: issues.append({'report_id':row['report_id'],'lang':row['lang'],'country':row.get('country_code'),**issue})
        after.append(new)
        if D2.search(json.dumps([row[k] for k in FIELDS],ensure_ascii=False)) and row.get('country_code')!='KR':
            visa_audit.append({'report_id':row['report_id'],'lang':row['lang'],'reason':'D-2 outside KR; preserved for separate factual review'})
        if any(new[k]!=row[k] for k in FIELDS):
            changes.append((row,new))
            for k in FIELDS:
                if new[k]!=row[k]: edits.append({'report_id':row['report_id'],'lang':row['lang'],'field':k,'before':row[k],'after':new[k]})
        if clean_row(new)[0]!=new: raise ValueError(f"Non-idempotent {row['report_id']}/{row['lang']}")
        if new['title']!=row['title']: raise ValueError('Title changed')
        if 'visa' in row['content'] and 'visa' not in new['content']: raise ValueError('Visa card removed')
        for k in ('type','duration','processing_days'):
            if row['content'].get('visa',{}).get(k)!=new['content'].get('visa',{}).get(k): raise ValueError('Visa fact changed')
        for k in ('legal_limit','part_time_allowed'):
            if row['content'].get('work',{}).get(k)!=new['content'].get('work',{}).get(k): raise ValueError('Work permission changed')
    for filename,items in [('after.jsonl',after),('changes.jsonl',edits),('exceptions.jsonl',issues),('visa-audit.jsonl',visa_audit)]:
        (directory/filename).write_text(''.join(json.dumps(x,ensure_ascii=False)+'\n' for x in items),encoding='utf-8')
    backup=sql_files(changes,directory,run_id)
    summary={'rows':len(rows),'changed_rows':len(changes),'changed_reports':len({r['report_id'] for r,_ in changes}),'exceptions':len(issues),'visa_cards_removed':0,'backup_table':backup}
    (directory/'summary.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8')
    return summary


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('snapshot',type=Path);parser.add_argument('output',type=Path)
    parser.add_argument('--run-id',required=True)
    args=parser.parse_args()
    rows=[json.loads(s) for s in args.snapshot.read_text(encoding='utf-8').splitlines()]
    print(json.dumps(generate(rows,args.output,args.run_id),indent=2))
