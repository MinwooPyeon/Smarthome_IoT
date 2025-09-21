package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.JSON)   
    @Column(name = "device_detail", columnDefinition = "jsonb")
    private Map<String, Object> deviceDetail;
    
    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "ir_device_id", nullable = false)
	private String irDeviceId;
    
    @Column(name = "user_home_id", nullable = false)
    private Integer userHomeId;
}
