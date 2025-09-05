package com.eeum.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class FloorplanRequest {
    private Integer homeId;        // 집 ID
    private String imageUrl;       // 이미지 URL
    private Double square;         // 평수
    private OffsetDateTime createdAt; // 생성일시
}
