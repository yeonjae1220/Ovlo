package me.yeonjae.ovlo.shared.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Spring Boot 4.x에서 FlywayAutoConfiguration이 spring-boot-autoconfigure에서 제거됨.
 * 별도 spring-boot-flyway 모듈이 필요하지만 현재 의존성에 없으므로 수동 설정.
 * spring.flyway.enabled=true 일 때만 활성화됨 (prod 프로파일).
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }
}
