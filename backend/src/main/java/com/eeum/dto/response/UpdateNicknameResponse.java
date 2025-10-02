package com.eeum.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter 
@Builder
public class UpdateNicknameResponse {
    private String nickname;
}