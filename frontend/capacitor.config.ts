import type { CapacitorConfig } from '@capacitor/cli'

// Hybrid 방식: 정적 번들을 싣지 않고 배포된 SSR 사이트를 WebView가 직접 로드한다.
// Next.js가 output:'standalone'(SSR) + middleware CSP nonce + rewrites(/api → 백엔드)에
// 의존하므로 output:'export'로 정적화가 불가능하다. 원격 로드를 택하면 WebView의 origin이
// 실제 도메인이 되어 CORS·SameSite 쿠키·CSP·SockJS 상대경로('/ws')가 전부 무수정으로 동작한다.
const config: CapacitorConfig = {
  appId: 'com.mungji.ovlo',
  appName: 'Ovlo',
  // Hybrid에서는 실제로 서빙되지 않지만 Capacitor가 존재를 요구한다.
  webDir: 'capacitor-shell',
  server: {
    url: 'https://ovlo.mungji.com',
    cleartext: false,
  },
  ios: {
    // 원격 도메인을 로드하므로 App-Bound Domains 제한을 걸지 않는다.
    limitsNavigationsToAppBoundDomains: false,
  },
}

export default config
