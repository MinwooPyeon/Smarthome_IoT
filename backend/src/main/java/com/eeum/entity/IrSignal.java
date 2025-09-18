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

@Entity
@Table(name = "ir_signal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IrSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer signalId;

    private String name;

    @Column(name = "samples_us", columnDefinition = "integer[]")
    private int[] samplesUs;

    private String model;

    private Integer protocolId;  
}