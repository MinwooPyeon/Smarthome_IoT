package com.eeum.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
