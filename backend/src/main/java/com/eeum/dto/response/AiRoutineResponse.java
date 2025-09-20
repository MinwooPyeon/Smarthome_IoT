package com.eeum.dto.response;

import java.time.OffsetDateTime;
import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AiRoutineResponse {
	
    private Integer routineId;
    private String name;
    private Integer routineWeekday;
    private String routineDescription;
    private OffsetDateTime actTime;
    private Integer iconId;
}
