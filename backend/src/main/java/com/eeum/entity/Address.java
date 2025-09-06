package com.eeum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter
@Setter
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Integer addressId;

    @Column(name = "detail")
    private String detail;

    // 군구 코드
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sgg_code", nullable = false)
    private Gungu gungu;

    // 시도 코드
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sido_code", nullable = false)
    private Sido sido;

    // 읍면동 코드
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emd_code", nullable = false)
    private EupMyeonDong eupMyeonDong;
}
