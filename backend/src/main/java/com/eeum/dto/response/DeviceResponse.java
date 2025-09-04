package com.eeum.dto.response;

import lombok.*;
import java.util.List;

import com.eeum.dto.request.DeviceRequest;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceResponse {
    private Integer totalCount;
    private List<DeviceRequest> items;
}
