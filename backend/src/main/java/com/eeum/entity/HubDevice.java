package com.eeum.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLInetType;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "hub_device", schema = "eeum")
public class HubDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hub_device_id")
    private Integer hubDeviceId;

    @Type(PostgreSQLInetType.class)
    @Column(name = "device_addr", columnDefinition = "inet")
    private String deviceAddr;
}