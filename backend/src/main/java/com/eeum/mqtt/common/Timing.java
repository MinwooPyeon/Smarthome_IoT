package com.eeum.mqtt.common;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// IR 프로토콜의 시간 파라미터(us)
@Getter
@Setter
@NoArgsConstructor
public class Timing {
	@NotNull @Size(min = 2, max = 2) private List<@Min(0) Integer> header;
	@NotNull @Size(min = 2, max = 2) private List<@Min(0) Integer> one;
	@NotNull @Size(min = 2, max = 2) private List<@Min(0) Integer> zero;
	@Min(0) private Integer gap;     
}
