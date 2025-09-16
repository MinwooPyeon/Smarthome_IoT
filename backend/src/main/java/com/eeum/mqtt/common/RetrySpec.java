package com.eeum.mqtt.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// order 메시지에서 사용하는 재시도 설정
@Getter
@Setter
@NoArgsConstructor
public class RetrySpec {
    private Integer max;	   // 최대 재시도 횟수
    private Integer backoffMs; // 재시도 간격 (밀리초)
}