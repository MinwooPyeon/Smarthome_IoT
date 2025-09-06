package com.eeum.dto.response;

import lombok.*;
import java.time.Instant;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceItemResponse {
    private Integer deviceId;
    private Integer homeId;
    private Integer roomId;
    private String  roomName;
    private String  deviceName;
    private Boolean active;
    private Instant registeredAt;
    private String  deviceDetail;
}
