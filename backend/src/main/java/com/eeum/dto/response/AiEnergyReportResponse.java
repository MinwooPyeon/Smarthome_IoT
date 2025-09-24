package com.eeum.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "AI 에너지 요약 리포트 응답")
public class AiEnergyReportResponse {

    @Schema(description = "한 줄 요약")
    private String summary;

    @Schema(description = "핵심 하이라이트 3~6개")
    private List<String> highlights;

    @Schema(description = "데이터 기반 인사이트 3~6개")
    private List<String> insights;

    @Schema(description = "행동 가능한 제안 3~6개")
    private List<String> suggestions;

    @Schema(description = "부가 통계 (예: 타입별 kWh, 기간합계, 피크시간 등)")
    private Map<String, Object> stats;

    @Schema(description = "리포트 생성 시각(UTC)")
    private Instant generatedAt;
}
