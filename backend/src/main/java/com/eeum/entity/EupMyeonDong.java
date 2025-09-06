package com.eeum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "eupmyeondong")
@Getter
@Setter
public class EupMyeonDong {

    @Id
    @Column(name = "emd_code")
    private Integer emdCode;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sgg_code", nullable = false)
    private Gungu gungu;
}
