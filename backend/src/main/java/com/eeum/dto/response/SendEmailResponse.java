package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SendEmailResponse {
    private boolean sent;
    private int expiresInMinutes;
}