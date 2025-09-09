package com.eeum.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final IrRemoteirRepository irRemoteirRepository;
    private final IrDeviceRepository irDeviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();


    // 디바이스 등록
    public Boolean registerDevice(Integer userId, RegisterDeviceRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (req == null)    throw new IllegalArgumentException("요청 바디가 비었습니다.");
        if (req.getIrDeviceId() == null) throw new IllegalArgumentException("irDeviceId(시리얼)는 필수입니다.");
        if (req.getModel() == null || req.getModel().isBlank()) throw new IllegalArgumentException("model은 필수입니다.");
        if (req.getHomeId() == null) throw new IllegalArgumentException("homeId는 필수입니다.");
        if (req.getRoomId() == null) throw new IllegalArgumentException("roomId는 필수입니다.");

        if (!deviceRepository.existsUserHome(userId, req.getHomeId())) {
            throw new IllegalArgumentException("user(" + userId + ")가 home(" + req.getHomeId() + ")에 소속되어 있지 않습니다.");
        }

        if (!deviceRepository.existsRoomInHome(req.getHomeId(), req.getRoomId())) {
            throw new IllegalArgumentException("room(" + req.getRoomId() + ")은 home(" + req.getHomeId() + ")에 속하지 않습니다.");
        }
        
        // IR 디바이스 존재 확인
        IrDevice irDevice = irDeviceRepository.findById(req.getIrDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ir_device_id: " + req.getIrDeviceId()));

        // ir_remoteir 정보 등록
        IrRemoteir remoteir = irRemoteirRepository.findById(req.getModel())
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
        String baseName = (req.getBrand() != null && !req.getBrand().isBlank())
                ? (req.getBrand() + " " + req.getModel())
                : req.getModel();
        String deviceName = roomName + " " + baseName;


        Device device = Device.builder()
                .deviceName(deviceName)
                .registeredAt(OffsetDateTime.now())
                .deviceDetail(new HashMap<>())
                .model(req.getModel())
                .irDeviceId(irDevice.getIrDeviceId())
                .build();
        
        Device saved = deviceRepository.save(device);

        // 평면도 좌표 저장
        deviceRepository.insertDevicePosition(
                req.getX(), 
                req.getY(),
                saved.getDeviceId(),
                req.getRoomId(),
                req.getHomeId()
        );

        return true;
    }

//    
//    // roomName + deviceName로 deviceId 조회
//    public Integer findDeviceId(Integer userId, String roomName, String deviceName) {
//        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
//        if (roomName == null || roomName.isBlank()) throw new IllegalArgumentException("roomName은 필수입니다.");
//        if (deviceName == null || deviceName.isBlank()) throw new IllegalArgumentException("deviceName은 필수입니다.");
//
//        return deviceRepository.findDeviceId(userId, roomName.trim(), deviceName.trim())
//                .orElse(null);
//    }
//
//    
//    // device 전체/조건 목록 조회
//    public DeviceResponse findDevices(Integer userId, Boolean active, String type, String roomName, String deviceName) {
//        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
//        
//        String typeF  = norm(type);
//        String roomF  = norm(roomName);
//        String devF   = norm(deviceName);
//
//        List<DeviceRow> list = deviceRepository.findDeviceList(userId, active, typeF, roomF, devF);
//        List<DeviceItemResponse> items = list.stream().map(this::toItem).toList();
//
//        return DeviceResponse.builder()
//                .totalCount(items.size())
//                .items(items)
//                .build();
//    }
//
    // device 단건 조회
    public DeviceItemResponse getDevice(Integer userId, Integer deviceId) {
        if (userId == null)   throw new IllegalArgumentException("userId는 필수입니다.");
        if (deviceId == null) throw new IllegalArgumentException("deviceId는 필수입니다.");
        
        return deviceRepository.findDevice(userId, deviceId)
                .map(this::toItem)
                .orElse(null);
    }

    private DeviceItemResponse toItem(DeviceRow r) {
        JsonNode detail = null;
        try {
            if (r.getDeviceDetail() != null) {
                detail = objectMapper.readTree(r.getDeviceDetail());
            }
        } catch (Exception ignore) {}

        return DeviceItemResponse.builder()
                .deviceId(r.getDeviceId())
                .roomId(r.getRoomId())
                .remoteId(r.getRemoteId())
                .irDeviceId(r.getIrDeviceId())
                .brand(r.getBrand())
                .model(r.getModel())
                .type(r.getType())
                .deviceName(r.getDeviceName())
                .registeredAt(r.getRegisteredAt())
                .deviceDetail(detail)
                .build();
    }

//    // 디바이스 상태 변경
//	public Integer updateStatus(Integer deviceId, DeviceStatusRequest request) {
//		
//	    if (deviceId == null) {
//	        throw new IllegalArgumentException("deviceId는 필수입니다.");
//	    }
//	    if (request == null || request.getDeviceDetail() == null) {
//	        throw new IllegalArgumentException("deviceDetail은 필수입니다.");
//	    }
//
//	    Device device = deviceRepository.findById(deviceId)
//	            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
//
//	    ObjectMapper mapper = new ObjectMapper();
//
//	    // 기존 deviceDetail 읽기
//	    JsonNode current = parseJsonOrEmpty(mapper, device.getDeviceDetail());
//
//	    // 새로 들어온 JSON
//	    JsonNode patch = request.getDeviceDetail();
//
//	    // 기존 JSON과 병합
//	    JsonNode merged = deepMerge(current, patch);
//
//	    // 직렬화 후 저장
//	    try {
//	        device.setDeviceDetail(mapper.writeValueAsString(merged));
//	    } catch (Exception e) {
//	        throw new RuntimeException("deviceDetail 직렬화 실패", e);
//	    }
//	    deviceRepository.save(device);
//
//	    return device.getDeviceId();
//	}
//
//	private JsonNode parseJsonOrEmpty(ObjectMapper mapper, String json) {
//	    try {
//	        if (json == null || json.isBlank()) {
//	            return mapper.createObjectNode();
//	        }
//	        return mapper.readTree(json);
//	    } catch (Exception e) {
//	        return mapper.createObjectNode();
//	    }
//	}
//
//	/** 깊은 병합: 객체는 키 단위로 병합, 배열/스칼라는 통째 교체 */
//	private JsonNode deepMerge(JsonNode base, JsonNode patch) {
//	    if (patch == null || patch.isNull()) return base;
//	    if (!base.isObject() || !patch.isObject()) return patch;
//
//	    ObjectNode baseObj = (ObjectNode) base;
//	    patch.fieldNames().forEachRemaining(field -> {
//	        JsonNode patchValue = patch.get(field);
//	        JsonNode baseValue  = baseObj.get(field);
//
//	        if (baseValue != null && baseValue.isObject() && patchValue.isObject()) {
//	            deepMerge(baseValue, patchValue);
//	        } else {
//	            baseObj.set(field, patchValue);
//	        }
//	    });
//	    return baseObj;
//	}
//	
//    private static String norm(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
//
}
