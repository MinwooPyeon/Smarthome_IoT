package com.eeum.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.eeum.dto.request.DeviceStatusRequest;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.dto.response.DeviceItemResponse;
import com.eeum.dto.response.DeviceResponse;
import com.eeum.entity.Device;
import com.eeum.entity.IrDevice;
import com.eeum.entity.IrRemoteir;
import com.eeum.repository.DeviceRepository;
import com.eeum.repository.DeviceRepository.DeviceRow;
import com.eeum.repository.IrDeviceRepository;
import com.eeum.repository.IrRemoteirRepository;
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
    private final ObjectMapper objectMapper = new ObjectMapper();


    // 디바이스 등록
    @Transactional
    public Boolean registerDevice(Integer userId, RegisterDeviceRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (req == null)    throw new IllegalArgumentException("요청 바디가 비었습니다.");
        if (req.getIrDeviceId() == null) throw new IllegalArgumentException("irDeviceId(시리얼)는 필수입니다.");
        if (req.getModel() == null || req.getModel().isBlank()) throw new IllegalArgumentException("model은 필수입니다.");
        if (req.getHomeId() == null) throw new IllegalArgumentException("homeId는 필수입니다.");
        if (req.getRoomId() == null) throw new IllegalArgumentException("roomId는 필수입니다.");

        Integer userHomeId = deviceRepository.findUserHomeId(userId, req.getHomeId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "user(" + userId + ")가 home(" + req.getHomeId() + ")에 소속되어 있지 않습니다."));

        if (!deviceRepository.existsRoomInHome(req.getHomeId(), req.getRoomId())) {
            throw new IllegalArgumentException(
                    "room(" + req.getRoomId() + ")은 home(" + req.getHomeId() + ")에 속하지 않습니다.");
        }
        
        // 사용자가 방에 이미 기기를 등록했는지 검증
        boolean duplicated = deviceRepository.existsDeviceInRoomByModel(userHomeId, req.getRoomId(), req.getModel());
        if (duplicated) {
            throw new IllegalArgumentException("이미 해당 방에 동일 모델(" + req.getModel() + ") 기기가 등록되어 있습니다.");
        }
        
        // IR 디바이스 존재 확인
        IrDevice irDevice = irDeviceRepository.findById(req.getIrDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ir_device_id: " + req.getIrDeviceId()));

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
        
        String roomName = deviceRepository.findRoomNameById(req.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("room을 찾을 수 없습니다: " + req.getRoomId()));

        // device 저장 (방이름 + 디바이스 type)
        String deviceName = roomName + " " + req.getDeviceType();

        // device 저장
        Device device = Device.builder()
                .deviceName(deviceName)
                .registeredAt(OffsetDateTime.now())
                .deviceDetail(new HashMap<>())
                .model(req.getModel())
                .irDeviceId(irDevice.getIrDeviceId())
                .userHomeId(userHomeId)
                .build();
        
        Device saved = deviceRepository.save(device);

        // 평면도 좌표 저장
        deviceRepository.insertDevicePosition(
                req.getX(), 
                req.getY(),
                saved.getDeviceId(),
                req.getRoomId(),
                req.getHomeId(),
                req.getModel()
        );

        return true;
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
                .type(r.getType())
                .registeredAt(r.getRegisteredAt())
                .deviceDetail(detailMap)
                .build();
    }

    // 디바이스 상태 변경
	public Integer updateStatus(Integer deviceId, DeviceStatusRequest request) {
		
	    if (deviceId == null) {
	        throw new IllegalArgumentException("deviceId는 필수입니다.");
	    }
	    if (request == null || request.getDeviceDetail() == null) {
	        throw new IllegalArgumentException("deviceDetail은 필수입니다.");
	    }

	    Device device = deviceRepository.findById(deviceId)
	            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

	    // 기존 deviceDetail 읽기
	    JsonNode current = toObjectNodeOrEmpty(device.getDeviceDetail());

	    // 새로 들어온 JSON
	    JsonNode patch = request.getDeviceDetail();

	    // 기존 JSON과 병합
	    JsonNode merged = deepMerge(current, patch);

	    Map<String, Object> mergedMap = objectMapper.convertValue(
	            merged, new TypeReference<Map<String, Object>>() {}
	    );
	    device.setDeviceDetail(mergedMap);

	    deviceRepository.save(device);
	    return device.getDeviceId();
	}
	
	// 기존 Map<String,Object>를 ObjectNode로 변환 (null이면 빈 객체)
	private ObjectNode toObjectNodeOrEmpty(Map<String, Object> src) {
	    if (src == null) {
	        return objectMapper.createObjectNode();
	    }
	    JsonNode node = objectMapper.valueToTree(src);
	    if (node != null && node.isObject()) {
	        return (ObjectNode) node;
	    }

	    return objectMapper.createObjectNode();
	}

	/** 깊은 병합: 객체는 키 단위로 병합, 배열/스칼라는 통째 교체 */
	private JsonNode deepMerge(JsonNode base, JsonNode patch) {
	    if (patch == null || patch.isNull()) return base;
	    if (!base.isObject() || !patch.isObject()) return patch;

	    ObjectNode baseObj = (ObjectNode) base;
	    patch.fieldNames().forEachRemaining(field -> {
	        JsonNode patchValue = patch.get(field);
	        JsonNode baseValue  = baseObj.get(field);

	        if (baseValue != null && baseValue.isObject() && patchValue.isObject()) {
	            deepMerge(baseValue, patchValue);
	        } else {
	            baseObj.set(field, patchValue);
	        }
	    });
	    return baseObj;
	}
}
