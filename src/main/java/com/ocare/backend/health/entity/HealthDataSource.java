package com.ocare.backend.health.entity;

import com.ocare.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 삼성헬스/애플건강 등에서 App to App 으로 전달되어 서버에 수신된
 * 건강 데이터 페이로드(파일/전송 단위) 메타 정보.
 * 예: INPUT_DATA1.json ~ INPUT_DATA4.json 각각이 1건의 HealthDataSource 가 된다.
 */
@Getter
@Entity
@Table(name = "health_data_source", indexes = {
        @Index(name = "idx_hds_recordkey", columnList = "recordkey")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthDataSource extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "health_data_source_id")
    private Long id;

    /** 사용자 구분 키 (삼성헬스/애플건강 발급 식별자) */
    @Column(name = "recordkey", nullable = false, length = 64)
    private String recordkey;

    /** 데이터 종류. 과제 Input Data 기준 "steps" 고정이나 확장을 고려해 컬럼화 */
    @Column(name = "type", length = 20)
    private String type;

    @Column(name = "memo", length = 255)
    private String memo;

    /** 헬스 플랫폼(단말)에서 마지막으로 동기화된 시각 */
    @Column(name = "external_last_update")
    private LocalDateTime externalLastUpdate;

    /** 회원과의 연결(선택). 회원가입 전/비연동 상태의 데이터 수집도 허용하기 위해 nullable */
    @Column(name = "member_id")
    private Long memberId;

    public HealthDataSource(String recordkey, String type, String memo,
                             LocalDateTime externalLastUpdate, Long memberId) {
        this.recordkey = recordkey;
        this.type = type;
        this.memo = memo;
        this.externalLastUpdate = externalLastUpdate;
        this.memberId = memberId;
    }
}
