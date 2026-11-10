"""Rebuild the guide's PNG diagrams and 12-page PDF with real UI captures (Python 3, Pillow, ReportLab).

Run from any directory: python docs/guides/generate_calendar_guide.py
Set OVLO_GUIDE_FONT_REGULAR / OVLO_GUIDE_FONT_BOLD to Korean TrueType fonts.
Windows defaults: C:/Windows/Fonts/malgun.ttf and malgunbd.ttf.
Outputs: sibling assets/calendar/*.png, sibling ovlo-calendar-guide-ko.pdf,
         project output/pdf/ovlo-calendar-guide-ko.pdf.
Diagrams are explanatory artwork. Existing screenshots are native production captures
with synthetic guide-only data; this script neither captures nor edits the UI images.
"""
from pathlib import Path
import os
import shutil
from PIL import Image, ImageDraw, ImageFont
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import Paragraph
from reportlab.lib.colors import HexColor
from reportlab.lib.enums import TA_LEFT

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
ASSETS = HERE / 'assets' / 'calendar'
OUT = ROOT / 'output' / 'pdf'
REG = Path(os.environ.get('OVLO_GUIDE_FONT_REGULAR', 'C:/Windows/Fonts/malgun.ttf'))
BOLD = Path(os.environ.get('OVLO_GUIDE_FONT_BOLD', 'C:/Windows/Fonts/malgunbd.ttf'))
assert REG.is_file() and BOLD.is_file(), 'Set the Korean TrueType font environment variables.'
for directory in [ASSETS, OUT]: directory.mkdir(parents=True, exist_ok=True)
BG='#F6F3EC'; INK='#173A3A'; TEAL='#09675F'; MUTED='#556C70'; GREEN='#E0F0E9'; BLUE='#E4EDF6'; CORAL='#FAE6DD'; WHITE='#FFFFFF'; LINE='#CBD7D1'

def font(size, bold=False): return ImageFont.truetype(str(BOLD if bold else REG),size)
def wrapped(draw,text,f,width):
    result=[]
    for original in text.split('\n'):
        line=''
        for char in original:
            if line and draw.textlength(line+char,font=f)>width:
                result.append(line.rstrip());line=char.lstrip()
            else: line+=char
        result.append(line)
    return result

def label(draw,text,xy,size=34,bold=False,color=INK,width=None,leading=None):
    f=font(size,bold); x,y=xy; step=leading or int(size*1.5)
    lines=wrapped(draw,text,f,width) if width else text.split('\n')
    for line in lines: draw.text((x,y),line,font=f,fill=color);y+=step
    return y

def box(d,xy,fill=WHITE,r=24):d.rounded_rectangle(xy,radius=r,fill=fill,outline=LINE,width=2)
def arrow(d,start,end,color=TEAL):
    d.line([start,end],fill=color,width=7)
    x,y=end
    if end[0]==start[0]:d.polygon([(x,y),(x-12,y-20),(x+12,y-20)],fill=color)
    else:d.polygon([(x,y),(x-20,y-12),(x-20,y+12)],fill=color)

def base(title,subtitle,height=1000):
    im=Image.new('RGB',(1600,height),BG);d=ImageDraw.Draw(im)
    label(d,'OVLO / 쉬운 사용 안내',(80,35),23,True,TEAL)
    label(d,title,(80,82),57,True)
    label(d,subtitle,(80,165),29,color=MUTED)
    return im,d

im,d=base('무엇이 달라졌나요?','학교 공지와 내 일정을 한곳에서, 역할에 맞게 이용해요.')
roles=[('학생 · MEMBER','학교를 구독하고\n내 일정을 추가해요.',GREEN),('학교 담당자 · UNIV','승인받은 학교의\n일정을 게시해요.',BLUE),('운영진 · ADMIN','담당자를 승인하고\n공식 공지를 대신 등록해요.',CORAL)]
for i,(title,body,fill) in enumerate(roles):
    x=80+i*490;box(d,(x,245,x+460,495),fill)
    label(d,title,(x+26,275),35,True,width=408)
    label(d,body,(x+26,365),31,width=408)
changes=[('01  학교·개인 캘린더','학교 일정과 나만의 계획을 함께 확인'),('02  학교 담당자 가입','신청 후 운영진 승인과 학교 배정'),('03  공식 출처 대리 등록','학교 공지의 원문 링크와 확인일 표시'),('04  좋아요 동시성 수정','같은 반응의 숫자가 중복 변경되지 않게')]
for i,(title,body) in enumerate(changes):
    x=80+(i%2)*740;y=545+(i//2)*185;box(d,(x,y,x+710,y+155))
    label(d,title,(x+26,y+18),34,True,width=660)
    label(d,body,(x+26,y+80),28,width=660)
label(d,'개인 일정은 본인만 볼 수 있어요. 학교 담당자와 운영진에게도 공개되지 않아요.',(80,940),26,color=TEAL)
im.save(ASSETS/'changes-overview.png')

im,d=base('학교 가입부터 학생 캘린더까지','학교가 직접 등록하거나, 운영진이 공식 공지를 확인해 대신 등록해요.',1100)
steps=[('01  담당자','학교 가입·신청','학교·부서·공식 연락처',GREEN),('02  운영진','승인·학교 배정','공식 소속·권한 확인',CORAL),('03  담당자','일정 저장','초안 상태에서는\n학생에게 비공개',BLUE),('04  담당자','게시','구독 학생에게 표시',BLUE)]
for i,(who,title,body,fill) in enumerate(steps):
    x=70+i*385;box(d,(x,245,x+300,475),fill)
    label(d,who,(x+22,265),27,True,TEAL)
    label(d,title,(x+22,320),34,True,width=256)
    label(d,body,(x+22,389),26,width=256,leading=39)
    if i<3:arrow(d,(x+310,360),(x+370,360))
box(d,(70,590,760,860),CORAL)
label(d,'학교 담당자가 없어도 가능',(100,615),28,True,TEAL)
label(d,'운영진의 공식 출처 대리 등록',(100,668),39,True,width=625)
label(d,'공식 공지 확인 → 출처·확인일 입력\n저장 → 게시',(100,744),30,width=625)
box(d,(920,590,1530,860),GREEN)
label(d,'05  학생',(950,615),28,True,TEAL)
label(d,'학교 구독 후 일정 확인',(950,668),39,True,width=550)
label(d,'내 캘린더에서 학교 일정 보기\n+ 개인 일정으로 나만의 계획 추가',(950,744),30,width=550)
arrow(d,(1375,487),(1375,575));arrow(d,(775,727),(902,727))
box(d,(70,930,1530,1035),WHITE)
label(d,'꼭 기억하기: 학교 일정은 “게시”, 학생은 “학교 구독”이 필요해요.',(105,957),34,True,width=1380)
im.save(ASSETS/'usage-flow.png')

im,d=base('좋아요 숫자가 틀어지지 않도록','두 요청이 동시에 도착해도 실제로 바뀐 반응만 숫자에 반영해요.')
box(d,(80,245,760,830),CORAL);box(d,(840,245,1520,830),GREEN)
label(d,'수정 전',(120,280),40,True);label(d,'수정 후',(880,280),40,True,TEAL)
label(d,'좋아요 1개에서 싫어요로 전환',(120,365),31,True,width=600)
label(d,'같은 사람이 보낸 두 전환 요청이\n이전 상태를 함께 읽음\n\n두 요청 모두 숫자를 바꾸면\n좋아요가 -1이 될 수 있었어요.',(120,440),31,width=600)
label(d,'좋아요 1개에서 싫어요로 전환',(880,365),31,True,width=600)
label(d,'실제로 반응을 바꾼 요청만\n숫자를 한 번 바꿈\n\n중복·경합 요청은 충돌 처리\n좋아요 0 / 싫어요 1로 맞춰요.',(880,440),31,width=600)
label(d,'검증: 실제 PostgreSQL에서 중복·전환·취소 경합을 재현하고 CI에 검사 추가',(80,885),29,True,TEAL,width=1440)
label(d,'일반 테스트 600개 + 통합 테스트 91개 통과 (수정 당시 검증 결과)',(80,944),28,color=MUTED)
im.save(ASSETS/'reaction-fix.png')

pdfmetrics.registerFont(TTFont('Guide',str(REG)))
pdfmetrics.registerFont(TTFont('GuideBold',str(BOLD)))
pdfmetrics.registerFontFamily('Guide',normal='Guide',bold='GuideBold')
W,H=595.2756,841.8898
pdf=OUT/'ovlo-calendar-guide-ko.pdf'
c=canvas.Canvas(str(pdf),pagesize=(W,H))
c.setTitle('Ovlo 학교·개인 캘린더 쉬운 사용 가이드')
c.setAuthor('Ovlo')
c.setSubject('학교 가입·승인, 일정 게시, 학생 구독, 운영진 대리 등록과 좋아요 수정 안내')

def rect(x,y,w,h,fill=WHITE):
    c.setFillColor(HexColor(fill));c.setStrokeColor(HexColor(LINE));c.setLineWidth(.6)
    c.roundRect(x,H-y-h,w,h,10,fill=1,stroke=1)
def para(text,x,y,w,size=11.5,bold=False,color=INK,maxh=None,leading=None):
    style=ParagraphStyle('body',fontName='GuideBold' if bold else 'Guide',fontSize=size,leading=leading or size*1.65,textColor=HexColor(color),wordWrap='CJK',alignment=TA_LEFT)
    p=Paragraph(text,style);_,h=p.wrap(w,1000)
    if maxh is not None:assert h<=maxh,(text[:60],h,maxh)
    assert y+h<790,(text[:60],y,h)
    p.drawOn(c,x,H-y-h);return y+h

def begin(n,title,subtitle):
    c.setFillColor(HexColor(BG));c.rect(0,0,W,H,fill=1,stroke=0)
    para('OVLO  /  처음 사용하는 사람을 위한 안내',42,30,460,9,True,TEAL)
    para(f'{n:02d} / 12',510,30,55,9,True,MUTED)
    para(title,42,66,510,25,True,maxh=90,leading=35)
    para(subtitle,42,145,510,11,color=MUTED,maxh=40)

def end(n):
    c.setStrokeColor(HexColor(LINE));c.line(42,48,W-42,48)
    c.setFont('Guide',8);c.setFillColor(HexColor(MUTED))
    c.drawString(42,32,'2026-11-10 | 도식은 설명용 · 실제 화면은 안내용 테스트 데이터로 촬영')
    c.drawRightString(W-42,32,str(n));c.showPage()

def art(name,y,h):c.drawImage(str(ASSETS/name),42,H-y-h,width=511,height=h,mask='auto')
def panel(title,body,x,y,w,h,fill=WHITE,size=11.5):
    rect(x,y,w,h,fill);para(title,x+18,y+16,w-36,14,True,maxh=45)
    para(body,x+18,y+(48 if h<125 else 58),w-36,size,maxh=h-(60 if h<125 else 73))
def link(label,url,x,y,w=500):
    para(f'<link href="{url}" color="{TEAL}"><u>{label}</u></link>',x,y,w,10.5)

begin(1,'학교 일정과 내 계획,<br/>이제 한곳에서','학교 소식을 모아 보는 달력과 나만의 일정장을 함께 쓴다고 생각하면 쉬워요.')
art('changes-overview.png',198,319.4)
panel('먼저 이것만 기억하세요','학교 담당자는 <b>승인을 받은 뒤</b> 학교 일정을 올립니다.<br/>학생은 <b>학교를 구독</b>해야 게시된 일정을 볼 수 있습니다.<br/>개인 일정은 <b>본인만</b> 볼 수 있습니다.',42,545,511,165,GREEN)
link('시작하기: Ovlo 회원가입','https://ovlo.mungji.com/register',42,735)
end(1)

begin(2,'어떤 순서로 쓰면 되나요?','학교 담당자가 직접 등록하는 길과 운영진이 대신 등록하는 길이 있어요.')
art('usage-flow.png',198,351.4)
panel('학생이 먼저 구독해 두어도 괜찮아요','학교 일정이 <b>게시</b>되고 학생이 그 학교를 <b>구독</b>한 상태라면 다음 캘린더 조회에 표시됩니다. 프로필에 학교를 입력하는 것과 구독은 별개예요.',42,575,511,135,WHITE)
link('학교 담당자 신청·상태 확인','https://ovlo.mungji.com/university-account',42,735)
end(2)


def shot(name,y,maxh=500,w=511,x=42):
    path=ASSETS/'screenshots'/name
    iw,ih=Image.open(path).size
    scale=min(w/iw,maxh/ih)
    dw,dh=iw*scale,ih*scale
    c.drawImage(str(path),x+(w-dw)/2,H-y-dh,width=dw,height=dh,mask='auto')
    return y+dh
def caption(text,y):
    para(text,42,y,511,9,color=MUTED,maxh=34)

begin(3,'가입할 때, 학교 담당자를 선택해요','학교 담당자 계정도 같은 회원가입 화면에서 시작합니다.')
shot('01-signup-choice.png',195,maxh=330)
caption('실제 회원가입 화면. 학교 담당자 가입 버튼을 누르세요.',535)
panel('학교 담당자 가입으로 들어가기','<b>1.</b> 아래 회원가입 링크를 열어요.<br/><b>2.</b> <b>학교 담당자 가입</b>을 선택해요.<br/><b>3.</b> 다음 쪽의 신청서를 작성해요. 이미 계정이 있다면 로그인 후 담당자 신청 화면을 이용하세요.',42,575,511,150,WHITE)
link('회원가입 화면 열기','https://ovlo.mungji.com/register',42,747)
end(3)

begin(4,'담당자 정보와 학교를 입력해요','신청서를 제출하면 검토 대기 상태가 됩니다. 승인 전에도 일반 회원 기능은 사용할 수 있어요.')
shot('02-staff-registration-form.png',195,maxh=465)
caption('실제 신청서의 입력 영역. 비밀번호를 입력하기 전에 촬영했습니다.',670)
para('<b>학교 검색 → 검색 결과 선택 → 부서·담당 업무 입력 → 계정 만들고 담당자 신청</b> 순서예요. 실제 신청에는 학교 업무 이메일과 확인 가능한 공식 안내를 적으세요. 화면의 학교·이메일은 촬영용 예시입니다.',42,703,511,11,maxh=75)
end(4)

begin(5,'운영진이 확인하고 승인해요','공식 직원 안내나 연락처로 소속과 업무 권한을 확인한 뒤 승인합니다.')
para('<b>운영진:</b> 관리자 로그인 → 학교 일정 · 담당자 → PENDING 조회 → 확인 근거 / 사유 입력 → <b>승인·학교 배정</b>',42,195,511,11.5,maxh=65)
shot('03-admin-approval.png',270,maxh=100)
caption('운영진 화면의 촬영용 신청 1건. 다른 신청자의 정보는 포함하지 않았습니다.',370)
para('<b>학교 담당자:</b> 내 신청 내역에서 <b>새로고침</b>을 누른 뒤 아래처럼 승인됨 상태인지 확인해요.',42,419,511,11.5,maxh=50)
shot('04-approval-result.png',485,maxh=165)
caption('승인된 신청과 학교 일정 관리하기 링크가 표시됩니다.',662)
para('<b>학교 일정 관리하기</b>로 이동하세요. 역할을 UNIV로만 바꾸면 관리 학교가 생기지 않아요. 승인 시 학교 배정까지 함께 처리해야 합니다.',42,710,511,11,maxh=60)
end(5)

begin(6,'학교 일정을 작성해요','관리 학교를 고른 뒤 + 학교 일정으로 새 일정을 만듭니다.')
shot('05-event-main-fields.png',195,maxh=250)
caption('실제 일정 편집 화면의 주요 입력 항목. 같은 편집 양식으로 새 일정을 작성합니다.',450)
panel('제목과 기간부터 입력하세요','<b>1.</b> 제목과 분류, 종일 / 시간 지정 여부를 선택해요.<br/><b>2.</b> 기준 시간대와 시작일·마지막 날을 확인해요.<br/><b>3.</b> 아래쪽의 장소·대상·설명·신청 링크도 필요하면 입력해요.<br/><b>4.</b> 입력을 마치면 맨 아래 <b>저장</b>을 눌러요.',42,495,511,185,BLUE)
para('하루 일정은 시작일과 마지막 날을 같은 날짜로 입력해요. 11월 20~22일 일정이라면 <b>마지막 날 (포함)</b>은 22일입니다. 실제 공지가 아닌 입력 예시예요.',42,708,511,11,maxh=60)
end(6)

begin(7,'담당자는 게시, 학생은 구독!','저장된 학교 일정은 초안이에요. 게시해야 구독한 학생에게 보입니다.')
shot('06-draft-and-publish.png',195,maxh=235)
caption('담당자 화면: 일정 목록에서 초안을 선택한 뒤 오른쪽 아래 게시를 누릅니다.',440)
para('<b>학생:</b> 캘린더 → 학교 구독 → 학교 검색·선택 → <b>이 학교 구독</b>',42,490,511,11.5,maxh=45)
shot('07-school-subscription.png',540,maxh=160)
caption('학생 화면: 선택한 학교를 구독하는 영역입니다.',712)
para('프로필에 학교를 입력하는 것과 캘린더 구독은 별개예요.',42,750,511,11,maxh=30)
end(7)

begin(8,'학생에게 이렇게 보여요','학교 일정, 운영진의 대리 등록 일정, 나만의 개인 일정을 함께 볼 수 있어요.')
shot('13-student-calendar-list.png',195,maxh=430)
caption('실제 학생 캘린더의 목록 보기. 일정을 누르면 상세 정보가 열립니다.',638)
panel('일정이 안 보이면 여기부터','<b>게시 여부 → 학교 구독 → 조회 월·선택 날짜·필터</b>를 확인하세요. 달력 보기와 목록 보기를 바꿀 수 있고, 모바일은 목록 중심으로 확인합니다.',42,665,511,113,GREEN,10.5)
end(8)

begin(9,'개인 일정은 나만 볼 수 있어요','+ 개인 일정에서 입력하고 저장하면 바로 내 캘린더에 표시됩니다.')
shot('14-personal-event-editor.png',195,maxh=420)
caption('실제 개인 일정 입력 화면. 이 입력 예시는 추가 저장하지 않았습니다.',627)
panel('학교 일정과 구분해서 기억하세요','개인 일정은 따로 게시할 필요가 없어요. 목록에서 선택한 뒤 <b>수정·삭제</b>로 관리합니다. <b>개인 · 나만 보기</b>로 표시되며 학교 담당자와 운영진에게도 공개되지 않아요.',42,665,511,113,GREEN,10.5)
end(9)

begin(10,'운영진이 공식 공지를 대신 등록해요','학교 담당자가 없어도 가능해요. 학교를 선택하고 원문을 확인한 뒤 작성합니다.')
shot('10-admin-curated-editor.png',190,maxh=490)
caption('실제 관리자 입력 화면. example.invalid 주소와 내용은 촬영용 예시입니다.',692)
para('<b>공식 출처 링크</b>에는 실제 학교 공지 주소를, <b>출처 확인일</b>에는 원문을 확인한 날짜를 넣어요. 둘 다 필수예요. <b>저장 → 게시</b> 후 학생 화면에서 확인하세요.',42,728,511,10.5,maxh=52)
end(10)

begin(11,'학생은 출처를 확인할 수 있어요','운영진이 대신 등록한 일정에는 등록 주체와 공식 출처가 표시됩니다.')
shot('11-curated-source-display.png',195,maxh=250,w=300,x=42)
shot('12-student-mobile.png',195,maxh=420,w=180,x=373)
para('왼쪽: 실제 대리 등록 일정의 상세.<br/>오른쪽: 실제 모바일 캘린더.',42,450,300,9,color=MUTED,maxh=40)
para('<b>Ovlo 등록 · 학교 공식 공지 참고</b> 표시로 학교 담당자의 직접 등록과 구분해요.<br/><br/><b>학교 공식 출처 ↗</b>에서 원문을 확인하세요. 이 가이드의 주소는 실제 공지가 아닌 예시입니다.',42,510,300,11,maxh=135)
panel('출처 링크가 자동 갱신을 뜻하지는 않아요','새 공지가 나오면 운영진이 원문을 다시 확인하고 내용·확인일을 수정해야 해요. 참가 신청이 필요한 일정은 상세의 신청 링크에서 따로 신청하세요.',42,665,511,113,WHITE,10.5)
end(11)

begin(12,'좋아요도 한 번만 정확하게','같은 요청이 동시에 들어오는 상황을 고쳐 숫자가 틀어지지 않도록 했어요.')
art('reaction-fix.png',195,319.4)
para('같은 표를 두 번 세지 않도록 고친 거예요. 실제로 반응을 바꾼 요청만 숫자를 조정하고, 중복 요청은 충돌로 처리해요. 이 상황을 확인하는 자동 검사도 추가했어요.',42,535,511,12,maxh=85)
panel('현재 제공하는 범위','학교·개인 일정, 담당자 승인, 출처 기반 대리 등록을 이용할 수 있어요. 푸시 알림·반복 일정·외부 달력 연동·자동 공지 수집은 아직 제공하지 않아요.',42,635,511,110,WHITE,11)
link('내 캘린더 열기','https://ovlo.mungji.com/calendar',42,758,170)
link('기능 PR #66 / 검증 근거','https://github.com/yeonjae1220/Ovlo/pull/66',220,758,190)
link('수정 PR #67','https://github.com/yeonjae1220/Ovlo/pull/67',435,758,120)
end(12)
c.save()
shutil.copy2(pdf,HERE/pdf.name)
print('Created',pdf)
print('Created 3 PNG diagrams and docs PDF copy')
