package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserHomeListResponse {
    private List<UserHomeItemResponse> homes;
}
