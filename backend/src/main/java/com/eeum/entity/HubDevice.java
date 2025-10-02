package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "hub_device", schema = "eeum")
public class HubDevice {
    @Id
    @Column(name = "hub_device_id")
    private String hubDeviceId;
    
    @Column(name = "user_home_id")
    private Integer userHomeId;
}