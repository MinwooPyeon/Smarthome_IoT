package com.eeum.mqtt.common;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// IR 프로토콜의 시간 파라미터(us)
@Getter
@Setter
@NoArgsConstructor
public class Timing {
    private List<Integer> header;
    private List<Integer> one;   
    private List<Integer> zero; 
    private Integer gap;       
}
