package com.eeum.dto.response;

import java.util.List;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateDeviceLocationsResponse {
    private int total;      // 요청 총 개수
    private int updated;    // 성공 개수
    private List<ErrorItem> errors; // 실패 목록

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ErrorItem {
        private Integer deviceId;
        private String  message;
    }
}
