package com.eeum.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;  

import java.util.List;
import java.util.Optional;

import com.eeum.entity.UserHome;

public interface UserHomeRepository extends JpaRepository<UserHome, Integer> {

	// userId가 해당 addressId의 집에 접근 권한이 있는지 확인
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                  FROM eeum.user_home uh
                  JOIN eeum.home h ON h.home_id = uh.home_id
                 WHERE uh.user_id = :userId
                   AND h.address_id = :addressId
            )
            """, nativeQuery = true)
        boolean existsByUserIdAndAddressId(@Param("userId") Integer userId,
                                           @Param("addressId") Integer addressId);

    
    // userId가 homeId에 속해 있는지 여부 확인
    boolean existsByUserIdAndHomeId(Integer userId, Integer homeId);

    
    // user_home에서 userId와 homeId 매핑 삭제
    void deleteByUserIdAndHomeId(Integer userId, Integer homeId);
    
    
    // 유저의 집 목록 조회
    @Query(value = """
    	    SELECT h.home_id   AS homeId,
    	           a.home_name AS homeName,
    	           (
    	             SELECT f.square
    	               FROM eeum.floorplans f
    	              WHERE f.home_id = h.home_id
    	           )           AS square
    	      FROM eeum.user_home uh
    	      JOIN eeum.home       h ON uh.home_id   = h.home_id
    	      JOIN eeum.addresses  a ON h.address_id = a.address_id
    	     WHERE uh.user_id = :userId
    	     ORDER BY h.home_id
    	    """, nativeQuery = true)
    	List<HomeIdNameSquareProjection> findHomeIdNameAndSquareByUserId(@Param("userId") Integer userId);

    	interface HomeIdNameSquareProjection {
    	    Integer getHomeId();
    	    String  getHomeName();
    	    Double  getSquare();
    	}

        // userId와 homeId로 user_home 엔티티 조회
        Optional<UserHome> findByUserIdAndHomeId(Integer userId, Integer homeId);

        
        // 해당 유저의 기존 대표 집(isPrimary=true) 해제
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
            UPDATE UserHome u SET u.isPrimary = false
             WHERE u.userId = :userId AND u.isPrimary = true
        """)
        int resetPrimaryByUserId(@Param("userId") Integer userId);

        
        // 특정 집을 대표 집으로 설정(isPrimary=true)
        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
            UPDATE UserHome u SET u.isPrimary = true
             WHERE u.userId = :userId AND u.homeId = :homeId
        """)
        int setPrimary(@Param("userId") Integer userId, @Param("homeId") Integer homeId);
        
        
        // 방 목록 조회
        @Query(
                value = """
                    SELECT 
                        r.room_id   AS roomId,
                        r.room_name AS roomName,
                        r.room_color AS roomColor
                    FROM eeum.room r
                    JOIN eeum.floorplans f ON r.floorplan_id = f.floorplan_id
                    WHERE f.home_id = :homeId
                    ORDER BY r.room_id
                """,
                nativeQuery = true)
            List<RoomRow> findRoomsByHomeId(@Param("homeId") Integer homeId);
        
        
        interface RoomRow {
            Integer getRoomId();
            String  getRoomName();
            Integer getRoomColor();
        }
        
        // 유저의 대표집 조회
        Optional<UserHome> findByUserIdAndIsPrimaryTrue(Integer userId);
        
        @Query("""
                select uh.homeId
                from UserHome uh
                where uh.userId = :userId
                  and uh.isPrimary = true
                """)
            Optional<Integer> findIsPrimaryHomeId(@Param("userId") Integer userId);
        
        
        @Query("""
                SELECT uh.userHomeId
                  FROM UserHome uh
                 WHERE uh.userId = :userId
                   AND uh.homeId = :homeId
                """)
            Optional<Integer> findUserHomeId(@Param("userId") Integer userId,
                                             @Param("homeId") Integer homeId);
        
    }