package com.eeum.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "room")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @Column(name = "room_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roomId;

    @Column(name = "room_name")
    private String roomName;
    
    @Column(name = "room_color")
    private Integer roomColor;

    @Column(name = "floorplan_id", nullable = false)
    private Integer floorplanId;
}
