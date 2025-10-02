package com.eeum.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.eeum.repository.RoutineRepository;
import com.eeum.repository.RoutineRepository.DueRoutineRow;
import com.eeum.service.RoutineService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매 분 루틴 실행 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutineScheduler {

    private final RoutineRepository routineRepository;
    private final RoutineService routineService;

    /**
     * 매 분 0초에 실행 (KST 기준)
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void checkAndRunRoutines() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        int hour = now.getHour();
        int minute = now.getMinute();
        int weekdayMask = weekdayToMask(now.getDayOfWeek());
        
        List<DueRoutineRow> due = routineRepository.findDueRoutinesDailyKst(weekdayMask);
        
        log.info("[SCHED] due-count={}", (due == null ? 0 : due.size()));

        for (DueRoutineRow row : due) {
            try {
                routineService.executeRoutine(row.getUserId(), row.getRoutineId());
                log.info("[루틴 실행] routineId={}, userId={}", row.getRoutineId(), row.getUserId());
            } catch (Exception e) {
                log.warn("[루틴 실행 실패] routineId={}, error={}", row.getRoutineId(), e.getMessage(), e);
            }
        }
    }

    private int weekdayToMask(DayOfWeek day) {
        // 비트마스크
        return switch (day) {
            case MONDAY    -> 1;
            case TUESDAY    -> 2;
            case WEDNESDAY   -> 4;
            case THURSDAY -> 8;
            case FRIDAY  -> 16;
            case SATURDAY    -> 32;
            case SUNDAY  -> 64;
        };
    }
}