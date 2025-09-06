package com.eeum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "floorplans", schema = "eeum")
@Getter
@Setter
public class Floorplan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "floorplan_id", nullable = false)
    private Integer floorplanId;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "square")
    private Double square;

    @Column(name = "floorplans_x")
    private Double floorplansX;

    @Column(name = "floorplans_y")
    private Double floorplansY;

    @Column(name = "home_id", nullable = false)
    private Integer homeId;
}
