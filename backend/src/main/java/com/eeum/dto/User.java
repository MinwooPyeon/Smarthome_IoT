package com.eeum.dto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "\"user\"", schema = "eeum")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "login_id")
    private String loginId;

    @Column(name = "password")
    private String password;

    @Column(name = "img")
    private String img;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "email")
    private String email;

    @Column(name = "join_date")
    private OffsetDateTime joinDate;

    @Column(name = "out")
    private OffsetDateTime out;

    @Column(name = "last_active")
    private OffsetDateTime lastActive;

    @Column(name = "trigger")
    private String trigger;

    @Column(name = "user_address")
    private String userAddress;

    @Column(name = "user_floorplans")
    private String userFloorplans;

    @Column(name = "floorplans_x")
    private Double floorplansX;

    @Column(name = "floorplans_y")
    private Double floorplansY;
}
