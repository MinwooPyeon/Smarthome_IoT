package com.eeum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(schema = "eeum", name = "ir_device")
public class IrDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ir_device_id", nullable = false)
    private Integer irDeviceId;

    @Column(name = "device_addr")
    private Integer deviceAddr;
    
    @Column(name = "hub_device_id", nullable = false)
    private Integer hubDevice;
}
