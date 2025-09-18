package com.eeum.service;

import org.springframework.stereotype.Service;

import com.eeum.dto.request.IrRegisterRequest;
import com.eeum.mqtt.MqttOutService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IrService {

    private final MqttOutService mqttOutService;

    public void requestIrLearning(IrRegisterRequest request) throws Exception {
        int txId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

        mqttOutService.publishIrReq(
            request.getDeviceId(),
            txId,
            request.getBrand(),
            request.getDevice(),
            request.getFunction()
        );
    }
}