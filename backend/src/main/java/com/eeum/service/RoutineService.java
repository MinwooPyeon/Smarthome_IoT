package com.eeum.service;

import com.eeum.dto.request.RoutineCreateRequest;
import com.eeum.dto.request.RoutineDetailRequest;
import com.eeum.dto.request.RoutineUpdateRequest;
import com.eeum.dto.response.RoutineDetailResponse;
import com.eeum.dto.response.RoutineResponse;
import com.eeum.entity.Routine;
import com.eeum.entity.RoutineDetail;
import com.eeum.repository.RoutineRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final ObjectMapper objectMapper;
    
    // actTime에서 '하루 중 시간'만 비교
    private LocalTime timeOfDayOrMax(OffsetDateTime odt) {
        return (odt == null) ? LocalTime.MAX : odt.toLocalTime();
    }

    // 루틴 생성
    @Transactional
    public Integer create(Integer userId, RoutineCreateRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Routine routine = Routine.builder()
                .userId(userId)
                .name(req.getName())
                .triggerType(Boolean.TRUE)
                .routineWeekday(req.getRoutineWeekday())
                .routineDescription(req.getRoutineDescription())
                .actTime(req.getActTime())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 요청 detail -> 엔티티 변환
        List<RoutineDetail> details = toDetails(req.getDetail());

        // 양방향 연관관계 연결(자식 -> 부모)
        for (RoutineDetail d : details) d.setRoutine(routine);

        // 부모 -> 자식
        routine.setDetails(details);

        Routine saved = routineRepository.save(routine);
        return saved.getRoutineId();
    }

    // 루틴 수정 (details 교체)
    @Transactional
    public void update(Integer userId, Integer routineId, RoutineUpdateRequest req) {
        if (userId == null || routineId == null) {
            throw new IllegalArgumentException("userId, routineId는 필수입니다.");
        }

        Routine entity = routineRepository.findByRoutineIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("루틴이 존재하지 않거나 접근 권한이 없습니다."));

        if (req.getName() != null) entity.setName(req.getName());
        if (req.getTriggerType() != null) entity.setTriggerType(req.getTriggerType());
        if (req.getRoutineWeekday() != null) entity.setRoutineWeekday(req.getRoutineWeekday());
        if (req.getRoutineDescription() != null) entity.setRoutineDescription(req.getRoutineDescription());
        if (req.getActTime() != null) entity.setActTime(req.getActTime());

        // details 전체 교체(요청 있을 때만)
        if (req.getDetail() != null) {
            List<RoutineDetail> newDetails = toDetails(req.getDetail());
            for (RoutineDetail d : newDetails) d.setRoutine(entity);

            entity.getDetails().clear();
            entity.getDetails().addAll(newDetails);
        }

        entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    // 루틴 삭제
    @Transactional
    public void delete(Integer userId, Integer routineId) {
        if (userId == null || routineId == null) {
            throw new IllegalArgumentException("userId, routineId는 필수입니다.");
        }

        Routine entity = routineRepository.findByRoutineIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("루틴이 존재하지 않거나 접근 권한이 없습니다."));

        routineRepository.delete(entity);
    }

    // 루틴 목록 조회 (요일 비트마스크로 필터 → 시간순 정렬)
    @Transactional(readOnly = true)
    public List<RoutineResponse> list(Integer userId, Integer mask) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        // actTime의 '하루 시간' 기준 오름차순
        Comparator<Routine> cmp = Comparator
                .comparing((Routine r) -> timeOfDayOrMax(r.getActTime()))
                .thenComparing(r -> r.getRoutineId() == null ? Integer.MAX_VALUE : r.getRoutineId());

        // 요일 적용하여 조회
            return routineRepository.findAllWithDetailsByUserId(userId).stream()
                .filter(r -> mask == null || mask == 0
                      || (r.getRoutineWeekday() != null && ((r.getRoutineWeekday() & mask) != 0)))
                .sorted(cmp)
                .map(this::toResponseWithDetails)
                .toList();
        }
    

    // 루틴 단건 조회
    @Transactional(readOnly = true)
    public RoutineResponse get(Integer userId, Integer routineId) {
        if (userId == null || routineId == null) {
            throw new IllegalArgumentException("userId, routineId는 필수입니다.");
        }

        Routine entity = routineRepository.findWithDetailsByRoutineIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("루틴이 존재하지 않거나 접근 권한이 없습니다."));

        return toResponseWithDetails(entity);
    }

    // ====================== 내부 헬퍼 ======================

    // 요청 detail -> 엔티티 리스트 변환
    private List<RoutineDetail> toDetails(List<RoutineDetailRequest> details) {
        if (details == null) return new ArrayList<>();

        List<RoutineDetail> list = new ArrayList<>();
        for (RoutineDetailRequest a : details) {
            if (a.getDeviceId() == null) {
                throw new IllegalArgumentException("actions[].deviceId는 필수입니다.");
            }

            String json = null;
            try {
                Map<String, Object> map = a.getDeviceDetail();
                if (map != null) json = objectMapper.writeValueAsString(map);
            } catch (Exception e) {
                throw new IllegalArgumentException("deviceDetail 직렬화 실패", e);
            }

            list.add(RoutineDetail.builder()
                    .deviceId(a.getDeviceId())
                    .deviceDetail(json)
                    .build());
        }
        return list;
    }


    private RoutineResponse toResponseWithDetails(Routine e) {
        List<RoutineDetailResponse> detailDtos =
                (e.getDetails() == null ? List.<RoutineDetailResponse>of()
                        : e.getDetails().stream()
                        .map(d -> new RoutineDetailResponse(
                                d.getRoutineDetailId(),
                                d.getDeviceId(),
                                d.getRoutine().getRoutineId(),
                                d.getDeviceDetail()
                        ))
                        .toList());

        return new RoutineResponse(
                e.getRoutineId(),
                e.getName(),
                e.getTriggerType(),
                e.getRoutineWeekday(),
                e.getRoutineDescription(),
                e.getActTime(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                detailDtos
        );
    }
}
