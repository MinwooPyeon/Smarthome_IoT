package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "routine_detail")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class RoutineDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_detail")
    private Integer routineDetailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_detail", columnDefinition = "jsonb")
    private String deviceDetail;
}
