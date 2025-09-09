package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "device", schema = "eeum")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "registered_at")
    private OffsetDateTime registeredAt;

    @Column(name = "device_detail", columnDefinition = "json")
    private String deviceDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remote_id", nullable = false)
    private IrRemoteir remote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ir_device_id", nullable = false)
    private IrDevice irDevice;
}
