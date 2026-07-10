package com.ocare.backend.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 로그인 성공 시 Redis 에 세션 토큰을 발급/보관한다.
 * key   : "session:{token}"
 * value : memberId
 * TTL   : ocare.auth.session-ttl-seconds (기본 1시간) - 만료 시 자동 로그아웃 효과
 */
@Service
@RequiredArgsConstructor
public class LoginSessionService {

    private static final String SESSION_KEY_PREFIX = "session:";

    private final RedisTemplate<String, String> redisTemplate;

    @Getter
    @Value("${ocare.auth.session-ttl-seconds:3600}")
    private long sessionTtlSeconds;

    public String issueToken(Long memberId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                SESSION_KEY_PREFIX + token,
                String.valueOf(memberId),
                Duration.ofSeconds(sessionTtlSeconds)
        );
        return token;
    }

    public Optional<Long> resolveMemberId(String token) {
        String value = redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + token);
        return Optional.ofNullable(value).map(Long::valueOf);
    }

}
