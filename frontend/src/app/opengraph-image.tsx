import { ImageResponse } from 'next/og'

/* eslint-disable react-refresh/only-export-components -- Next metadata image module requires named metadata exports. */

export const runtime = 'edge'
export const alt = 'Ovlo exchange student community'
export const size = { width: 1200, height: 630 }
export const contentType = 'image/png'

export default function OpenGraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: '100%',
          height: '100%',
          position: 'relative',
          display: 'flex',
          alignItems: 'center',
          padding: '76px 82px',
          overflow: 'hidden',
          background: '#f2efe6',
          color: '#172321',
          fontFamily: 'sans-serif',
        }}
      >
        <div style={{ position: 'absolute', width: 620, height: 620, border: '2px solid #b7d9cf', borderRadius: 310, right: -230, top: -260 }} />
        <div style={{ position: 'absolute', width: 450, height: 450, border: '2px solid #d8eee8', borderRadius: 225, right: -145, top: -175 }} />

        <div style={{ width: 790, display: 'flex', flexDirection: 'column' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 18, marginBottom: 48 }}>
            <div style={{ width: 64, height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 20, background: '#0a5f55' }}>
              <div style={{ width: 31, height: 31, border: '6px solid #fffdf8', borderRadius: 20 }} />
            </div>
            <span style={{ fontSize: 42, fontWeight: 800, letterSpacing: '-2px' }}>Ovlo</span>
          </div>
          <div style={{ color: '#0a5f55', fontSize: 22, fontWeight: 700, letterSpacing: '2px' }}>YOUR EXCHANGE, CLEARLY MAPPED</div>
          <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', fontSize: 70, lineHeight: 1.08, fontWeight: 800, letterSpacing: '-4px' }}>
            <span>교환학생 생활을</span>
            <span>먼저 살아본 사람들과</span>
          </div>
          <div style={{ marginTop: 28, color: '#53625e', fontSize: 25 }}>
            대학 정보 · 검증된 후기 · 커뮤니티 · 채팅
          </div>
        </div>

        <div style={{ position: 'absolute', right: 72, bottom: 62, display: 'flex', alignItems: 'center', gap: 14, color: '#0a5f55', fontSize: 22, fontWeight: 700 }}>
          <span>ICN</span><span style={{ letterSpacing: '5px' }}>•••••›</span><span>WORLD</span>
        </div>
      </div>
    ),
    size,
  )
}
