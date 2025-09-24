package com.eeum.dto.request;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateDeviceLocationItem {
    private Integer deviceId;
    private Integer homeId;
    private Integer roomId;
    private Double  x;
    private Double  y;
}
