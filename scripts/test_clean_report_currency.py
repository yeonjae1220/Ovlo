import unittest,tempfile
from pathlib import Path
from clean_report_currency import clean_string,clean_row,generate,monies

class CurrencyCleanupTest(unittest.TestCase):
 def test_foreign_conversions(self):
  for raw,expected,local in [
   ('€600 (≈60,000 won)','€600',['EUR']),
   ('€600 (≈US$650)','€600',['EUR']),
   ('60,000 won (≈€600)','€600',['EUR']),
   ('₩600,000 (≈US$450)','₩600,000',['KRW']),
   ('€600 (약 60,000원, 단수 입국 기준)','€600 (단수 입국 기준)',['EUR']),
   ('€600 / 약 60,000원','€600',['EUR']),
   ('€600, equivalent to KRW 60,000','€600',['EUR']),
   ('€600（約6万ウォン）','€600',['EUR']),
   ('€600 (environ 60 000 wons)','€600',['EUR']),
   ('€600 (khoảng 60.000 won Hàn Quốc)','€600',['EUR']),
   ('€600 (约6万韩元)','€600',['EUR']),
   ('€600 (etwa 60 Tausend Won)','€600',['EUR']),
   ('£700–£900（110,000–135,000 KRW)','£700–£900',['GBP']),
   ('60,000 원 (~$50 USD)','$50 USD',['USD']),
   ('**$150–$250 USD** (40–60 million COP)','**40–60 million COP**',['COP']),
   ('**$60 USD** (equivalent to 60,000 COP)','**60,000 COP**',['COP']),
   ('**$150〜$250 USD**（40〜60百万 COP）','**40〜60百万 COP**',['COP']),
   ('**$400 to $600 USD** (40–60 million COP)','**40–60 million COP**',['COP']),
   ('€1,200–€1,500 (120–150k won)','€1,200–€1,500',['EUR']),
  ]:
   with self.subTest(raw=raw): self.assertEqual(expected,clean_string(raw,local))
 def test_non_price_information(self):
  for raw in ['D-2 student visa','Processing takes 4–8 weeks.','Visa costs €600 (single entry).','Students won awards.','스워ンジ大学','Costs €600. Travel to Japan costs ¥8000.','USD 500 tuition','Visa costs $600 (single entry).']:
   self.assertEqual(raw,clean_string(raw,['EUR']))
 def test_korean_local_prices(self):
  for raw in ['50만 원','₩600,000','KRW 600,000 (월세 포함)']:
   self.assertEqual(raw,clean_string(raw,['KRW']))
 def test_standalone_structured_krw(self):
  self.assertIsNone(clean_string('60,000 won',['EUR'],True))
  self.assertIsNone(clean_string('60,000 KRW/month',['EUR'],True))
 def test_pure_cost_sentence_removed_but_requirements_retained(self):
  self.assertEqual('Apply early.',clean_string('Monthly costs average 150 million won. Apply early.',['EUR']))
  text='Required documents include proof of funds of 100,000 KRW.'
  self.assertEqual(text,clean_string(text,['EUR']))
  text='Dorm availability is limited, with monthly prices of 60 million won.'
  self.assertEqual(text,clean_string(text,['EUR']))
 def test_krw_only_parentheses_preserve_context(self):
  for raw,expected in [
    ('Dorms (40–60 million won) are available.','Dorms are available.'),
    ('Proof of funds (around 100–130 million won monthly).','Proof of funds.'),
    ('資金証明（月額約100〜130百万ウォン）、保険。','資金証明、保険。'),
    ('住房（40–60百万韩元）和保险。','住房和保险。'),
  ]:
   self.assertEqual(expected,clean_string(raw,['EUR']))
 def test_bold_fee_clause_does_not_leave_markdown(self):
  self.assertEqual('Apply for a visa.',clean_string('Apply for a visa, which costs approximately **60,000 won**.',['EUR']))
 def test_unknown_currency_not_relabelled(self):
  self.assertEqual('$600 (approx. €550)',clean_string('$600 (approx. €550)',['EUR']))
 def test_mixed_cost_prose_is_preserved(self):
  for text in ['Off-campus options are available near Downtown, with rents of 80 million KRW.', 'Transport costs 7 million KRW, though biking can reduce expenses.', '## Housing costs 60 million KRW.', 'Costs €600, and flights cost about USD 800.']:
   self.assertEqual(text,clean_string(text,['EUR']))
 def test_scale_tokens_do_not_consume_following_words(self):
  self.assertEqual('€600',monies('€600 kostet',['EUR'])[0].text)
  self.assertEqual('£50',monies('£50 monthly',['GBP'])[0].text)
  self.assertEqual('£50',monies('£50 mỗi tháng',['GBP'])[0].text)
  self.assertEqual('$160 USD',monies('$160 USD입니다',['USD'])[0].text)
 def row(self):
  return dict(report_id=70,lang='en',country_code='DE',title='Munich',summary='Guide',body='**Visa & Entry**\nD-2 student visa costs €600 (≈60,000 won). Processing takes 4–8 weeks.',content={'visa':{'type':'D-2 student visa','cost':'€600 (≈60,000 won)','duration':'4–8 weeks','required_docs':['Passport']},'work':{'legal_limit':'20 hours/week','part_time_allowed':True},'costs':{'monthly_total':'€1200 (1,200,000 KRW)','currency':'KRW'},'misc':{'empty':None,'list':[]}})
 def test_visa_and_work_facts_are_preserved(self):
  row=self.row();new,_=clean_row(row)
  self.assertEqual('€600',new['content']['visa']['cost'])
  self.assertIn('D-2 student visa',new['body'])
  self.assertIn('Processing takes 4–8 weeks.',new['body'])
  self.assertEqual(row['content']['work'],new['content']['work'])
  self.assertEqual(row['content']['misc'],new['content']['misc'])
  self.assertEqual('EUR',new['content']['costs']['currency'])
  self.assertEqual(new,clean_row(new)[0])
 def test_explicit_fee_clause_preserves_visa_and_timing(self):
  row=self.row();row['body']='Apply for a D-2 visa, costing 60,000 won, at least 4 weeks early.'
  new,issues=clean_row(row)
  self.assertEqual('Apply for a D-2 visa, at least 4 weeks early.',new['body'])
 def test_unknown_country(self):
  row=self.row();row['country_code']=None
  self.assertEqual(row,clean_row(row)[0])
  self.assertEqual('unknown_country',clean_row(row)[1][0]['reason'])
 def test_sql_atomic_backup_and_guarded_rollback(self):
  with tempfile.TemporaryDirectory() as temp:
   directory=Path(temp);generate([self.row()],directory,'test_run')
   review=(directory/'review.sql').read_text();apply=(directory/'apply.sql').read_text();rollback=(directory/'rollback.sql').read_text()
   self.assertTrue(review.endswith('ROLLBACK;\n'))
   self.assertTrue(apply.endswith('COMMIT;\n'))
   self.assertIn('Snapshot changed',apply)
   self.assertIn('Post-cleanup edits found',rollback)
   self.assertIn('CREATE TABLE report_currency_backup_test_run',apply)
 def test_run_id_rejects_sql_injection(self):
  with tempfile.TemporaryDirectory() as temp:
   with self.assertRaises(ValueError):generate([self.row()],Path(temp),'bad;DROP TABLE x')

if __name__=='__main__':unittest.main()
