package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "ir_device", schema = "eeum")
public class IrDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ir_device_id")
    private Integer irDeviceId;

    @Column(name = "device_addr")
    private Integer deviceAddr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_device_id", nullable = false)
    private HubDevice hubDevice;
}
