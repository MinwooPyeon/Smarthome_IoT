package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "ir_remoteir", schema = "eeum")
public class IrRemoteir {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "remote_id")
    private Integer remoteId;

    private String brand;
    private String model;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "power consumption")
    private Float powerConsumption;
}