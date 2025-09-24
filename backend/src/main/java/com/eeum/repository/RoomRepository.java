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
               AND abs(((r.room_color >> 16) & 255) - :r) <= :tol
               AND abs(((r.room_color >>  8) & 255) - :g) <= :tol
               AND abs(( r.room_color        & 255) - :b) <= :tol
             ORDER BY
               abs(((r.room_color >> 16) & 255) - :r)
             + abs(((r.room_color >>  8) & 255) - :g)
             + abs(( r.room_color        & 255) - :b)
             ASC
             LIMIT 1
            """, nativeQuery = true)
        Optional<Room> findNearestByHomeIdAndRgbWithinTol(@Param("homeId") Integer homeId,
                                                          @Param("r") int r,
                                                          @Param("g") int g,
                                                          @Param("b") int b,
                                                          @Param("tol") int tol);
}
