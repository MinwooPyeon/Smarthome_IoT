package com.eeum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "device", schema = "eeum")
@Getter
@Setter
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Integer deviceId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(name = "remote_id", nullable = false)
    private Integer remoteId;

    @Column(name = "ir_device_id", nullable = false)
    private Integer irDeviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "registered_at", columnDefinition = "timestamptz")
    private OffsetDateTime registeredAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_detail", columnDefinition = "json")
    private String deviceDetail;
}
