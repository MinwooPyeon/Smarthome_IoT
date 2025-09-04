package com.eeum.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import com.eeum.dto.Device;
import com.eeum.dto.request.RegisterDeviceRequest;
import com.eeum.repository.DeviceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;

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
}