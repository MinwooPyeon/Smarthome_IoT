package com.eeum.dto.request;

import java.util.List;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateDeviceLocationsRequest {
    private List<UpdateDeviceLocationItem> items;
}
