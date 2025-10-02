package com.eeum.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "ir_tx_queue", schema = "eeum")
public class IrTxQueue {

    @Id
    @Column(name = "tx_id", nullable = false)
    private UUID txId;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "repeat_count")
    private Integer repeatCount;

    @Column(name = "interval_ms")
    private Integer intervalMs;

    @Column(name = "status")
    private String status;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "signal_id")
    private Integer signalId;

    @Column(name = "ir_device_id")
    private String irDeviceId;

    @Column(name = "model")
    private String model;
}
