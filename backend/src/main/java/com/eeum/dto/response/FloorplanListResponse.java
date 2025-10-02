package com.eeum.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FloorplanListResponse {
    private List<FloorplanItemResponse> items;
}