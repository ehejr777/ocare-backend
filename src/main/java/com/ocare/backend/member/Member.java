package com.ocare.backend.member;

import com.ocare.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔티티.
 * 요구사항: 이름, 닉네임, 이메일, 패스워드 로 회원가입 / 이메일+패스워드로 로그인.
 */
@Getter
@Entity
@Table(name = "member", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_member_nickname", columnNames = "nickname")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /** BCrypt 로 암호화된 비밀번호 해시 (평문 저장 금지) */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    public Member(String name, String nickname, String email, String encodedPassword) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.password = encodedPassword;
    }
}
