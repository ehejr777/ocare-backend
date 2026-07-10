package com.ocare.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 는 아래 두 가지 용도로 사용한다.
 *  1) 로그인 세션 토큰 저장 (LoginSessionService) - TTL 기반 자동 만료
 *  2) Daily/Monthly 집계 조회 결과 캐시 (HealthSummaryQueryService) - Cache-Aside 패턴
 *
 * 값은 모두 문자열(JSON)로 직렬화하여 저장하므로 String 기반 Serializer 를 사용한다.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
