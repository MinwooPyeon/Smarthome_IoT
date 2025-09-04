package com.eeum.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.dto.response.DeviceItemResponse;
import com.eeum.dto.response.DeviceResponse;
import com.eeum.entity.Device;
import com.eeum.repository.DeviceRepository;
import com.eeum.repository.DeviceRepository.DeviceRow;

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

    private static String norm(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
