package com.eeum.service;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.request.DeviceStatusRequest;
import com.eeum.dto.request.RoutineCreateRequest;
import com.eeum.dto.request.RoutineDetailRequest;
import com.eeum.dto.request.RoutineUpdateRequest;
import com.eeum.dto.response.RoutineDetailResponse;
import com.eeum.dto.response.RoutineResponse;
import com.eeum.entity.Routine;
import com.eeum.entity.RoutineDetail;
import com.eeum.entity.RoutineIcon;
import com.eeum.notification.RoutineExecutedEvent;
import com.eeum.repository.DeviceRepository;
import com.eeum.repository.RoutineIconRepository;
import com.eeum.repository.RoutineRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final RoutineIconRepository routineIconRepository;
    private final ObjectMapper objectMapper;

    private final DeviceService deviceService;           
    private final ApplicationEventPublisher publisher;   
    private final DeviceRepository deviceRepository;
    
    // 루틴 생성
    @Transactional
    public Integer create(Integer userId, RoutineCreateRequest req) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (req == null || req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("name은 필수입니다.");
        }
        
        if (routineRepository.existsByUserIdAndNameIgnoreCase(userId, req.getName().trim())) {
            throw new IllegalArgumentException("이미 존재하는 루틴 이름입니다: " + req.getName());
        }

        
        Integer iconId = req.getIconId();
        if (iconId != null) {
            if (!routineIconRepository.existsById(iconId)) {
                throw new IllegalArgumentException("유효하지 않은 iconId: " + iconId);
            }
        }

        LocalTime actTimeLocal = instantToLocalTimeMinute(req.getActTime());

        
        Routine routine = Routine.builder()
                .userId(userId)
                .name(req.getName())
                .triggerType(Boolean.TRUE)
                .routineWeekday(req.getRoutineWeekday())
                .routineDescription(req.getRoutineDescription())
                .actTime(actTimeLocal)
                .iconId(req.getIconId())  
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isAi(req.getIsAi())
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

        LocalTime actTimeLocal = instantToLocalTimeMinute(req.getActTime());
        
        if (req.getName() != null) entity.setName(req.getName());
        if (req.getTriggerType() != null) entity.setTriggerType(req.getTriggerType());
        if (req.getRoutineWeekday() != null) entity.setRoutineWeekday(req.getRoutineWeekday());
        if (req.getRoutineDescription() != null) entity.setRoutineDescription(req.getRoutineDescription());
        if (req.getActTime() != null) entity.setActTime(actTimeLocal);

        // details 전체 교체(요청 있을 때만)
        if (req.getDetail() != null) {
            List<RoutineDetail> newDetails = toDetails(req.getDetail());
            for (RoutineDetail d : newDetails) d.setRoutine(entity);

            entity.getDetails().clear();
            entity.getDetails().addAll(newDetails);
        }

        entity.setUpdatedAt(Instant.now());
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
    public List<RoutineResponse> list(Integer userId, Integer weekday) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        Comparator<Routine> cmp = Comparator.<Routine, LocalTime>comparing(r -> {
            LocalTime actTime = r.getActTime();
            return (actTime == null) ? LocalTime.MAX : actTime;
        });

        // 루틴 목록 조회
        List<Routine> routines = routineRepository.findAllWithDetailsByUserId(userId).stream()
                .filter(r -> weekday == null || weekday == 0
                        || (r.getRoutineWeekday() != null && ((r.getRoutineWeekday() & weekday) != 0)))
                .sorted(cmp)
                .toList();

        // 아이콘 id 수집
        Set<Integer> iconIds = routines.stream()
                .map(Routine::getIconId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 아이콘 URL 매핑
        Map<Integer, String> iconUrlMap = iconIds.isEmpty()
                ? Collections.emptyMap()
                : routineIconRepository.findAllById(iconIds).stream()
                    .collect(Collectors.toMap(
                            RoutineIcon::getIconId,
                            RoutineIcon::getIconUrl
                    ));

        // 4) DTO 변환
        return routines.stream()
                .map(r -> toResponseWithDetails(r, iconUrlMap.get(r.getIconId())))
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

        String iconUrl = null;
        if (entity.getIconId() != null) {
            iconUrl = routineIconRepository.findById(entity.getIconId())
                    .map(ri -> ri.getIconUrl())
                    .orElse(null);
        }
        return toResponseWithDetails(entity, iconUrl);
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


    private RoutineResponse toResponseWithDetails(Routine e, String iconUrl) {
        List<RoutineDetailResponse> detailDtos =
                (e.getDetails() == null ? List.<RoutineDetailResponse>of()
                        : e.getDetails().stream()
                        .map(d -> new RoutineDetailResponse(
                                d.getRoutineDetailId(),
                                d.getDeviceId(),
                                e.getRoutineId(),
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
                e.getIconId(),
                e.getIsAi(),
                iconUrl,
                detailDtos
        );
    }
    
    @Transactional
    public void executeRoutine(Integer userId, Integer routineId) {
        if (userId == null || routineId == null) {
            throw new IllegalArgumentException("userId, routineId는 필수입니다.");
        }

        Routine routine = routineRepository.findWithDetailsByRoutineIdAndUserId(routineId, userId)
                .orElseThrow(() -> new IllegalArgumentException("루틴이 존재하지 않거나 접근 권한이 없습니다."));

        List<RoutineDetail> details = routine.getDetails();
        if (details == null || details.isEmpty()) {
            // 상세 액션이 없으면 알림 없이 종료
            return;
        }

        // 상세 액션을 순서대로 실행
        for (RoutineDetail d : details) {
            // JSON 문자열 -> JsonNode
            JsonNode node;
            try {
                node = (d.getDeviceDetail() == null)
                        ? objectMapper.createObjectNode()
                        : objectMapper.readTree(d.getDeviceDetail());
            } catch (Exception e) {
                throw new IllegalArgumentException("루틴 detail의 deviceDetail JSON 파싱 실패: id=" + d.getRoutineDetailId(), e);
            }

            DeviceStatusRequest req = new DeviceStatusRequest();
            req.setDeviceDetail(node);

            // 실제 IR 제어/로그/큐 적재는 DeviceService가 담당
            deviceService.updateStatus(d.getDeviceId(), req);
        }

        // homeId 해석
        Integer firstDeviceId = details.get(0).getDeviceId();
        Integer homeId = resolveHomeIdByDevice(firstDeviceId);

        // 트랜잭션 커밋 후 푸시 발송되도록 도메인 이벤트 발행
        publisher.publishEvent(new RoutineExecutedEvent(
                homeId,
                routine.getRoutineId(),
                routine.getName(),
                java.time.OffsetDateTime.now()
        ));
    }
    
    private Integer resolveHomeIdByDevice(Integer deviceId) {
        // device.user_home_id → user_home.home_id
        var viaUserHome = deviceRepository.findHomeIdByDeviceIdViaUserHome(deviceId);
        if (viaUserHome.isPresent()) return viaUserHome.get();

        // device_positions.home_id
        var viaPositions = deviceRepository.findHomeIdByDeviceId(deviceId);
        if (viaPositions.isPresent()) return viaPositions.get();

        throw new IllegalStateException("deviceId=" + deviceId + " 에 대한 homeId를 찾을 수 없습니다.");
    }
    

    private LocalTime instantToLocalTimeMinute(Instant instant) {
        if (instant == null) return null;
        return LocalTime.ofInstant(instant, ZoneOffset.UTC)
                        .withSecond(0)
                        .withNano(0);
    }
}
