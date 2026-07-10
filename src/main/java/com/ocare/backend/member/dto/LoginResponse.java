package com.ocare.backend.member.dto;

public record LoginResponse(Long memberId, String nickname, String sessionToken, long expiresInSeconds) {
}
