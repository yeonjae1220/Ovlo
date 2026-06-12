package me.yeonjae.ovlo.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 작업 활성화 (인증 자격 만료 등). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
