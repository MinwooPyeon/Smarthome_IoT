package com.eeum.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FloorplanItemResponse {
    private Integer floorplanId;
    private String imageUrl;
    private Instant createdAt;
    private Double square;
    private Double floorplansX;
    private Double floorplansY;
    private Integer homeId;
    private String homeName;
}
