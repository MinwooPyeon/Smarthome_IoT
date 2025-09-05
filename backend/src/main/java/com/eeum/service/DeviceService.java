package com.eeum.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.eeum.dto.request.DeviceStatusRequest;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.dto.response.DeviceItemResponse;
import com.eeum.dto.response.DeviceResponse;
import com.eeum.entity.Device;
import com.eeum.repository.DeviceRepository;
import com.eeum.repository.DeviceRepository.DeviceRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    // 디바이스 등록
    public Boolean registerDevice(Integer userId, RegisterDeviceRequest req) {

        Device device = new Device();
        device.setUserId(userId);
        device.setRoomId(req.getRoomId());
        device.setRemoteId(req.getRemoteId());
        device.setIrDeviceId(req.getIrDeviceId());
        device.setDeviceName(req.getDeviceName());
        device.setRegisteredAt(OffsetDateTime.now());
        device.setDeviceDetail(null);

        deviceRepository.save(device);

        return true;
    }
    
    // roomName + deviceName로 deviceId 조회
    public Integer findDeviceId(Integer userId, String roomName, String deviceName) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (roomName == null || roomName.isBlank()) throw new IllegalArgumentException("roomName은 필수입니다.");
        if (deviceName == null || deviceName.isBlank()) throw new IllegalArgumentException("deviceName은 필수입니다.");

        return deviceRepository.findDeviceId(userId, roomName.trim(), deviceName.trim())
                .orElse(null);
    }

    
    // device 전체/조건 목록 조회
    public DeviceResponse findDevices(Integer userId, Boolean active, String type, String roomName, String deviceName) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        
        String typeF  = norm(type);
        String roomF  = norm(roomName);
        String devF   = norm(deviceName);

        List<DeviceRow> list = deviceRepository.findDeviceList(userId, active, typeF, roomF, devF);
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
        return new DeviceItemResponse(
                r.getDeviceId(),
                r.getRoomId(),
                r.getRemoteId(),
                r.getIrDeviceId(),
                r.getBrand(),
                r.getModel(),
                r.getDeviceName(),
                r.getType(),
                r.getRegisteredAt(),
                r.getDeviceDetail()
        );
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

	    ObjectMapper mapper = new ObjectMapper();

	    // 기존 deviceDetail 읽기
	    JsonNode current = parseJsonOrEmpty(mapper, device.getDeviceDetail());

	    // 새로 들어온 JSON
	    JsonNode patch = request.getDeviceDetail();

	    // 기존 JSON과 병합
	    JsonNode merged = deepMerge(current, patch);

	    // 직렬화 후 저장
	    try {
	        device.setDeviceDetail(mapper.writeValueAsString(merged));
	    } catch (Exception e) {
	        throw new RuntimeException("deviceDetail 직렬화 실패", e);
	    }
	    deviceRepository.save(device);

	    return device.getDeviceId();
	}

	private JsonNode parseJsonOrEmpty(ObjectMapper mapper, String json) {
	    try {
	        if (json == null || json.isBlank()) {
	            return mapper.createObjectNode();
	        }
	        return mapper.readTree(json);
	    } catch (Exception e) {
	        return mapper.createObjectNode();
	    }
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
	
    private static String norm(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

}
