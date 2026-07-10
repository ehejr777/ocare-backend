package com.ocare.backend.member;

import com.ocare.backend.common.exception.BusinessException;
import com.ocare.backend.common.exception.ErrorCode;
import com.ocare.backend.member.dto.LoginRequest;
import com.ocare.backend.member.dto.LoginResponse;
import com.ocare.backend.member.dto.SignUpRequest;
import com.ocare.backend.member.dto.SignUpResponse;
import com.ocare.backend.security.LoginSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginSessionService loginSessionService;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = new Member(request.name(), request.nickname(), request.email(), encodedPassword);
        Member saved = memberRepository.save(member);

        return new SignUpResponse(saved.getId(), saved.getName(), saved.getNickname(), saved.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        String token = loginSessionService.issueToken(member.getId());
        return new LoginResponse(member.getId(), member.getNickname(), token, loginSessionService.getSessionTtlSeconds());
    }
}
