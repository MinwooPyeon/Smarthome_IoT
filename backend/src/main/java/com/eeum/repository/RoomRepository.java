package com.eeum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.eeum.entity.Room;

public interface RoomRepository extends JpaRepository<Room, Integer> {

	// 방 색깔로 방id 조회
    @Query(value = """
        SELECT r.*
        FROM eeum.room r
        JOIN eeum.floorplans f ON f.floorplan_id = r.floorplan_id
        WHERE f.home_id = :homeId
          AND r.room_color = :roomColor
        LIMIT 1
    """, nativeQuery = true)
    Optional<Room> findByHomeIdAndRoomColor(@Param("homeId") Integer homeId,
                                            @Param("roomColor") Integer roomColor);
}
