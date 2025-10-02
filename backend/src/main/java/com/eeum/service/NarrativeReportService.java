package com.eeum.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.response.AiEnergyReportResponse;
import com.eeum.dto.response.AiEnergyReportResponse.HighlightItem;
import com.eeum.dto.response.AiEnergyReportResponse.Comparison;
import com.eeum.dto.response.AiEnergyReportResponse.Comparison.Period;
import com.eeum.service.ai.AiChatClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NarrativeReportService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final EnergyService energyService;
    private final AiChatClient aiChatClient;

    @Transactional(readOnly = true)
    public AiEnergyReportResponse generate(Integer userId, Integer homeId) {

        // ===== 기간 정의 =====
        LocalDate today = LocalDate.now(KST);

        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd   = thisMonthStart.minusDays(1);
        LocalDate lastYearSameStart = thisMonthStart.minusYears(1);
        LocalDate lastYearSameEnd   = lastMonthEnd.minusYears(1).withDayOfMonth(
                Math.min(lastMonthEnd.getDayOfMonth(), lastYearSameStart.lengthOfMonth())
        );

        // 비교용: 이번달/지난달/전년동월 사용량(kWh) — 타입별 합계의 총합
        double thisMonthKwh      = sumMap(energyService.getUsageByTypeKwh(userId, homeId, thisMonthStart, today));
        double lastMonthKwh      = sumMap(energyService.getUsageByTypeKwh(userId, homeId, lastMonthStart, lastMonthEnd));
        double lastYearSameKwh   = sumMap(energyService.getUsageByTypeKwh(userId, homeId, lastYearSameStart, lastYearSameEnd));

        // 추정요금(간단 고정단가)
        long thisMonthBill    = estimateBillKrw(thisMonthKwh);
        long lastMonthBill    = estimateBillKrw(lastMonthKwh);
        long lastYearSameBill = estimateBillKrw(lastYearSameKwh);

        // 최근 90일/30일 분석(인사이트용 데이터 제공)
        LocalDate start90 = today.minusDays(89);
        LocalDate start30 = today.minusDays(29);

        Map<String, Double> kwhByType90d = safeMap(energyService.getUsageByTypeKwh(userId, homeId, start90, today));
        Map<String, Double> kwhByType30d = safeMap(energyService.getUsageByTypeKwh(userId, homeId, start30, today));

        double total90d = sumMap(kwhByType90d);
        double total30d = sumMap(kwhByType30d);

        // 피크 시간/요일 (90일 기준)
        OptionalInt peakHourOpt    = energyService.getPeakHour(userId, homeId, start90, today);     // 0~23
        OptionalInt peakWeekdayOpt = energyService.getPeakWeekday(userId, homeId, start90, today);  // 1~7 (월~일)

        // 이번달 타입별 비중 문자열 (상위 3개 + 기타)
        String shareTextThisMonth = buildShareText(
                safeMap(energyService.getUsageByTypeKwh(userId, homeId, thisMonthStart, today)), 3);

        // ===== 하이라이트 카드 구성 =====
        String trendVsLastMonth = formatTrend(thisMonthKwh, lastMonthKwh);          // "전월 대비 -8%"
        List<HighlightItem> highlights = new ArrayList<>();
        highlights.add(HighlightItem.builder().label("총 사용량")
                .value(formatKwh(thisMonthKwh)).trend(trendVsLastMonth).build());
        peakHourOpt.ifPresent(h ->
                highlights.add(HighlightItem.builder().label("최대 사용 시간대")
                        .value(h + "시대").build()));
        peakWeekdayOpt.ifPresent(wd ->
                highlights.add(HighlightItem.builder().label("최대 사용 요일")
                        .value(weekdayKorean(wd)).build()));
        highlights.add(HighlightItem.builder().label("가전별 비중")
                .value(shareTextThisMonth).build());
        highlights.add(HighlightItem.builder().label("예상 전기요금")
                .value(formatBill(thisMonthBill)).build());

        // ===== AI 요약/인사이트/제안 =====
        String system = """
                당신은 주택 에너지 사용 분석 전문가입니다.
                - 과장 없이 데이터 기반으로, 높임말을 사용하세요.
                - 반드시 아래 4개 섹션 제목을 그대로 사용하세요:
                  1) 한 줄 요약
                  2) 핵심 하이라이트
                  3) 데이터 인사이트
                  4) 절감 제안
                - 각 섹션은 불릿('- ') 위주로 간결히, 특수 기호나 마크다운 없이 작성하세요.
                - '핵심 하이라이트'는 3~6개, '데이터 인사이트'와 '절감 제안'은 각 3~6개.
                """;

        String user = buildUserPromptForAI(today, thisMonthStart, today, lastMonthStart, lastMonthEnd,
                lastYearSameStart, lastYearSameEnd,
                thisMonthKwh, lastMonthKwh, lastYearSameKwh,
                start90, start30, total90d, total30d,
                kwhByType90d, kwhByType30d, peakHourOpt, peakWeekdayOpt, shareTextThisMonth);

        String narrative = aiChatClient.chat(system, user);
        Map<String, List<String>> aiSections = splitToSections(narrative);

        // ===== 비교 섹션 =====
        Comparison comparison = Comparison.builder()
                .thisMonth(Period.builder().usage(round1(thisMonthKwh)).bill(thisMonthBill).build())
                .lastMonth(Period.builder().usage(round1(lastMonthKwh)).bill(lastMonthBill).build())
                .lastYearSameMonth(Period.builder().usage(round1(lastYearSameKwh)).bill(lastYearSameBill).build())
                .build();

        // ===== 부가 통계(stats) =====
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("period_90d", Map.of(
                "start", start90.toString(),
                "end", today.toString(),
                "totalKwh", round1(total90d)
        ));
        stats.put("period_30d", Map.of(
                "start", start30.toString(),
                "end", today.toString(),
                "totalKwh", round1(total30d)
        ));
        peakHourOpt.ifPresent(h -> stats.put("peakHour", h));
        peakWeekdayOpt.ifPresent(wd -> stats.put("peakWeekday", wd)); // 1=월..7=일

        // 상위 타입 Top3 (90일)
        stats.put("topTypes90d", topNText(kwhByType90d, 3));

        return AiEnergyReportResponse.builder()
                .summary(firstOrDefault(aiSections.get("summary"),
                        "최근 에너지 사용 현황을 요약했습니다."))
                .highlights(highlights)
                .insights(aiSections.getOrDefault("insights", List.of()))
                .suggestions(aiSections.getOrDefault("suggestions", List.of()))
                .comparison(comparison)
                .stats(stats)
                .generatedAt(Instant.now())
                .build();
    }

    // ====== 내부 유틸 ======

    private static Map<String, Double> safeMap(Map<String, Double> m) {
        return (m == null) ? Map.of() : m;
    }

    private static double sumMap(Map<String, Double> m) {
        return safeMap(m).values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private static String formatKwh(double v) {
        return round1(v) + " kWh";
    }

    private static String formatBill(long billKrw) {
        return String.format("%,d원", billKrw);
    }

    private static String formatTrend(double cur, double prev) {
        if (prev <= 0.0) return "전월 대비 -";
        double diffPct = ((cur - prev) / prev) * 100.0;
        return "전월 대비 " + (diffPct >= 0 ? "+" : "") + round1(diffPct) + "%";
    }

    private static String weekdayKorean(int wd) {
        // 1=월 .. 7=일
        return switch (wd) {
            case 1 -> "월요일";
            case 2 -> "화요일";
            case 3 -> "수요일";
            case 4 -> "목요일";
            case 5 -> "금요일";
            case 6 -> "토요일";
            case 7 -> "일요일";
            default -> "-";
        };
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    // 매우 단순한 고정단가 추정: 1 kWh ≈ 180원
    private static long estimateBillKrw(double kwh) {
        return Math.round(kwh * 180.0);
    }

    private static String buildShareText(Map<String, Double> map, int topN) {
        if (map.isEmpty()) return "-";
        double total = sumMap(map);
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        List<String> parts = new ArrayList<>();
        double accTop = 0.0;
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            var e = sorted.get(i);
            double pct = total > 0 ? (e.getValue() / total * 100.0) : 0;
            parts.add(e.getKey() + " " + round1(pct) + "%");
            accTop += e.getValue();
        }
        if (sorted.size() > topN) {
            double othersPct = total > 0 ? ((total - accTop) / total * 100.0) : 0;
            parts.add("기타 " + round1(othersPct) + "%");
        }
        return String.join(", ", parts);
    }

    private static List<String> topNText(Map<String, Double> map, int n) {
        if (map.isEmpty()) return List.of();
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(n)
                .map(e -> e.getKey() + " " + round1(e.getValue()) + " kWh")
                .collect(Collectors.toList());
    }

    private static String buildUserPromptForAI(
            LocalDate today,
            LocalDate thisStart, LocalDate thisEnd,
            LocalDate prevStart, LocalDate prevEnd,
            LocalDate lastYearStart, LocalDate lastYearEnd,
            double thisKwh, double prevKwh, double lastYearKwh,
            LocalDate start90, LocalDate start30,
            double total90d, double total30d,
            Map<String, Double> kwhByType90d, Map<String, Double> kwhByType30d,
            OptionalInt peakHourOpt, OptionalInt peakWeekdayOpt,
            String shareTextThisMonth
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("분석 기준일(KST): ").append(today.format(DateTimeFormatter.ISO_DATE)).append("\n");
        sb.append("이번달 기간: ").append(thisStart).append(" ~ ").append(thisEnd).append(", 총합: ").append(round1(thisKwh)).append(" kWh\n");
        sb.append("지난달 기간: ").append(prevStart).append(" ~ ").append(prevEnd).append(", 총합: ").append(round1(prevKwh)).append(" kWh\n");
        sb.append("전년동월 기간: ").append(lastYearStart).append(" ~ ").append(lastYearEnd).append(", 총합: ").append(round1(lastYearKwh)).append(" kWh\n");
        sb.append("최근 90일 총합: ").append(round1(total90d)).append(" kWh, 타입별: ").append(kwhByType90d).append("\n");
        sb.append("최근 30일 총합: ").append(round1(total30d)).append(" kWh, 타입별: ").append(kwhByType30d).append("\n");
        peakHourOpt.ifPresent(h -> sb.append("피크 시간대(추정): ").append(h).append("시\n"));
        peakWeekdayOpt.ifPresent(wd -> sb.append("피크 요일(추정, 1=월..7=일): ").append(wd).append("\n"));
        sb.append("이번달 가전별 비중(요약): ").append(shareTextThisMonth).append("\n\n");

        sb.append("아래 형식을 정확히 지켜 작성하세요.\n")
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

    /** 간단 타이틀 파싱: 4개 섹션(요약/하이라이트/인사이트/제안)만 텍스트로 가져옴 */
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

            if (line.startsWith("한 줄 요약"))        { current = "summary";     continue; }
            if (line.startsWith("핵심 하이라이트"))  { current = "highlights";  continue; }
            if (line.startsWith("데이터 인사이트"))  { current = "insights";    continue; }
            if (line.startsWith("절감 제안"))        { current = "suggestions"; continue; }

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
