package com.eeum.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.*;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.response.EnergySeriesResponse;
import com.eeum.dto.response.EnergySeriesResponse.Point;
import com.eeum.dto.response.EnergyTypeResponse;
import com.eeum.repository.IrEventLogRepository;
import com.eeum.repository.IrEventLogRepository.EnergyRow;
import com.eeum.repository.UserHomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnergyService {

    private final IrEventLogRepository irEventLogRepository;
    private final UserHomeRepository   userHomeRepository;

    private static final ZoneId DEFAULT_TZ = ZoneId.of("Asia/Seoul");

    // 기본 소비전력(W)
    private static final Map<String, Double> DEFAULT_WATTS = Map.of(
        "에어컨",      1500.0,
        "조명",        12.0,
        "선풍기",      45.0,
        "공기청정기",   40.0,
        "티비",     120.0
    );
    private static final double DEFAULT_WATTS_FALLBACK = 100.0;

    /** 기간별(시/일/월 단위) 사용량 시계열 응답 생성 */
    @Transactional(readOnly = true)
    public EnergySeriesResponse getUsageSeries(Integer userId, Integer homeId,
                                               String range, String dateIso) {
        Window w = resolveWindow(range, dateIso);
        checkAccess(userId, homeId);

        CalcResult calc = calculate(userId, homeId, w);

        List<Point> series = new ArrayList<>(w.labels.size());
        for (int i = 0; i < w.labels.size(); i++) {
            series.add(Point.builder()
                .label(w.labels.get(i))
                .kwh(round3(calc.buckets[i]))
                .build());
        }

        return EnergySeriesResponse.builder()
                .range(w.range)
                .from(w.from)
                .to(w.to)
                .totalKwh(round3(Arrays.stream(calc.buckets).sum()))
                .series(series)
                .build();
    }

    /** 기간 내 기기 타입별 사용량 합계/비율 응답 생성 */
    @Transactional(readOnly = true)
    public EnergyTypeResponse getUsageByType(Integer userId, Integer homeId,
                                             String range, String dateIso) {
        Window w = resolveWindow(range, dateIso);
        checkAccess(userId, homeId);

        CalcResult calc = calculate(userId, homeId, w);
        double total = calc.byType.values().stream().mapToDouble(Double::doubleValue).sum();

        List<EnergyTypeResponse.Item> items = calc.byType.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(e -> EnergyTypeResponse.Item.builder()
                    .deviceType(e.getKey())
                    .kwh(round3(e.getValue()))
                    .percentage(total > 0 ? round1(e.getValue() * 100.0 / total) : 0.0)
                    .build())
            .collect(Collectors.toList());

        return EnergyTypeResponse.builder()
                .range(w.range)
                .from(w.from)
                .to(w.to)
                .totalKwh(round3(total))
                .items(items)
                .build();
    }

    /** 윈도우 내 전원 on/off 이벤트를 기반으로 버킷/타입별 kWh 계산 */
    private CalcResult calculate(Integer userId, Integer homeId, Window w) {
        List<EnergyRow> inRange =
            irEventLogRepository.findPowerEventsInRange(userId, homeId, w.from, w.to);
        List<EnergyRow> lastBefore =
            irEventLogRepository.findLastPowerEventBefore(userId, homeId, w.from);

        record Key(String irDeviceId, String model) {}

        Map<Key, Boolean> initialOn = new HashMap<>();
        Map<Key, Double>  watts     = new HashMap<>();
        Map<Key, String>  type      = new HashMap<>();

        // 윈도우 시작 직전 상태로 초기 on/off 및 소비전력/타입 설정
        for (EnergyRow r : lastBefore) {
            Key k = new Key(r.getIrDeviceId(), r.getModel());
            initialOn.put(k, isOn(r.getKind()));
            watts.putIfAbsent(k, resolveWatts(r.getDeviceType(), r.getPowerConsumption()));
            if (r.getDeviceType() != null) type.putIfAbsent(k, r.getDeviceType());
        }
        // 윈도우 내 이벤트로 보완 (소비전력/타입 정보 보강, 미등록 키 채움)
        for (EnergyRow r : inRange) {
            Key k = new Key(r.getIrDeviceId(), r.getModel());
            watts.putIfAbsent(k, resolveWatts(r.getDeviceType(), r.getPowerConsumption()));
            if (r.getDeviceType() != null) type.putIfAbsent(k, r.getDeviceType());
            initialOn.putIfAbsent(k, false);
        }

        double[] buckets = new double[w.labels.size()];
        Map<String, Double> byType = new HashMap<>();

        // 기기별 이벤트 묶기
        Map<Key, List<EnergyRow>> grouped = inRange.stream()
            .collect(Collectors.groupingBy(r -> new Key(r.getIrDeviceId(), r.getModel()),
                                           LinkedHashMap::new, Collectors.toList()));

        // 각 기기의 on 구간을 버킷/타입에 누적
        for (Map.Entry<Key, List<EnergyRow>> entry : grouped.entrySet()) {
            Key key = entry.getKey();
            List<EnergyRow> events = entry.getValue();
            events.sort(Comparator.comparing(EnergyRow::getEventTime));

            boolean on = initialOn.getOrDefault(key, false);
            Instant cursor = w.from;

            double watt = watts.getOrDefault(key, DEFAULT_WATTS_FALLBACK);
            String tp   = type.getOrDefault(key, "기타");

            for (EnergyRow ev : events) {
                Instant t = ev.getEventTime();
                if (on) {
                    addToBuckets(cursor, t, watt, w, buckets);
                    addToType(cursor, t, watt, tp, byType);
                }
                on = isOn(ev.getKind());
                cursor = t;
            }
            // 마지막 이벤트 이후 구간 처리 (여전히 on이면 현재 버킷 경계까지)
            if (on) {
                Instant cap = endOfCurrentBucket(cursor, w);
                if (cap.isAfter(w.to)) cap = w.to;
                if (cursor.isBefore(cap)) {
                    addToBuckets(cursor, cap, watt, w, buckets);
                    addToType(cursor, cap, watt, tp, byType);
                }
            }
        }

        return new CalcResult(buckets, byType);
    }

    /** 이벤트 종류 문자열을 on/off Boolean으로 표준화 */
    private static boolean isOn(String kind) {
        if (kind == null) return false;
        String k = kind.toLowerCase(Locale.ROOT);
        if (k.equals("power_on") || k.equals("on"))  return true;
        if (k.equals("power_off") || k.equals("off")) return false;
        return false;
    }

    /** DB 지정 소비전력 우선, 없으면 타입별 기본값, 그래도 없으면 폴백 */
    private static double resolveWatts(String deviceType, Double dbWatts) {
        if (dbWatts != null && dbWatts > 0) return dbWatts;
        if (deviceType != null && DEFAULT_WATTS.containsKey(deviceType)) return DEFAULT_WATTS.get(deviceType);
        return DEFAULT_WATTS_FALLBACK;
    }

    /** 유저가 해당 homeId 접근 권한을 갖는지 검사 */
    private void checkAccess(Integer userId, Integer homeId) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (homeId == null) throw new IllegalArgumentException("homeId는 필수입니다.");
        if (!userHomeRepository.existsByUserIdAndHomeId(userId, homeId)) {
            throw new IllegalArgumentException("해당 집에 대한 접근 권한이 없습니다.");
        }
    }

    /** 버킷/라벨/시간범위를 포함하는 조회 윈도우 생성 */
    private Window resolveWindow(String range, String dateIso) {
        String r = (range == null || range.isBlank()) ? "day" : range.toLowerCase();
        ZoneId tz = DEFAULT_TZ;
        LocalDate base = (dateIso == null || dateIso.isBlank()) ? LocalDate.now(tz) : LocalDate.parse(dateIso);

        switch (r) {
            case "day": {
                // 기준일 00:00 ~ +1일, 시(hour) 단위 라벨
                ZonedDateTime s = base.atStartOfDay(tz);
                return new Window(r, s.toInstant(), s.plusDays(1).toInstant(), tz,
                        ChronoUnit.HOURS, hourlyLabels());
            }
            case "week": {
                // 월요일 시작 주 ~ +1주, 일(day) 단위 라벨(월~일)
                LocalDate start = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                ZonedDateTime s = start.atStartOfDay(tz);
                return new Window(r, s.toInstant(), s.plusWeeks(1).toInstant(), tz,
                        ChronoUnit.DAYS, weekdayKoLabels(start));
            }
            case "month": {
                // 최근 30일 ~ 오늘 포함(내일 00:00 배타), 일(day) 단위 라벨(MM-dd)
                LocalDate end = base;
                LocalDate start = end.minusDays(30);
                ZonedDateTime s = start.atStartOfDay(tz);
                ZonedDateTime e = end.plusDays(1).atStartOfDay(tz);

                return new Window(r,
                        s.toInstant(),
                        e.toInstant(),
                        tz,
                        ChronoUnit.DAYS,
                        dailyLabelsWithoutYear(start, 31));
            }

            case "year": {
                // 현재 달부터 과거 6개월, 월(month) 단위 라벨
                LocalDate startMonth = base.withDayOfMonth(1).minusMonths(5);
                LocalDate endMonthExclusive = base.withDayOfMonth(1).plusMonths(1);
                ZonedDateTime s = startMonth.atStartOfDay(tz);
                ZonedDateTime e = endMonthExclusive.atStartOfDay(tz);

                List<String> labels = monthlyLabelsBetween(startMonth, base);

                return new Window("year", s.toInstant(), e.toInstant(), tz,
                        ChronoUnit.MONTHS, labels);
            }

            default:
                throw new IllegalArgumentException("range는 day|week|month|year 중 하나여야 합니다.");
        }
    }

    /** 24개 시(hour) 라벨 생성: 00:00 ~ 23:00 */
    private static List<String> hourlyLabels() {
        List<String> l = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) l.add(String.format("%d:00", h));
        return l;
    }

    /** 타입별 kWh 합산 누적 */
    private static void addToType(Instant a, Instant b, double watts,
                                  String type, Map<String, Double> byType) {
        double kwh = Duration.between(a, b).toMillis() / 3600000.0 * (watts / 1000.0);
        byType.merge(type, kwh, Double::sum);
    }

    /** 버킷 경계(시/일/월)에 맞춰 구간 [a,b) kWh를 분할 누적 */
    private static void addToBuckets(Instant a, Instant b, double watts,
                                     Window w, double[] buckets) {
        if (!b.isAfter(a)) return;
        Instant cur = a;
        while (cur.isBefore(b)) {
            Instant next = nextBoundary(cur, w.unit, w.tz);
            if (next.isAfter(b)) next = b;
            int idx = bucketIndex(cur, w);
            if (idx >= 0 && idx < buckets.length) {
                double kwh = Duration.between(cur, next).toMillis() / 3600000.0 * (watts / 1000.0);
                buckets[idx] += kwh;
            }
            cur = next;
        }
    }

    /** 현재 시각 t가 속한 버킷의 다음 경계 시각(단위별) 계산 */
    private static Instant nextBoundary(Instant t, TemporalUnit unit, ZoneId tz) {
        ZonedDateTime z = t.atZone(tz);
        ZonedDateTime n;
        if (unit == ChronoUnit.HOURS)      n = z.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        else if (unit == ChronoUnit.DAYS)  n = z.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        else if (unit == ChronoUnit.MONTHS) n = z.withDayOfMonth(1).with(LocalTime.MIDNIGHT).plusMonths(1);
        else n = z.plusSeconds(1);
        return n.toInstant();
    }

    /** 시각 t가 속한 버킷의 인덱스 계산(단위: 시/일/월) */
    private static int bucketIndex(Instant t, Window w) {
        ZonedDateTime s = w.from.atZone(w.tz);
        ZonedDateTime c = t.atZone(w.tz);
        if (w.unit == ChronoUnit.HOURS)   return (int) Duration.between(s, c).toHours();
        if (w.unit == ChronoUnit.DAYS)    return (int) Duration.between(
                                                s.toLocalDate().atStartOfDay(w.tz),
                                                c.toLocalDate().atStartOfDay(w.tz)
                                            ).toDays();
        if (w.unit == ChronoUnit.MONTHS)  return (c.getYear() - s.getYear()) * 12 + (c.getMonthValue() - s.getMonthValue());
        return 0;
    }
    
    /** 마지막 이벤트 시각이 속한 버킷의 끝(단위 경계)을 반환 */
    private static Instant endOfCurrentBucket(Instant t, Window w) {
        ZonedDateTime z = t.atZone(w.tz);
        ZonedDateTime end;
        if (w.unit == ChronoUnit.HOURS) {
            end = z.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        } else if (w.unit == ChronoUnit.DAYS) {
            end = z.toLocalDate().plusDays(1).atStartOfDay(w.tz);
        } else if (w.unit == ChronoUnit.MONTHS) {
            end = z.withDayOfMonth(1).with(LocalTime.MIDNIGHT).plusMonths(1);
        } else {
            end = z.plusSeconds(1); // fallback
        }
        return end.toInstant();
    }

    /** 소수점 셋째 자리까지 반올림 */
    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    /** 소수점 한 자리까지 반올림 */
    private static double round1(double v) { return Math.round(v * 10.0)   / 10.0; }
    
    // ---- 라벨 유틸: 주(요일), 월(최근 30일), 연(최근 6개월) ----

    /** 한글 요일 배열 */
    private static final String[] DOW_KO = {"월", "화", "수", "목", "금", "토", "일"};

    /** 시작일 기준 7일간의 한글 요일 라벨 생성 (월~일) */
    private static List<String> weekdayKoLabels(LocalDate start) {
        List<String> l = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = start.plusDays(i).getDayOfWeek();
            l.add(toKoreanDow(dow));
        }
        return l;
    }

    /** DayOfWeek → "월/화/..." 변환 */
    private static String toKoreanDow(DayOfWeek d) {
        switch (d) {
            case MONDAY:    return DOW_KO[0];
            case TUESDAY:   return DOW_KO[1];
            case WEDNESDAY: return DOW_KO[2];
            case THURSDAY:  return DOW_KO[3];
            case FRIDAY:    return DOW_KO[4];
            case SATURDAY:  return DOW_KO[5];
            case SUNDAY:    return DOW_KO[6];
            default:        return "";
        }
    }

    /** 시작일 기준 N일간의 라벨을 MM-dd 포맷으로 생성 */
    private static List<String> dailyLabelsWithoutYear(LocalDate start, int days) {
        List<String> l = new ArrayList<>(days);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 0; i < days; i++) {
            l.add(start.plusDays(i).format(fmt));
        }
        return l;
    }

    /** 시작달~끝달(포함)의 월 라벨을 "MM" 형식으로 생성 */
    private static List<String> monthlyLabelsBetween(LocalDate startInclusive, LocalDate endInclusive) {
        LocalDate cur = startInclusive.withDayOfMonth(1);
        LocalDate end = endInclusive.withDayOfMonth(1);
        List<String> list = new ArrayList<>();
        while (!cur.isAfter(end)) {
            list.add(String.format("%d", cur.getMonthValue()));
            cur = cur.plusMonths(1);
        }
        return list;
    }

    /** 내부 계산결과: 버킷 배열과 타입별 kWh 합계 */
    private record CalcResult(double[] buckets, Map<String, Double> byType) {}

    /** 조회 윈도우: 범위/시간대/버킷단위/라벨 포함 */
    private record Window(String range, Instant from, Instant to, ZoneId tz,
                          TemporalUnit unit, List<String> labels) {}
}
