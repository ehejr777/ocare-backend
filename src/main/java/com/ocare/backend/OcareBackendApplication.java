package com.ocare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * OCare Backend Developer 채용과제 진입점.
 *
 * 삼성헬스/애플건강 등 외부 헬스 플랫폼에서 App to App 으로 전달된
 * 걸음수/거리/칼로리 데이터를 수집(저장)하고, 회원가입/로그인 및
 * Daily/Monthly 단위 조회 기능을 제공하는 백엔드 서비스입니다.
 */
@EnableJpaAuditing
@SpringBootApplication
public class OcareBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcareBackendApplication.class, args);
    }
}
