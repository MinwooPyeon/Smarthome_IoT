package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UpdateNicknameResponse {
    private Integer userId;
    private String nickname;
}