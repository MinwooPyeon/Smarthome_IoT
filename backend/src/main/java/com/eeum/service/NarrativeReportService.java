package com.eeum.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.response.AiEnergyReportResponse;
import com.eeum.service.ai.AiChatClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NarrativeReportService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final EnergyService energyService;   // 기존에 구현된 서비스 (타입별 집계 등 제공)
    private final AiChatClient aiChatClient;

    /**
     * 사용자-대표집 기반 전체 데이터를 분석해 AI 내러티브 리포트 생성
     */
    @Transactional(readOnly = true)
    public AiEnergyReportResponse generate(Integer userId, Integer homeId) {
        // 1) 분석 윈도우 정의 (예: 최근 90일 + 최근 30일 비교)
        LocalDate today = LocalDate.now(KST);
        LocalDate start90 = today.minusDays(89);
        LocalDate start30 = today.minusDays(29);

        // 2) 타입별 사용량 합계(kWh), 총합, 피크 시간대 등 조회 (EnergyService에서 호출)
        Map<String, Double> kwhByType90d = safeMap(energyService.getUsageByTypeKwh(userId, homeId, start90, today));
        Map<String, Double> kwhByType30d = safeMap(energyService.getUsageByTypeKwh(userId, homeId, start30, today));

        double total90d = kwhByType90d.values().stream().mapToDouble(Double::doubleValue).sum();
        double total30d = kwhByType30d.values().stream().mapToDouble(Double::doubleValue).sum();

        // 피크 시간/요일
        OptionalInt peakHourOpt = energyService.getPeakHour(userId, homeId, start90, today);   // 0~23
        OptionalInt peakWeekdayOpt = energyService.getPeakWeekday(userId, homeId, start90, today); // 1~7 (월~일)

        // 상위 소비 카테고리 Top-N
        List<Map.Entry<String, Double>> topTypes = new ArrayList<>(kwhByType90d.entrySet());
        topTypes.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        List<String> top3 = new ArrayList<>();
        for (int i = 0; i < Math.min(3, topTypes.size()); i++) {
            Map.Entry<String, Double> e = topTypes.get(i);
            top3.add(e.getKey() + " " + round1(e.getValue()) + " kWh");
        }

        // 3) AI 프롬프트 구성
        String system = """
        		당신은 주택 에너지 사용 분석 전문가입니다.

        		[작성 규칙]
        		- 실제 데이터 기반으로 과장 없이 작성하세요.
        		- 간결한 문장과 불릿을 사용하세요.
        		- 반드시 아래 5개 섹션으로 나누어 작성하세요:
        		  1) 한 줄 요약 (전체 상황을 1문장으로 요약)
        		  2) 핵심 하이라이트 (최근 사용 패턴, 3~6개)
        		  3) 데이터 인사이트 (최근 90일·30일 분석 + 전년도 동기간 비교 포함, 3~6개)
        		  4) 절감 제안 (실행 가능한 개선 방안, 3~6개)
        		  5) 전년도 비교 요약 (올해 vs 작년 동기간 차이, 증가·감소 요인 분석)
        		- 전문 용어는 괄호 안에 간단히 풀어 설명하세요.
        		- 출력은 마크다운이나 특수 기호 없이, 순수 텍스트로만 작성하세요.
        		- 높임말을 사용해줘.
        		""";

        String user = buildUserPrompt(today, start90, start30, total90d, total30d, kwhByType90d, kwhByType30d,
                                      peakHourOpt, peakWeekdayOpt, top3);

        // 4) AI 호출
        String narrative = aiChatClient.chat(system, user);

        // 5) 파싱
        Map<String, List<String>> sections = splitToSections(narrative);

        // 6) 응답 조립
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("period_90d", Map.of(
            "start", start90.toString(),
            "end", today.toString(),
            "totalKwh", round1(total90d),
            "topTypes", top3
        ));
        stats.put("period_30d", Map.of(
            "start", start30.toString(),
            "end", today.toString(),
            "totalKwh", round1(total30d)
        ));
        peakHourOpt.ifPresent(h -> stats.put("peakHour", h));
        peakWeekdayOpt.ifPresent(wd -> stats.put("peakWeekday", wd)); // 1=월 ... 7=일

        return AiEnergyReportResponse.builder()
            .summary(firstOrDefault(sections.get("summary"), "최근 에너지 사용 현황을 요약했습니다."))
            .highlights(sections.getOrDefault("highlights", List.of()))
            .insights(sections.getOrDefault("insights", List.of()))
            .suggestions(sections.getOrDefault("suggestions", List.of()))
            .stats(stats)
            .generatedAt(Instant.now())
            .build();
    }

    // ===== 헬퍼 =====

    private static Map<String, Double> safeMap(Map<String, Double> m) {
        return (m == null) ? Map.of() : m;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String buildUserPrompt(
            LocalDate today, LocalDate start90, LocalDate start30,
            double total90d, double total30d,
            Map<String, Double> kwhByType90d, Map<String, Double> kwhByType30d,
            OptionalInt peakHourOpt, OptionalInt peakWeekdayOpt, List<String> top3
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("분석 기준일(KST): ").append(today.format(DateTimeFormatter.ISO_DATE)).append("\n");
        sb.append("기간1: 최근 90일 (").append(start90).append(" ~ ").append(today).append(") 총합: ").append(round1(total90d)).append(" kWh\n");
        sb.append("기간2: 최근 30일 (").append(start30).append(" ~ ").append(today).append(") 총합: ").append(round1(total30d)).append(" kWh\n");
        sb.append("타입별 kWh (90일): ").append(kwhByType90d).append("\n");
        sb.append("타입별 kWh (30일): ").append(kwhByType30d).append("\n");
        peakHourOpt.ifPresent(h -> sb.append("피크 시간대(추정): ").append(h).append("시\n"));
        peakWeekdayOpt.ifPresent(wd -> sb.append("피크 요일(추정, 1=월..7=일): ").append(wd).append("\n"));
        if (!top3.isEmpty()) {
            sb.append("상위 소비 카테고리 Top3: ").append(top3).append("\n");
        }
        sb.append("\n아래 형식으로 작성하세요.\n")
          .append("한 줄 요약:\n")
          .append("- ...\n\n")
          .append("핵심 하이라이트:\n")
          .append("- ...\n- ...\n- ...\n\n")
          .append("데이터 인사이트:\n")
          .append("- ...\n- ...\n- ...\n\n")
          .append("절감 제안:\n")
          .append("- ...\n- ...\n- ...\n");
        return sb.toString();
    }

    private static Map<String, List<String>> splitToSections(String text) {
        Map<String, List<String>> out = new HashMap<>();
        out.put("summary", new ArrayList<>());
        out.put("highlights", new ArrayList<>());
        out.put("insights", new ArrayList<>());
        out.put("suggestions", new ArrayList<>());

        String[] lines = text.split("\\R");
        String current = null;
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isBlank()) continue;

            if (line.startsWith("한 줄 요약")) { current = "summary"; continue; }
            if (line.startsWith("핵심 하이라이트")) { current = "highlights"; continue; }
            if (line.startsWith("데이터 인사이트")) { current = "insights"; continue; }
            if (line.startsWith("절감 제안")) { current = "suggestions"; continue; }

            if (current == null) continue;
            if (line.startsWith("-")) line = line.substring(1).trim();

            if (current.equals("summary")) {
                if (out.get("summary").isEmpty()) out.get("summary").add(line);
            } else {
                out.get(current).add(line);
            }
        }
        return out;
    }

    private static String firstOrDefault(List<String> list, String def) {
        return (list == null || list.isEmpty()) ? def : list.get(0);
    }
}
