package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "routine_icon", schema = "eeum")
public class RoutineIcon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "icon_id")
    private Integer iconId;

    @Column(name = "icon_url")
    private String iconUrl;
}
