import { Link } from 'react-router-dom'

const FEATURES = [
  {
    icon: '🎓',
    title: '교환 대학 정보',
    desc: '78개 협정 교환 대학의 상세 정보와 YouTube 영상 후기를 한 곳에서 확인하세요. 비용·비자·기숙사 정보까지 정리되어 있습니다.',
  },
  {
    icon: '✈️',
    title: '파견·인바운드 후기 구분',
    desc: '이 학교에서 나가는 교환인지, 이 학교로 오는 교환인지 방향별로 필터링된 실제 학생 후기를 확인할 수 있습니다.',
  },
  {
    icon: '💬',
    title: '커뮤니티 게시판',
    desc: '학교별·지역별 게시판에서 질문하고 경험을 나누세요. 주거·학업·비자 등 다양한 카테고리의 정보가 모여 있습니다.',
  },
  {
    icon: '🤝',
    title: '팔로우 & 채팅',
    desc: '같은 대학으로 파견 가는 친구를 찾고, 1:1 또는 그룹 채팅으로 실시간으로 소통하세요.',
  },
]

export default function LandingPage() {
  return (
    <div
      style={{
        minHeight: '100vh',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        color: '#e2e8f0',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* ── 헤더 ─────────────────────────────────────────── */}
      <header
        style={{
          padding: '18px 32px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #2d3748',
          position: 'sticky',
          top: 0,
          background: '#242424',
          zIndex: 10,
        }}
      >
        <span style={{ fontSize: 22, fontWeight: 800, color: '#a78bfa', letterSpacing: '-0.5px' }}>
          Ovlo
        </span>
        <nav style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Link
            to="/exchange-universities"
            style={{
              padding: '8px 16px',
              borderRadius: 8,
              color: '#94a3b8',
              fontSize: 14,
              textDecoration: 'none',
            }}
          >
            교환대학 검색
          </Link>
          <Link
            to="/login"
            style={{
              padding: '8px 16px',
              borderRadius: 8,
              color: '#94a3b8',
              fontSize: 14,
              textDecoration: 'none',
            }}
          >
            로그인
          </Link>
          <Link
            to="/register"
            style={{
              padding: '8px 20px',
              borderRadius: 8,
              background: '#7c3aed',
              color: '#fff',
              fontSize: 14,
              fontWeight: 600,
              textDecoration: 'none',
            }}
          >
            회원가입
          </Link>
        </nav>
      </header>

      {/* ── 히어로 ───────────────────────────────────────── */}
      <section
        style={{
          flex: 1,
          maxWidth: 820,
          margin: '0 auto',
          padding: '80px 32px 64px',
          textAlign: 'center',
        }}
      >
        <div
          style={{
            display: 'inline-block',
            padding: '5px 14px',
            borderRadius: 20,
            background: '#7c3aed22',
            color: '#a78bfa',
            fontSize: 13,
            fontWeight: 600,
            marginBottom: 28,
            border: '1px solid #7c3aed44',
          }}
        >
          교환학생 커뮤니티 플랫폼
        </div>

        <h1
          style={{
            fontSize: 'clamp(32px, 6vw, 52px)',
            fontWeight: 900,
            lineHeight: 1.18,
            margin: '0 0 22px',
            color: '#f1f5f9',
            letterSpacing: '-1px',
          }}
        >
          교환학생 생활,
          <br />
          <span style={{ color: '#a78bfa' }}>Ovlo</span>와 함께 시작하세요
        </h1>

        <p
          style={{
            fontSize: 17,
            color: '#94a3b8',
            lineHeight: 1.75,
            margin: '0 0 44px',
            maxWidth: 560,
            marginLeft: 'auto',
            marginRight: 'auto',
          }}
        >
          전 세계 교환 대학 정보와 실제 학생들의 생생한 후기를 확인하고,
          <br />
          같은 경험을 공유하는 커뮤니티에서 함께 소통하세요.
        </p>

        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link
            to="/exchange-universities"
            style={{
              padding: '14px 30px',
              borderRadius: 10,
              background: '#7c3aed',
              color: '#fff',
              fontSize: 15,
              fontWeight: 700,
              textDecoration: 'none',
              boxShadow: '0 4px 20px #7c3aed55',
            }}
          >
            교환 대학 검색 →
          </Link>
          <Link
            to="/register"
            style={{
              padding: '14px 30px',
              borderRadius: 10,
              border: '1.5px solid #374151',
              color: '#e2e8f0',
              fontSize: 15,
              fontWeight: 500,
              textDecoration: 'none',
            }}
          >
            지금 시작하기
          </Link>
        </div>

        {/* 스탯 */}
        <div
          style={{
            display: 'flex',
            gap: 32,
            justifyContent: 'center',
            marginTop: 56,
            flexWrap: 'wrap',
          }}
        >
          {[
            { num: '78', label: '교환 대학' },
            { num: '2,800+', label: '영상 리뷰' },
            { num: '5개 대륙', label: '파견 국가' },
          ].map((s) => (
            <div key={s.label} style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 26, fontWeight: 800, color: '#a78bfa' }}>{s.num}</div>
              <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>{s.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── 기능 소개 ─────────────────────────────────────── */}
      <section
        style={{
          maxWidth: 960,
          margin: '0 auto',
          padding: '0 32px 80px',
          width: '100%',
          boxSizing: 'border-box',
        }}
      >
        <h2
          style={{
            fontSize: 22,
            fontWeight: 700,
            color: '#f1f5f9',
            textAlign: 'center',
            marginBottom: 32,
          }}
        >
          Ovlo로 할 수 있는 것
        </h2>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))',
            gap: 16,
          }}
        >
          {FEATURES.map((f) => (
            <div
              key={f.title}
              style={{
                padding: '28px 22px',
                borderRadius: 12,
                border: '1px solid #2d3748',
                background: '#1e2836',
              }}
            >
              <div style={{ fontSize: 30, marginBottom: 14 }}>{f.icon}</div>
              <h3 style={{ fontSize: 15, fontWeight: 700, color: '#f1f5f9', margin: '0 0 8px' }}>
                {f.title}
              </h3>
              <p style={{ fontSize: 13, color: '#94a3b8', lineHeight: 1.65, margin: 0 }}>
                {f.desc}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA 배너 ──────────────────────────────────────── */}
      <section
        style={{
          background: '#1e1433',
          borderTop: '1px solid #2d3748',
          borderBottom: '1px solid #2d3748',
          padding: '48px 32px',
          textAlign: 'center',
        }}
      >
        <h2 style={{ fontSize: 24, fontWeight: 800, color: '#f1f5f9', margin: '0 0 12px' }}>
          지금 바로 시작하세요
        </h2>
        <p style={{ fontSize: 15, color: '#94a3b8', margin: '0 0 28px' }}>
          무료로 가입하고 교환학생 정보를 한눈에 확인하세요.
        </p>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link
            to="/register"
            style={{
              padding: '12px 28px',
              borderRadius: 9,
              background: '#7c3aed',
              color: '#fff',
              fontSize: 14,
              fontWeight: 700,
              textDecoration: 'none',
            }}
          >
            무료 회원가입
          </Link>
          <Link
            to="/login"
            style={{
              padding: '12px 28px',
              borderRadius: 9,
              border: '1.5px solid #374151',
              color: '#cbd5e1',
              fontSize: 14,
              textDecoration: 'none',
            }}
          >
            이미 계정이 있어요
          </Link>
        </div>
      </section>

      {/* ── 푸터 ─────────────────────────────────────────── */}
      <footer
        style={{
          borderTop: '1px solid #1e293b',
          padding: '22px 32px',
          textAlign: 'center',
          color: '#475569',
          fontSize: 13,
        }}
      >
        © 2026 Ovlo — 교환학생 커뮤니티 플랫폼
      </footer>
    </div>
  )
}
