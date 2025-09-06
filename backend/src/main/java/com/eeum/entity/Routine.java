package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routine")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Routine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Integer routineId;

    @Column(name = "name")
    private String name;

    @Column(name = "trigger_type")
    private Boolean triggerType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "routine_weekday")
    private Integer routineWeekday;

    @Column(name = "routine_description")
    private String routineDescription;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "act_time")
    private OffsetDateTime actTime;

    @OneToMany(mappedBy = "routine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutineDetail> details = new ArrayList<>();
    
    public void setDetails(List<RoutineDetail> newDetails) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.clear();
        if (newDetails != null) {
            for (RoutineDetail d : newDetails) {
                d.setRoutine(this);
                this.details.add(d);
            }
        }
    }
}
