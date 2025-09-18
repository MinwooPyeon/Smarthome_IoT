package com.eeum.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

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
@Table(schema = "eeum", name = "ir_event_log")
public class IrEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;                     

    @Column(name = "event_time")
    private OffsetDateTime eventTime;         

    @Column(name = "kind")
    private String kind;                      

    @Column(name = "ir_device_id", nullable = false)
    private String irDeviceId;                

    @Column(name = "tx_id", nullable = false)
    private UUID txId;                        

    @Column(name = "model")
    private String model;                     
}
