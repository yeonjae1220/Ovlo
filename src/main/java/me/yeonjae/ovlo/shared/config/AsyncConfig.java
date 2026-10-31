package me.yeonjae.ovlo.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 승인 뒤 공지 번역처럼 요청 응답과 분리해야 하는 작업용. 실행기는 Boot 자동 구성(가상 스레드)을 그대로 쓴다.
 * 비동기 메서드 안의 예외는 호출자에게 전파되지 않으므로 각 작업이 스스로 기록·알림해야 한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
