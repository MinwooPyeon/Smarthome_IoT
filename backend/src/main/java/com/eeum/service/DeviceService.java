package com.eeum.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.eeum.dto.request.DeviceRequest;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.dto.response.DeviceResponse;
import com.eeum.entity.Device;
import com.eeum.repository.DeviceRepository;

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
    
    
    // 디바이스 목록 조회
    public DeviceResponse findDevices(Integer userId, Boolean active, String type, String roomName, String deviceName) {

        String typeFilter  = (type == null || type.isBlank()) ? null : type.trim();
        String roomFilter  = (roomName == null || roomName.isBlank()) ? null : roomName.trim();
        String deviceFilter = (deviceName == null || deviceName.isBlank()) ? null : deviceName.trim();

        List<DeviceRepository.DeviceList> rows = deviceRepository.searchList(userId, active, typeFilter, roomFilter, deviceFilter);
        
        List<DeviceRequest> items = rows.stream()
                .map(r -> new DeviceRequest(
                        r.getDeviceId(),
                        r.getDeviceName(),
                        r.getRoomName(),
                        r.getDeviceType(),
                        r.getActive()
                ))
                .toList();


        return new DeviceResponse(items.size(), items);
    }
}