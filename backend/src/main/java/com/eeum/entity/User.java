package com.eeum.entity;

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
    @Column(name = "user_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

}
