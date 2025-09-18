package com.eeum.entity;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "hub_device", schema = "eeum")
public class HubDevice {
    @Id
    @Column(name = "hub_device_id")
    private Integer hubDeviceId;

    @Column(name = "device_addr", columnDefinition = "inet", nullable = false)
    @JsonAlias("device_addr")
    private String deviceAddr;
    
    @Column(name = "user_home_id")
    private Integer userHomeId;
}