package com.eeum.service;

import java.time.*;
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

    // 기간별 전체 사용량
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

    // 기간, 타입별 사용량
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

    // -------- 계산 공통부 --------
    private CalcResult calculate(Integer userId, Integer homeId, Window w) {
        List<EnergyRow> inRange =
            irEventLogRepository.findPowerEventsInRange(userId, homeId, w.from, w.to);
        List<EnergyRow> lastBefore =
            irEventLogRepository.findLastPowerEventBefore(userId, homeId, w.from);

        record Key(String irDeviceId, String model) {}

        Map<Key, Boolean> initialOn = new HashMap<>();
        Map<Key, Double>  watts     = new HashMap<>();
        Map<Key, String>  type      = new HashMap<>();

        // 윈도우 시작 직전의 상태로 초기 on/off 결정
        for (EnergyRow r : lastBefore) {
            Key k = new Key(r.getIrDeviceId(), r.getModel());
            initialOn.put(k, isOn(r.getKind()));
            watts.putIfAbsent(k, resolveWatts(r.getDeviceType(), r.getPowerConsumption()));
            if (r.getDeviceType() != null) type.putIfAbsent(k, r.getDeviceType());
        }
        // 윈도우 내 이벤트로 보완 (소비전력/타입 정보 확보)
        for (EnergyRow r : inRange) {
            Key k = new Key(r.getIrDeviceId(), r.getModel());
            watts.putIfAbsent(k, resolveWatts(r.getDeviceType(), r.getPowerConsumption()));
            if (r.getDeviceType() != null) type.putIfAbsent(k, r.getDeviceType());
            initialOn.putIfAbsent(k, false);
        }

        double[] buckets = new double[w.labels.size()];
        Map<String, Double> byType = new HashMap<>();

        Map<Key, List<EnergyRow>> grouped = inRange.stream()
            .collect(Collectors.groupingBy(r -> new Key(r.getIrDeviceId(), r.getModel()),
                                           LinkedHashMap::new, Collectors.toList()));

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

    // ---- helpers ----
    private static boolean isOn(String kind) {
        if (kind == null) return false;
        String k = kind.toLowerCase(Locale.ROOT);
        if (k.equals("power_on") || k.equals("on"))  return true;
        if (k.equals("power_off") || k.equals("off")) return false;
        return false;
    }

    private static double resolveWatts(String deviceType, Double dbWatts) {
        if (dbWatts != null && dbWatts > 0) return dbWatts;
        if (deviceType != null && DEFAULT_WATTS.containsKey(deviceType)) return DEFAULT_WATTS.get(deviceType);
        return DEFAULT_WATTS_FALLBACK;
    }

    private void checkAccess(Integer userId, Integer homeId) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (homeId == null) throw new IllegalArgumentException("homeId는 필수입니다.");
        if (!userHomeRepository.existsByUserIdAndHomeId(userId, homeId)) {
            throw new IllegalArgumentException("해당 집에 대한 접근 권한이 없습니다.");
        }
    }

    private record Window(String range, Instant from, Instant to, ZoneId tz,
                          TemporalUnit unit, List<String> labels) {}
    private record CalcResult(double[] buckets, Map<String, Double> byType) {}

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
                        ChronoUnit.DAYS, dailyLabels(start, 7));
            }
            case "month": {
                LocalDate start = base.withDayOfMonth(1);
                ZonedDateTime s = start.atStartOfDay(tz);
                return new Window(r, s.toInstant(), s.plusMonths(1).toInstant(), tz,
                        ChronoUnit.DAYS, dailyLabels(start, start.lengthOfMonth()));
            }
            case "year": {
                LocalDate start = base.withDayOfYear(1);
                ZonedDateTime s = start.atStartOfDay(tz);
                return new Window(r, s.toInstant(), s.plusYears(1).toInstant(), tz,
                        ChronoUnit.MONTHS, monthlyLabels());
            }
            default:
                throw new IllegalArgumentException("range는 day|week|month|year 중 하나여야 합니다.");
        }
    }

    private static List<String> hourlyLabels() {
        List<String> l = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) l.add(String.format("%02d:00", h));
        return l;
    }

    private static List<String> dailyLabels(LocalDate start, int days) {
        List<String> l = new ArrayList<>(days);
        for (int i = 0; i < days; i++) l.add(start.plusDays(i).toString());
        return l;
    }

    private static List<String> monthlyLabels() {
        List<String> l = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) l.add(String.format("%02d월", m));
        return l;
    }

    private static void addToType(Instant a, Instant b, double watts,
                                  String type, Map<String, Double> byType) {
        double kwh = Duration.between(a, b).toMillis() / 3600000.0 * (watts / 1000.0);
        byType.merge(type, kwh, Double::sum);
    }

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

    private static Instant nextBoundary(Instant t, TemporalUnit unit, ZoneId tz) {
        ZonedDateTime z = t.atZone(tz);
        ZonedDateTime n;
        if (unit == ChronoUnit.HOURS)      n = z.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        else if (unit == ChronoUnit.DAYS)  n = z.truncatedTo(ChronoUnit.DAYS).plusDays(1);
        else if (unit == ChronoUnit.MONTHS) n = z.withDayOfMonth(1).with(LocalTime.MIDNIGHT).plusMonths(1);
        else n = z.plusSeconds(1);
        return n.toInstant();
    }

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
    
    // 마지막 이벤트 시각이 속한 "현재 버킷"의 끝을 반환
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

    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }
    private static double round1(double v) { return Math.round(v * 10.0)   / 10.0; }
}
