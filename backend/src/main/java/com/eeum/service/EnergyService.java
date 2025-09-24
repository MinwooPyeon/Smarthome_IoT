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
        "에어컨",       1500.0,
        "조명",         12.0,
        "선풍기",        45.0,
        "공기청정기",     40.0,
        "티비",         120.0
    );
    private static final double DEFAULT_WATTS_FALLBACK = 100.0; // 미정의 타입 기본 W

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
            // 마지막 이벤트 이후 구간 처리 (여전히 on이면 버킷 경계/윈도우 끝까지)
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
        if (k.equals("power_on") || k.equals("on"))   return true;
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
                ZonedDateTime s = base.atStartOfDay(tz);
                return new Window(r, s.toInstant(), s.plusDays(1).toInstant(), tz,
                        ChronoUnit.HOURS, hourlyLabels());
            }
            case "week": {
                LocalDate start = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                ZonedDateTime s = start.atStartOfDay(tz);
                return new Window(r, s.toInstant(), s.plusWeeks(1).toInstant(), tz,
                        ChronoUnit.DAYS, weekdayKoLabels(start));
            }
            case "month": {
                LocalDate end = base;
                LocalDate start = end.minusDays(30);
                ZonedDateTime s = start.atStartOfDay(tz);
                ZonedDateTime e = end.plusDays(1).atStartOfDay(tz);
                return new Window(r, s.toInstant(), e.toInstant(), tz,
                        ChronoUnit.DAYS, dailyLabelsWithoutYear(start, 31));
            }
            case "year": {
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
        double kwh = Duration.between(a, b).toMillis() / 3_600_000.0 * (watts / 1000.0);
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
                double kwh = Duration.between(cur, next).toMillis() / 3_600_000.0 * (watts / 1000.0);
                buckets[idx] += kwh;
            }
            cur = next;
        }
    }

    /** 현재 시각 t가 속한 버킷의 다음 경계 시각(단위별) 계산 */
    private static Instant nextBoundary(Instant t, TemporalUnit unit, ZoneId tz) {
        ZonedDateTime z = t.atZone(tz);
        ZonedDateTime n;
        if (unit == ChronoUnit.HOURS)       n = z.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        else if (unit == ChronoUnit.DAYS)   n = z.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        else if (unit == ChronoUnit.MONTHS) n = z.withDayOfMonth(1).with(LocalTime.MIDNIGHT).plusMonths(1);
        else                                n = z.plusSeconds(1);
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
            end = z.plusSeconds(1);
        }
        return end.toInstant();
    }

    /** 소수점 3자리 반올림 */
    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
    /** 소수점 1자리 반올림 */
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static <T> T nullSafe(T v, T def) { return v == null ? def : v; }

    /** 내부 계산결과: 버킷 배열과 타입별 kWh 합계 */
    private record CalcResult(double[] buckets, Map<String, Double> byType) {}

    /** 조회 윈도우: 범위/시간대/버킷단위/라벨 포함 */
    private record Window(String range, Instant from, Instant to, ZoneId tz,
                          TemporalUnit unit, List<String> labels) {}

    // -------------------- 집계 메서드 (active_seconds 기반) --------------------

    /**
     * 기간 내 ‘타입별’ 추정 kWh 합계
     * - 가동시간(초) × 기본소비전력(W) → Wh → kWh
     * - 레포지토리 집계 쿼리는 userId/homeId 기준으로 필터링됨
     */
    @Transactional(readOnly = true)
    public Map<String, Double> getUsageByTypeKwh(Integer userId, Integer homeId, LocalDate start, LocalDate end) {
        // 접근권한 체크
        Integer userHomeId = userHomeRepository.findUserHomeId(userId, homeId).orElseThrow(
            () -> new IllegalArgumentException("user(" + userId + ")가 home(" + homeId + ")에 소속되어 있지 않습니다.")
        );

        // window 구성 (일 단위 버킷 1칸만 필요해도 상관없음)
        ZoneId tz = ZoneId.of("Asia/Seoul");
        Instant from = start.atStartOfDay(tz).toInstant();
        Instant to   = end.plusDays(1).atStartOfDay(tz).toInstant();

        Window w = new Window("custom", from, to, tz, ChronoUnit.DAYS, List.of("total"));
        CalcResult calc = calculate(userId, homeId, w);

        // byType 그대로 반환 (소수 1자리 반올림)
        return calc.byType.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Math.round(e.getValue() * 10.0) / 10.0
            ));
    }


    @Transactional(readOnly = true)
    public OptionalInt getPeakHour(Integer userId, Integer homeId, LocalDate start, LocalDate end) {
        // 이벤트 로딩
        ZoneId tz = ZoneId.of("Asia/Seoul");
        Instant from = start.atStartOfDay(tz).toInstant();
        Instant to   = end.plusDays(1).atStartOfDay(tz).toInstant();

        var inRange = irEventLogRepository.findPowerEventsInRange(userId, homeId, from, to);
        var lastBefore = irEventLogRepository.findLastPowerEventBefore(userId, homeId, from);

        record Key(String irDeviceId, String model) {}
        Map<Key, Boolean> initialOn = new HashMap<>();

        for (IrEventLogRepository.EnergyRow r : lastBefore) {
            initialOn.put(new Key(r.getIrDeviceId(), r.getModel()), isOn(r.getKind()));
        }
        for (IrEventLogRepository.EnergyRow r : inRange) {
            initialOn.putIfAbsent(new Key(r.getIrDeviceId(), r.getModel()), false);
        }

        // 시간대별 가동 "초" 누적
        long[] secByHour = new long[24];

        Map<Key, List<IrEventLogRepository.EnergyRow>> grouped = inRange.stream()
            .collect(Collectors.groupingBy(r -> new Key(r.getIrDeviceId(), r.getModel())));

        for (var entry : grouped.entrySet()) {
            var key = entry.getKey();
            var events = entry.getValue();
            events.sort(Comparator.comparing(IrEventLogRepository.EnergyRow::getEventTime));

            boolean on = initialOn.getOrDefault(key, false);
            Instant cursor = from;

            for (var ev : events) {
                Instant t = ev.getEventTime();
                if (on) {
                    addToHourHistogram(cursor, t, tz, secByHour);
                }
                on = isOn(ev.getKind());
                cursor = t;
            }
            if (on) {
                Instant cap = to;
                addToHourHistogram(cursor, cap, tz, secByHour);
            }
        }

        int bestHour = -1;
        long bestVal = -1;
        for (int h = 0; h < 24; h++) {
            if (secByHour[h] > bestVal) { bestVal = secByHour[h]; bestHour = h; }
        }
        return bestHour >= 0 ? OptionalInt.of(bestHour) : OptionalInt.empty();
    }

    private static void addToHourHistogram(Instant a, Instant b, ZoneId tz, long[] secByHour) {
        if (!b.isAfter(a)) return;
        Instant cur = a;
        while (cur.isBefore(b)) {
            ZonedDateTime z = cur.atZone(tz);
            ZonedDateTime nextHour = z.truncatedTo(ChronoUnit.HOURS).plusHours(1);
            Instant boundary = nextHour.toInstant();
            Instant end = b.isBefore(boundary) ? b : boundary;

            long seconds = Duration.between(cur, end).getSeconds();
            int hour = z.getHour(); // 0~23
            secByHour[hour] += Math.max(0, seconds);

            cur = end;
        }
    }


    /** 기간 내 피크 ‘요일(1=월 … 7=일, ISO)’ – 가동시간(초) 기준 최대 요일 */
    @Transactional(readOnly = true)
    public OptionalInt getPeakWeekday(Integer userId, Integer homeId, LocalDate start, LocalDate end) {
        ZoneId tz = ZoneId.of("Asia/Seoul");
        Instant from = start.atStartOfDay(tz).toInstant();
        Instant to   = end.plusDays(1).atStartOfDay(tz).toInstant();

        var inRange = irEventLogRepository.findPowerEventsInRange(userId, homeId, from, to);
        var lastBefore = irEventLogRepository.findLastPowerEventBefore(userId, homeId, from);

        record Key(String irDeviceId, String model) {}
        Map<Key, Boolean> initialOn = new HashMap<>();

        for (IrEventLogRepository.EnergyRow r : lastBefore) {
            initialOn.put(new Key(r.getIrDeviceId(), r.getModel()), isOn(r.getKind()));
        }
        for (IrEventLogRepository.EnergyRow r : inRange) {
            initialOn.putIfAbsent(new Key(r.getIrDeviceId(), r.getModel()), false);
        }

        long[] secByIsoDow = new long[8]; // 1..7만 사용

        Map<Key, List<IrEventLogRepository.EnergyRow>> grouped = inRange.stream()
            .collect(Collectors.groupingBy(r -> new Key(r.getIrDeviceId(), r.getModel())));

        for (var entry : grouped.entrySet()) {
            var key = entry.getKey();
            var events = entry.getValue();
            events.sort(Comparator.comparing(IrEventLogRepository.EnergyRow::getEventTime));

            boolean on = initialOn.getOrDefault(key, false);
            Instant cursor = from;

            for (var ev : events) {
                Instant t = ev.getEventTime();
                if (on) addToWeekdayHistogram(cursor, t, tz, secByIsoDow);
                on = isOn(ev.getKind());
                cursor = t;
            }
            if (on) addToWeekdayHistogram(cursor, to, tz, secByIsoDow);
        }

        int bestDow = -1;
        long bestVal = -1;
        for (int iso = 1; iso <= 7; iso++) {
            if (secByIsoDow[iso] > bestVal) { bestVal = secByIsoDow[iso]; bestDow = iso; }
        }
        return bestDow > 0 ? OptionalInt.of(bestDow) : OptionalInt.empty();
    }

    private static void addToWeekdayHistogram(Instant a, Instant b, ZoneId tz, long[] secByIsoDow) {
        if (!b.isAfter(a)) return;
        Instant cur = a;
        while (cur.isBefore(b)) {
            ZonedDateTime z = cur.atZone(tz);
            ZonedDateTime dayEnd = z.toLocalDate().plusDays(1).atStartOfDay(tz);
            Instant boundary = dayEnd.toInstant();
            Instant end = b.isBefore(boundary) ? b : boundary;

            long seconds = Duration.between(cur, end).getSeconds();
            int iso = z.getDayOfWeek().getValue(); // 1=MON..7=SUN
            secByIsoDow[iso] += Math.max(0, seconds);

            cur = end;
        }
    }

    
    
 // 24시간 라벨은 이미 있으니, 아래 3개만 추가하세요.

    /** 시작일 기준 7일간의 한글 요일 라벨 생성 (월~일) */
    private static List<String> weekdayKoLabels(LocalDate start) {
        String[] DOW_KO = {"월", "화", "수", "목", "금", "토", "일"};
        List<String> l = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = start.plusDays(i).getDayOfWeek(); // MON..SUN
            int idx = (dow.getValue() - 1);                   // 1=MON -> 0
            l.add(DOW_KO[idx]);
        }
        return l;
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

    /** 시작달~끝달(포함)의 월 라벨을 "MM"(또는 한자리면 "M") 형식으로 생성 */
    private static List<String> monthlyLabelsBetween(LocalDate startInclusive, LocalDate endInclusive) {
        LocalDate cur = startInclusive.withDayOfMonth(1);
        LocalDate end = endInclusive.withDayOfMonth(1);
        List<String> list = new ArrayList<>();
        while (!cur.isAfter(end)) {
            list.add(String.valueOf(cur.getMonthValue())); // "1" ~ "12"
            cur = cur.plusMonths(1);
        }
        return list;
    }

}
