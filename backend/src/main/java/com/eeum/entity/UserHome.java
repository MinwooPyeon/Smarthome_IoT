package com.eeum.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_home")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserHome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_home_id")
    private Integer userHomeId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "home_id", nullable = false)
    private Integer homeId;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary;       
}
