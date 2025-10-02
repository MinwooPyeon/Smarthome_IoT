package com.eeum.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private Integer totalCount;
    private List<DeviceItemResponse> items;
}