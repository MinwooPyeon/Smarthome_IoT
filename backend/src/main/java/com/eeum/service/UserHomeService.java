package com.eeum.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.response.RoomItemResponse;
import com.eeum.dto.response.UserHomeItemResponse;
import com.eeum.repository.UserHomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserHomeService {
    
    private final UserHomeRepository userHomeRepository;

    // 유저의 집 목록 조회
    public List<UserHomeItemResponse> listUserHomes(Integer userId) {
        List<UserHomeRepository.HomeIdNameProjection> rows = userHomeRepository.findHomeIdAndNameByUserId(userId);
        
        return rows.stream()
            .map(r -> new UserHomeItemResponse(r.getHomeId(), r.getHomeName()))
            .toList();
    }
    
    // 유저 대표 집 주소 변경
    @Transactional
    public Integer changePrimaryHome(Integer userId, Integer homeId) {
        userHomeRepository.findByUserIdAndHomeId(userId, homeId)
            .orElseThrow(() -> new IllegalArgumentException("해당 집이 사용자 소유가 아닙니다."));

        userHomeRepository.resetPrimaryByUserId(userId);
        userHomeRepository.setPrimary(userId, homeId);

        return homeId;
    }
    
    
    // 유저 집의 방 목록 조회
    @Transactional
    public List<RoomItemResponse> listByHomeId(Integer userId, Integer homeId) {
        if (userId == null || homeId == null) {
            throw new IllegalArgumentException("userId, homeId는 필수입니다.");
        }
        if (!userHomeRepository.existsByUserIdAndHomeId(userId, homeId)) {
            throw new IllegalArgumentException("해당 집에 대한 접근 권한이 없습니다.");
        }

        List<UserHomeRepository.RoomRow> rows = userHomeRepository.findRoomsByHomeId(homeId);

        return rows.stream()
            .map(r -> new RoomItemResponse(
                    r.getRoomId(),
                    r.getRoomName(),
                    toHex(r.getRoomColor())
            ))
            .toList();
    }

    private static String toHex(Integer rgb) {
        if (rgb == null) return null;
        return String.format("#%06X", (rgb & 0xFFFFFF));
    }
}
