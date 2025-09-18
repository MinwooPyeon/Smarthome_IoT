package com.eeum.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.eeum.dto.request.DeviceStatusRequest;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.dto.request.UpdateDeviceLocationRequest;
import com.eeum.dto.response.DeviceItemResponse;
import com.eeum.dto.response.DeviceLocationResponse;
import com.eeum.dto.response.DeviceResponse;
import com.eeum.entity.Device;
import com.eeum.entity.IrDevice;
import com.eeum.entity.IrRemoteir;
import com.eeum.entity.Room;
import com.eeum.repository.DeviceRepository;
import com.eeum.repository.DeviceRepository.DeviceRow;
import com.eeum.repository.HubDeviceRepository;
import com.eeum.repository.IrDeviceRepository;
import com.eeum.repository.IrRemoteirRepository;
import com.eeum.repository.RoomRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final IrRemoteirRepository irRemoteirRepository;
    private final IrDeviceRepository irDeviceRepository;
    private final RoomRepository roomRepository;
    private final HubDeviceRepository hubDeviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    
    private static final Map<String, Set<String>> ALLOWED_KEYS = Map.of(
    	    "light", Set.of("power"),
    	    "air conditioner", Set.of("power", "temperature", "level"),
    	    "air purifier", Set.of("power", "level"),
    	    "fan", Set.of("power", "level")
    	);

    

    // 디바이스 등록
    @Transactional
    public Boolean registerDevice(Integer userId, RegisterDeviceRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (req.getHomeId() == null) throw new IllegalArgumentException("homeId는 필수입니다.");
        if (req.getRoomColor() == null) throw new IllegalArgumentException("roomColor는 필수입니다.");
        if (req.getIrDeviceId() == null) throw new IllegalArgumentException("deviceAddr는 필수입니다.");  // 수정

        Integer userHomeId = deviceRepository.findUserHomeId(userId, req.getHomeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "user(" + userId + ")가 home(" + req.getHomeId() + ")에 소속되어 있지 않습니다."));
        
        boolean hasHub = hubDeviceRepository.existsByUserHomeId(userHomeId);
        if (!hasHub) {
            throw new IllegalArgumentException("해당 집에는 아직 허브가 등록되지 않았습니다. 먼저 허브를 등록해주세요.");
        }

        Integer colorInt = parseHexColorToInt(req.getRoomColor());
        
        // home_id + room_color 로 방 조회
        Room room = roomRepository.findByHomeIdAndRoomColor(req.getHomeId(), colorInt)
                .orElseThrow(() -> new IllegalArgumentException("해당 색상의 방을 찾을 수 없습니다."));
        
        Integer roomId = room.getRoomId();
        
        // 사용자가 방에 이미 기기를 등록했는지 검증
        boolean duplicated = deviceRepository.existsDeviceInRoomByModel(userHomeId, roomId, req.getModel());
        if (duplicated) {
            throw new IllegalArgumentException("이미 해당 방에 동일 모델(" + req.getModel() + ") 기기가 등록되어 있습니다.");
        }
        
        // IR 디바이스 존재 확인
        IrDevice irDevice = irDeviceRepository.findById(req.getIrDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 deviceAddr: " + req.getIrDeviceId()));

        // ir_remoteir 정보 등록
        IrRemoteir model = irRemoteirRepository.findById(req.getModel())
        	    .orElseGet(() -> {
        	        IrRemoteir toSave = IrRemoteir.builder()
        	            .model(req.getModel())
        	            .brand(req.getBrand())
        	            .deviceType(req.getDeviceType())
        	            .createdAt(Instant.now())   
        	            .build();
        	        return irRemoteirRepository.save(toSave);
        	    });
        
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
            throw new IllegalArgumentException("deviceDetail은 필수입니다.");
        }

        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        // 디바이스 타입 확인
        String deviceType = irRemoteirRepository.findById(device.getModel())
                .map(IrRemoteir::getDeviceType)
                .orElseThrow(() -> new IllegalArgumentException(
                    "해당 model(" + device.getModel() + ")에 대한 deviceType을 찾을 수 없습니다."
                ));

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

        // 4) 다시 Map으로 변환하여 엔티티에 저장
        ObjectNode filtered = filterAllowedKeys((ObjectNode) mergedNode, allowed);

        Map<String, Object> mergedMap = objectMapper.convertValue(
                filtered, new TypeReference<Map<String, Object>>() {}
            );
        
        device.setDeviceDetail(mergedMap);
        deviceRepository.save(device);
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
}
