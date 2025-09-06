package com.eeum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sido")
@Getter
@Setter
public class Sido {

    @Id
    @Column(name = "sido_code")
    private Integer sidoCode;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "sido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Gungu> gungus = new ArrayList<>();
}
