package com.eeum.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AiEnergyReportResponse {

    private String summary;                     // 한 줄 요약
    private List<HighlightItem> highlights;     // 카드형 하이라이트
    private List<String> insights;              // 데이터 인사이트
    private List<String> suggestions;           // 절감 제안

    private Comparison comparison;              // 이번달/지난달/전년동월 비교
    private Map<String, Object> stats;          // 기타 통계
    private Instant generatedAt;

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HighlightItem {
        private String label;   // 예: "총 사용량"
        private String value;   // 예: "320 kWh"
        private String trend;   // 예: "전월 대비 -8%" (없으면 null)
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Comparison {
        private Period thisMonth;
        private Period lastMonth;
        private Period lastYearSameMonth;

        @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
        public static class Period {
            private double usage;   // kWh
            private long   bill;    // KRW (추정치)
        }
    }
}

