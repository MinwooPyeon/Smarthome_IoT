package com.eeum.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.eeum.dto.request.DeviceStatusRequest;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.dto.request.UpdateDeviceLocationRequest;
import com.eeum.dto.response.DeviceItemResponse;
import com.eeum.dto.response.DeviceLocationResponse;
import com.eeum.dto.response.DeviceLogItemResponse;
import com.eeum.dto.response.DeviceResponse;
import com.eeum.entity.Device;
import com.eeum.entity.IrButton;
import com.eeum.entity.IrDevice;
import com.eeum.entity.IrEventLog;
import com.eeum.entity.IrRemoteir;
import com.eeum.entity.IrSignal;
import com.eeum.entity.IrTxQueue;
import com.eeum.entity.Room;
import com.eeum.mqtt.MqttOutService;
import com.eeum.repository.DeviceRepository;
import com.eeum.repository.DeviceRepository.DeviceRow;
import com.eeum.repository.HubDeviceRepository;
import com.eeum.repository.IrButtonRepository;
import com.eeum.repository.IrDeviceRepository;
import com.eeum.repository.IrEventLogRepository;
import com.eeum.repository.IrEventLogRepository.LogRow;
import com.eeum.repository.IrRemoteirRepository;
import com.eeum.repository.IrSignalRepository;
import com.eeum.repository.IrTxQueueRepository;
import com.eeum.repository.RoomRepository;
import com.eeum.repository.UserHomeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final IrRemoteirRepository irRemoteirRepository;
    private final IrDeviceRepository irDeviceRepository;
    private final RoomRepository roomRepository;
    private final HubDeviceRepository hubDeviceRepository;
    private final IrButtonRepository irButtonRepository;      
    private final IrSignalRepository irSignalRepository;     
    private final IrTxQueueRepository irTxQueueRepository;
    private final IrEventLogRepository irEventLogRepository;
    private final UserHomeRepository userHomeRepository;
    private final MqttOutService mqttService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    
    private static final Map<String, Set<String>> ALLOWED_KEYS = Map.of(
    	    "조명", Set.of("power"),
    	    "에어컨", Set.of("power", "temperature", "level"),
    	    "공기청정기", Set.of("power", "level"),
    	    "선풍기", Set.of("power", "level"),
    	    "티비", Set.of("power"),
    	    "빔프로젝터", Set.of("power")
    	);

    

    // 디바이스 등록
    @Transactional
    public Boolean registerDevice(Integer userId, RegisterDeviceRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (req.getHomeId() == null) throw new IllegalArgumentException("homeId는 필수입니다.");
        if (req.getRoomColor() == null) throw new IllegalArgumentException("roomColor는 필수입니다.");
        if (req.getIrDeviceId() == null) throw new IllegalArgumentException("IrDeviceId는 필수입니다.");

        Integer userHomeId = deviceRepository.findUserHomeId(userId, req.getHomeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "user(" + userId + ")가 home(" + req.getHomeId() + ")에 소속되어 있지 않습니다."));
        
        boolean hasHub = hubDeviceRepository.existsByUserHomeId(userHomeId);
        if (!hasHub) {
            throw new IllegalArgumentException("해당 집에는 아직 허브가 등록되지 않았습니다. 먼저 허브를 등록해주세요.");
        }

        Integer colorInt = parseHexColorToInt(req.getRoomColor());
        
        // home_id + room_color 로 방 조회
        Room room = roomRepository
                .findNearestByHomeIdAndRoomColorWithinTol(req.getHomeId(), colorInt, 10000000)
                .orElseThrow(() -> new IllegalArgumentException("해당 색상(±" + 10000000 + ")의 방을 찾을 수 없습니다."));

        
        Integer roomId = room.getRoomId();
        
        // 사용자가 방에 이미 기기를 등록했는지 검증
        boolean duplicated = deviceRepository.existsDeviceInRoomByModel(userHomeId, roomId, req.getModel());
        if (duplicated) {
            throw new IllegalArgumentException("이미 해당 방에 동일 모델(" + req.getModel() + ") 기기가 등록되어 있습니다.");
        }
        
        // IR 디바이스 존재 확인
        IrDevice irDevice = irDeviceRepository.findById(req.getIrDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 IrDeviceId: " + req.getIrDeviceId()));
        
        if (irDevice.getHubDevice() == null || irDevice.getHubDevice().isBlank()) {
            String hubDeviceId = hubDeviceRepository.findHubDeviceIdByUserHomeId(userHomeId)
                    .orElseThrow(() -> new IllegalStateException(
                            "userHomeId=" + userHomeId + " 에 바인딩된 허브가 없습니다."));
            irDevice.setHubDevice(hubDeviceId);
            irDeviceRepository.save(irDevice); // 주입된 허브를 영속화
            log.info("[REGISTER] IR 디바이스 허브 주입: irDeviceId={}, hubDeviceId={}",
                    irDevice.getIrDeviceId(), hubDeviceId);
        }

        // ir_remoteir 정보 등록
        IrRemoteir model = irRemoteirRepository.findById(req.getModel())
        	    .orElseGet(() -> {
        	        IrRemoteir toSave = IrRemoteir.builder()
        	            .model(req.getModel())
        	            .brand(req.getBrand())
        	            .deviceType(req.getDeviceType())
        	            .createdAt(Instant.now())
        	            .powerConsumption(500.0f)
        	            .build();
        	        return irRemoteirRepository.save(toSave);
        	    });
        
        if (model.getPowerConsumption() == null) {
            model.setPowerConsumption(500.0f);
            irRemoteirRepository.save(model); 
            log.info("[REGISTER] ir_remoteir 기본 소비전력 보정: model={}, power_consumption=500.0", model.getModel());
        }
        
        String roomName = deviceRepository.findRoomNameById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("room을 찾을 수 없습니다: " + roomId));

        // device 저장 (방이름 + 디바이스 type)
        String deviceName = roomName + " " + req.getDeviceType();

        Map<String, Object> defaultDetail = new HashMap<>();
        defaultDetail.put("power", false);
        
        // device 저장
        Device saved = deviceRepository.save(
                Device.builder()
                        .deviceName(deviceName)
                        .registeredAt(OffsetDateTime.now())
                        .deviceDetail(defaultDetail)
                        .model(req.getModel())
                        .irDeviceId(irDevice.getIrDeviceId())
                        .userHomeId(userHomeId)
                        .build()
        );

        
        // 평면도 좌표 저장
        deviceRepository.insertDevicePosition(
                req.getFloorplansX(), 
                req.getFloorplansY(),
                saved.getDeviceId(),
                roomId,
                req.getHomeId(),
                req.getModel()
        );
        
        try {
        	String hubDeviceId = irDevice.getHubDevice();
            if (hubDeviceId == null || hubDeviceId.isBlank()) {
                throw new IllegalStateException("ir_device.hub_device_id가 비어있음: " + irDevice.getIrDeviceId());
            } 

            int txId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

            mqttService.publishSendDevice(
                    hubDeviceId,                   // hub/{deviceId}/sendDevice
                    txId,
                    irDevice.getIrDeviceId(),      // 등록할 IR 송신기 ID
                    req.getDeviceType(),           // 가전 타입
                    true                           // 등록 모드
            );
            log.info("[MQTT] sendDevice 발행 완료: hubDeviceId={}, irDeviceId={}, type={}",
                    hubDeviceId, irDevice.getIrDeviceId(), req.getDeviceType()); 
        } catch (Exception e) {
            
            log.warn("[MQTT] sendDevice 발행 실패(등록은 완료 처리): {}", e.getMessage(), e);
        }
        
        return true;
    }

    // 방 색깔 string -> Integer
    private int parseHexColorToInt(String hex) {
        String v = hex.trim();
        if (v.startsWith("#")) v = v.substring(1);
        if (v.length() != 6 || !v.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("유효하지 않은 색상값입니다. 기대형식: #RRGGBB 또는 RRGGBB");
        }
        return Integer.parseInt(v, 16);
    }
    
  
    // device 전체/조건 목록 조회
    public DeviceResponse findDevices(Integer userId, Boolean power, String type, String roomName, String deviceName) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        List<DeviceRow> list = deviceRepository.findDeviceList(userId, power, type, roomName, deviceName);
        List<DeviceItemResponse> items = list.stream().map(this::toItem).toList();

        return DeviceResponse.builder()
                .totalCount(items.size())
                .items(items)
                .build();
    }

    // device 단건 조회
    public DeviceItemResponse getDevice(Integer userId, Integer deviceId) {
        if (userId == null)   throw new IllegalArgumentException("userId는 필수입니다.");
        if (deviceId == null) throw new IllegalArgumentException("deviceId는 필수입니다.");
        
        return deviceRepository.findDevice(userId, deviceId)
                .map(this::toItem)
                .orElse(null);
    }

    private DeviceItemResponse toItem(DeviceRow r) {
        Map<String, Object> detailMap = new HashMap<>();
        try {
            if (r.getDeviceDetail() != null && !r.getDeviceDetail().isBlank()) {
                detailMap = objectMapper.readValue(r.getDeviceDetail(), Map.class);
            }
        } catch (Exception e) {
        }

        return DeviceItemResponse.builder()
                .deviceId(r.getDeviceId())
                .roomId(r.getRoomId())
                .irDeviceId(r.getIrDeviceId())
                .brand(r.getBrand())
                .model(r.getModel())
                .deviceName(r.getDeviceName())
                .deviceType(r.getDeviceType())
                .registeredAt(r.getRegisteredAt())
                .deviceDetail(detailMap)
                .x(r.getX())
                .y(r.getY())
                .build();
    }

    // 디바이스 상태 변경
    @Transactional
    public Integer updateStatus(Integer deviceId, DeviceStatusRequest request) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId는 필수입니다.");
        }
        if (request == null || request.getDeviceDetail() == null) {
            throw new IllegalArgumentException("deviceDetail은 JSON object여야 합니다.");
        }

        // Device 조회
        java.util.Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Device not found: " + deviceId);
        }
        Device device = deviceOpt.get();

        // 디바이스 타입 확인: 람다 제거 + null 보호
        java.util.Optional<IrRemoteir> remoteirOpt = irRemoteirRepository.findById(device.getModel());
        if (remoteirOpt.isEmpty() || remoteirOpt.get().getDeviceType() == null) {
            throw new IllegalArgumentException("해당 model(" + device.getModel() + ")에 대한 deviceType을 찾을 수 없습니다.");
        }
        String deviceType = remoteirOpt.get().getDeviceType();

        Set<String> allowed = ALLOWED_KEYS.getOrDefault(deviceType.toLowerCase(), Set.of("power"));

        // 1) 기존 값(Map) -> JsonNode로
        JsonNode currentNode = toObjectNodeOrEmpty(device.getDeviceDetail());

        // 2) 들어온 패치(JsonNode)
        JsonNode patchNode = request.getDeviceDetail();
        if (!patchNode.isObject()) {
            throw new IllegalArgumentException("deviceDetail은 JSON object여야 합니다.");
        }

        // 3) 깊은 병합
        JsonNode mergedNode = deepMerge(currentNode, patchNode);

        // 4) 다시 Map으로 변환하여 엔티티에 저장 (허용 키만 유지)
        ObjectNode filtered = filterAllowedKeys((ObjectNode) mergedNode, allowed);
        Map<String, Object> mergedMap = objectMapper.convertValue(
                filtered, new TypeReference<Map<String, Object>>() {}
        );
        device.setDeviceDetail(mergedMap);
        deviceRepository.save(device);
        
        log.info("[DEVICE] saved id={} detail={}", deviceId, safeJson(mergedMap));

        String model = device.getModel();
        String irDeviceId = device.getIrDeviceId();
        OffsetDateTime now = OffsetDateTime.now();
        ObjectNode updatedNode = (ObjectNode) patchNode;

        for (Iterator<Map.Entry<String, JsonNode>> it = updatedNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String category = entry.getKey(); // 예: "power", "temperature"

            try {
                // 허용 키만 처리
                if (!allowed.contains(category)) {
                    continue;
                }

                // 현재값과 요청값 정규화 비교 → 동일하면 스킵
                JsonNode incoming = entry.getValue();
                JsonNode currentVal = currentNode.get(category);
                boolean isChanged = false;
                String kindValue = null;  // 세부 카테고리 (power_on/off, level_N, temperature_N)

                switch (category) {
                    case "power": {
                        // 요청값 → boolean 정규화
                        boolean newPower;
                        if (incoming.isBoolean()) newPower = incoming.asBoolean();
                        else if (incoming.isNumber()) newPower = incoming.asInt() != 0;
                        else if (incoming.isTextual()) {
                            String s = incoming.asText().trim().toLowerCase();
                            newPower = "true".equals(s) || "1".equals(s);
                        } else newPower = false;

                        // 현재값 → boolean 정규화
                        if (currentVal == null || currentVal.isNull()) {
                            isChanged = true;
                        } else if (currentVal.isBoolean()) {
                            isChanged = currentVal.asBoolean() != newPower;
                        } else if (currentVal.isNumber()) {
                            isChanged = (currentVal.asInt() != 0) != newPower;
                        } else if (currentVal.isTextual()) {
                            String s = currentVal.asText().trim().toLowerCase();
                            boolean oldPower = "true".equals(s) || "1".equals(s);
                            isChanged = oldPower != newPower;
                        }

                        if (!isChanged) {
                            continue;
                        }
                        kindValue = newPower ? "power_on" : "power_off";
                        break;
                    }
                    case "temperature": {
                        int newTemp;
                        if (incoming.isNumber()) newTemp = incoming.asInt();
                        else if (incoming.isTextual()) {
                            String s = incoming.asText().trim();
                            try { newTemp = Integer.parseInt(s); }
                            catch (NumberFormatException e) { continue; }
                        } else continue;

                        if (currentVal == null || currentVal.isNull()) {
                            isChanged = true;
                        } else if (currentVal.isNumber()) {
                            isChanged = (currentVal.asInt() != newTemp);
                        } else if (currentVal.isTextual()) {
                            String s = currentVal.asText().trim();
                            try {
                                int oldTemp = Integer.parseInt(s);
                                isChanged = (oldTemp != newTemp);
                            } catch (NumberFormatException e) {
                                isChanged = true;
                            }
                        }

                        if (!isChanged) {
                            continue;
                        }
                        kindValue = "temperature_" + newTemp;
                        break;
                    }
                    case "level": {
                        int newLevel;
                        if (incoming.isNumber()) newLevel = incoming.asInt();
                        else if (incoming.isTextual()) {
                            String s = incoming.asText().trim();
                            try { newLevel = Integer.parseInt(s); }
                            catch (NumberFormatException e) { continue; }
                        } else continue;

                        if (currentVal == null || currentVal.isNull()) {
                            isChanged = true;
                        } else if (currentVal.isNumber()) {
                            isChanged = (currentVal.asInt() != newLevel);
                        } else if (currentVal.isTextual()) {
                            String s = currentVal.asText().trim();
                            try {
                                int oldLevel = Integer.parseInt(s);
                                isChanged = (oldLevel != newLevel);
                            } catch (NumberFormatException e) {
                                isChanged = true;
                            }
                        }

                        if (!isChanged) {
                            continue;
                        }
                        kindValue = "level_" + newLevel;
                        break;
                    }
                    default:
                        continue;
                }

                // buttonId 조회
                java.util.Optional<IrButton> buttonOpt = irButtonRepository.findByModelAndCategory(model, kindValue);
                if (buttonOpt.isEmpty()) {
                    throw new IllegalArgumentException("버튼 없음: model=" + model + ", category=" + kindValue);
                }
                Integer buttonId = buttonOpt.get().getButtonId();

                // signalId 조회
                java.util.Optional<Integer> signalIdOpt =
                        irSignalRepository.findSignalIdByModelAndButtonId(model, buttonId);
                if (signalIdOpt.isEmpty()) {
                    throw new IllegalArgumentException("시그널 없음: model=" + model + ", buttonId=" + buttonId);
                }
                Integer signalId = signalIdOpt.get();

                // signal 조회
                java.util.Optional<IrSignal> signalOpt = irSignalRepository.findById(signalId);
                if (signalOpt.isEmpty()) {
                    throw new IllegalArgumentException("시그널 객체 없음: signalId=" + signalId);
                }
                IrSignal signal = signalOpt.get();

                List<Integer> rawData = Arrays.stream(signal.getSamplesUs())
                        .boxed()
                        .collect(Collectors.toList());

                // ir_tx_queue insert
                UUID txId = UUID.randomUUID();
                IrTxQueue tx = IrTxQueue.builder()
                        .txId(txId)
                        .scheduledAt(now)
                        .priority(1)
                        .repeatCount(0)
                        .intervalMs(0)
                        .status("SENT")
                        .lastError(null)
                        .createdAt(now)
                        .signalId(signalId)
                        .irDeviceId(irDeviceId)
                        .model(model)
                        .build();
                irTxQueueRepository.save(tx);
                log.info("[IR 큐 등록] txId={}, model={}, category={}", txId, model, category);

                // 이벤트 로그 저장: kind=세부 카테고리
                irEventLogRepository.save(
                        IrEventLog.builder()
                                .eventTime(now)
                                .kind(kindValue)
                                .irDeviceId(irDeviceId)
                                .txId(txId)        // DB에는 UUID 그대로 저장
                                .model(model)
                                .build()
                );

                // 허브 디바이스 ID 조회
                java.util.Optional<IrDevice> irDevOpt = irDeviceRepository.findById(irDeviceId);
                if (irDevOpt.isEmpty() || irDevOpt.get().getHubDevice() == null || irDevOpt.get().getHubDevice().isBlank()) {
                    throw new IllegalStateException("hub_device_id 없음: " + irDeviceId);
                }
                String hubDeviceId = irDevOpt.get().getHubDevice();

                // MQTT 발행: function=세부 카테고리(A안)
                mqttService.publishControl(
                        hubDeviceId,
                        txId,           // UUID를 넘기면 내부에서 hashCode()로 int 변환
                        irDeviceId,
                        deviceType,
                        rawData,
                        kindValue,      // ← 세부 카테고리 전달
                        List.of(),
                        model
                );

            } catch (Exception e) {
                log.warn("[IR 전송 실패] model={}, category={}, error={}", model, category, e.getMessage(), e);
            }
        }

        return device.getDeviceId();
    }



    // 필터링 함수
    private ObjectNode filterAllowedKeys(ObjectNode node, Set<String> allowed) {
        ObjectNode filtered = objectMapper.createObjectNode();
        node.fieldNames().forEachRemaining(field -> {
            if (allowed.contains(field)) {
                filtered.set(field, node.get(field));
            }
        });
        return filtered;
    }

    
    /**
     * 기존 Map<String, Object>를 ObjectNode로 바꿔준다.
     */
    private ObjectNode toObjectNodeOrEmpty(Map<String, Object> source) {
        if (source == null) {
            return objectMapper.createObjectNode();
        }
        JsonNode node = objectMapper.valueToTree(source);
        if (node != null && node.isObject()) {
            return (ObjectNode) node;
        }
        
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("value", node == null ? objectMapper.nullNode() : node);
        return wrapper;
    }

    /**
     * JSON 깊은 병합 (ObjectNode 기준)
     * - 같은 key가 Object/Object면 재귀 병합
     * - 배열은 들어온 값으로 대체
     */
    private ObjectNode deepMerge(JsonNode current, JsonNode patch) {
        ObjectNode result = current.isObject()
            ? ((ObjectNode) current).deepCopy()
            : objectMapper.createObjectNode();

        patch.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            JsonNode patchValue = entry.getValue();
            JsonNode currentValue = result.get(field);

            if (currentValue != null && currentValue.isObject() && patchValue.isObject()) {
                result.set(field, deepMerge(currentValue, patchValue));
            } else {
                result.set(field, patchValue);
            }
        });
        return result;
    }

	
	// 디바이스 삭제
    @Transactional
    public void deleteDevice(Integer userId, Integer deviceId) {
        if (userId == null)   throw new IllegalArgumentException("userId는 필수입니다.");
        if (deviceId == null) throw new IllegalArgumentException("deviceId는 필수입니다.");

        boolean owns = deviceRepository.userOwnsDevice(userId, deviceId);
        if (!owns) {
            throw new IllegalArgumentException("디바이스가 없거나 접근 권한이 없습니다. deviceId=" + deviceId);
        }
        
        deviceRepository.deleteRoutineDetails(deviceId);
        deviceRepository.deleteCommands(deviceId);
        deviceRepository.deletePositions(deviceId);
        int deleted = deviceRepository.deleteDeviceHard(deviceId);

        if (deleted == 0) {
            throw new IllegalStateException("디바이스 삭제에 실패했습니다. deviceId=" + deviceId);
        }
    }
    
    // 디바이스 평면도 좌표 위치 수정
    @Transactional
    public Integer updateLocation(Integer userId, Integer deviceId, UpdateDeviceLocationRequest req) {
        if (userId == null)   throw new IllegalArgumentException("userId는 필수입니다.");
        if (deviceId == null) throw new IllegalArgumentException("deviceId는 필수입니다.");
        if (req == null)      throw new IllegalArgumentException("요청 바디가 비었습니다.");

        // 소유권 확인
        if (!deviceRepository.userOwnsDevice(userId, deviceId)) {
            throw new NoSuchElementException("디바이스가 없거나 권한이 없습니다. deviceId=" + deviceId);
        }
        
        Integer userHomeId = deviceRepository.findUserHomeId(userId, req.getHomeId())
        	    .orElseThrow(() -> new IllegalArgumentException(
        	        "사용자가 home(" + req.getHomeId() + ")에 소속되어 있지 않습니다."));
        
        if (!deviceRepository.existsRoomInHome(req.getHomeId(), req.getRoomId())) {
            throw new IllegalArgumentException("room(" + req.getRoomId() + ")은 home(" + req.getHomeId() + ")에 속하지 않습니다.");
        }
    
        int updated = deviceRepository.updateDevicePosition(
                deviceId, req.getHomeId(), req.getRoomId(), req.getX(), req.getY());

        if (updated == 0) {
            throw new NoSuchElementException("디바이스의 위치 변경을 실패했습니다. deviceId=" + deviceId);
        }
        return deviceId;
    }


    // 디바이스 평면도 좌표 목록 조회
    public List<DeviceLocationResponse> listDeviceLocations(Integer userId, Integer homeId) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (homeId == null) throw new IllegalArgumentException("homeId는 필수입니다.");

        if (deviceRepository.findUserHomeId(userId, homeId).isEmpty()) {
            throw new IllegalArgumentException("사용자가 home(" + homeId + ")에 소속되어 있지 않습니다.");
        }

        List<DeviceRepository.DeviceLocationRow> rows =
                deviceRepository.findDeviceLocationsInHome(userId, homeId);

        return rows.stream()
                .map(r -> DeviceLocationResponse.builder()
                        .positionId(r.getPositionId())
                        .deviceId(r.getDeviceId())
                        .homeId(r.getHomeId())
                        .roomId(r.getRoomId())
                        .x(r.getX())
                        .y(r.getY())
                        .build())
                .toList();
    }
    
    
    // 디바이스 로그
    @Transactional
    public List<DeviceLogItemResponse> listRecentLogs(Integer userId, Integer homeId, Integer limit) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (homeId == null) throw new IllegalArgumentException("homeId는 필수입니다.");

        int effectiveLimit = (limit == null || limit <= 0 || limit > 500) ? 100 : limit;

        boolean hasAccess = userHomeRepository.existsByUserIdAndHomeId(userId, homeId);
        if (!hasAccess) {
            throw new IllegalArgumentException("해당 집에 대한 접근 권한이 없습니다.");
        }

        List<LogRow> rows = irEventLogRepository.findRecentLogsByUserAndHome(userId, homeId, effectiveLimit);

        List<DeviceLogItemResponse> result = rows.stream()
            .map(r -> new DeviceLogItemResponse(
                r.getDeviceName(),
                r.getEventTime(),
                r.getKind(),
                r.getRoomId(),
                r.getRoomName()
            ))
            .collect(Collectors.toList());

        return result;
    }
    
    private String safeJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return String.valueOf(o); }
    }
}
