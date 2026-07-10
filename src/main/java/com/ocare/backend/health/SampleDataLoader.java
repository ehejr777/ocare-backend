package com.ocare.backend.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocare.backend.health.dto.HealthDataUploadRequest;
import com.ocare.backend.health.service.HealthDataIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * "데이터 저장/조회 샘플 코드" 제출 요건을 위한 데모용 러너.
 *
 * 실행 방법: -Dspring.profiles.active=sample 옵션으로 기동하면
 * sample-data/ 디렉터리의 INPUT_DATA1~4.json 을 실제 저장 로직(HealthDataIngestService)에
 * 태워 DB 에 적재한다. (동일 파일을 여러 번 실행해도 idempotent 하게 중복 저장되지 않는다.)
 */
@Slf4j
@Component
@Profile("sample")
@RequiredArgsConstructor
public class SampleDataLoader implements CommandLineRunner {

    private final HealthDataIngestService healthDataIngestService;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("file:sample-data/*.json");

        for (Resource resource : resources) {
            log.info("샘플 데이터 적재 시작: {}", resource.getFilename());
            HealthDataUploadRequest request = objectMapper.readValue(resource.getInputStream(), HealthDataUploadRequest.class);
            healthDataIngestService.ingest(request);
            log.info("샘플 데이터 적재 완료: recordkey={}, entries={}", request.recordkey(), request.data().entries().size());
        }
    }
}
