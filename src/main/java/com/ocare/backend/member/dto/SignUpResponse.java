package com.ocare.backend.member.dto;

public record SignUpResponse(Long memberId, String name, String nickname, String email) {
}
