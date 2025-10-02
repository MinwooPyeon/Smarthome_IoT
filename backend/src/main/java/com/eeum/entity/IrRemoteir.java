package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "ir_remoteir", schema = "eeum")
public class IrRemoteir {
    @Id
    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "brand", nullable = false)
    private String brand;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "power_consumption")
    private Float powerConsumption;
}