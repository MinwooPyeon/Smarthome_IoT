package com.eeum.service;

import com.eeum.dto.request.RoutineCreateRequest;
import com.eeum.dto.request.RoutineDetailRequest;
import com.eeum.dto.request.RoutineUpdateRequest;
import com.eeum.dto.response.RoutineResponse;
import com.eeum.entity.Routine;
import com.eeum.entity.RoutineDetail;
import com.eeum.repository.RoutineRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 루틴 생성
    @Transactional
    public Integer create(Integer userId, RoutineCreateRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }

        OffsetDateTime now = OffsetDateTime.now();

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

        routine.setDetails(toDetails(req.getDetail()));

        Routine saved = routineRepository.save(routine);
        return saved.getRoutineId();
    }

    // 루틴 수정
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

        if (req.getDetail() != null) {
            entity.setDetails(toDetails(req.getDetail()));
        }

        entity.setUpdatedAt(OffsetDateTime.now());
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

    // 루틴 전체 조회 (상세는 포함하지 않음)
    @Transactional(readOnly = true)
    public List<RoutineResponse> list(Integer userId) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        return routineRepository.findAllByUserIdOrderByRoutineIdAsc(userId)
                .stream().map(this::toResponse)
                .toList();
    }

    // 루틴 단건 조회 (상세는 포함하지 않음)
    @Transactional(readOnly = true)
    public RoutineResponse get(Integer userId, Integer routineId) {
        if (userId == null || routineId == null) {
            throw new IllegalArgumentException("userId, routineId는 필수입니다.");
        }
        Routine entity = routineRepository.findByRoutineIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("루틴이 존재하지 않거나 접근 권한이 없습니다."));
        return toResponse(entity);
    }

    // 요청 detail → 엔티티 리스트 변환
    private List<RoutineDetail> toDetails(List<RoutineDetailRequest> actions) {
        if (actions == null) return new ArrayList<>();
        List<RoutineDetail> list = new ArrayList<>();
        for (RoutineDetailRequest a : actions) {
            if (a.getDeviceId() == null) {
                throw new IllegalArgumentException("actions[].deviceId는 필수입니다.");
            }
            String json = null;
            try {
                Map<String, Object> map = a.getDeviceDetail();
                if (map != null) {
                    json = objectMapper.writeValueAsString(map);
                }
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

    
    private RoutineResponse toResponse(Routine e) {
        return new RoutineResponse(
                e.getRoutineId(),
                e.getName(),
                e.getTriggerType(),
                e.getRoutineWeekday(),
                e.getRoutineDescription(),
                e.getActTime(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
