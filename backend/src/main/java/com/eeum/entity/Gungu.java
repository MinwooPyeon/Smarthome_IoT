package com.eeum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gungu")
@Getter
@Setter
public class Gungu {

    @Id
    @Column(name = "sgg_code")
    private Integer sggCode;

    @Column(name = "name")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sido_code", nullable = false)
    private Sido sido;

    @OneToMany(mappedBy = "gungu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EupMyeonDong> eupMyeonDongs = new ArrayList<>();
}
