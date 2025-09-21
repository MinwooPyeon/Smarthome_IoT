package com.eeum.entity;

import java.time.OffsetDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_routine", schema = "eeum")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiRoutine {
    @Id
    @Column(name = "routine_id")
    private Integer routineId;

    private String name;
    private Integer routineWeekday;
    private String routineDescription;
    private OffsetDateTime actTime;
    private Integer iconId;
}

