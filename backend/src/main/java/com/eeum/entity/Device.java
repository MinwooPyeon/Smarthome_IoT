package com.eeum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

@Entity
@Table(name = "device", schema = "eeum")
@Getter
@Setter
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "home_id", nullable = false)
    private Integer homeId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "registered_at", columnDefinition = "timestamptz")
    private OffsetDateTime registeredAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_detail", columnDefinition = "json")
    private String deviceDetail;
    
//    @Column(name = "room_id", nullable = false)
//    private Integer roomId;
}
