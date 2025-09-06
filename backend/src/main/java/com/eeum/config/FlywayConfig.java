package com.eeum.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return new FlywayMigrationStrategy() {
            @Override
            public void migrate(Flyway flyway) {
                // 1) 체크섬/히스토리 불일치 정정
                flyway.repair();

                // (선택) 비어있는 DB인데 11부터 시작하고 싶다면 properties로 baseline-on-migrate 설정해두면 됨

                // 2) 마이그레이션 실행
                flyway.migrate();
            }
        };
    }
}
