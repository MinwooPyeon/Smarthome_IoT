package com.eeum.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceLogItemResponse {
    private String  deviceName;
    private Instant eventTime;
    private String  kind;
    private Integer roomId;
    private String  roomName;
}
