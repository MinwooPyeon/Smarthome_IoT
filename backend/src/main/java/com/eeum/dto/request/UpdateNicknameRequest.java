package com.eeum.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter 
@NoArgsConstructor
public class UpdateNicknameRequest {
    private String newNickname;
}