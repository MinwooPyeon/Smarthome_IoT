package com.eeum.service;

import com.eeum.dto.request.FloorplanRequest;
import com.eeum.dto.response.FloorplanResponse;
import com.eeum.entity.Floorplan;
import com.eeum.entity.Home;
import com.eeum.entity.UserHome;
import com.eeum.repository.FloorplanRepository;
import com.eeum.repository.HomeRepository;
import com.eeum.repository.UserHomeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FloorplanService {

    private final FloorplanRepository floorplanRepository;
    private final UserHomeRepository userHomeRepository;

    // 평면도 등록
    @Transactional
    public void selectMyFloorplan(Integer userId, Integer homeId, Integer floorplanId) {
        if (userId == null || homeId == null || floorplanId == null) {
            throw new IllegalArgumentException("userId, homeId, floorplanId는 필수입니다.");
        }
        
        UserHome userhome = userHomeRepository.findByUserIdAndHomeId(userId, homeId);

        userhome.setFloorplanId(floorplanId);
    }

    
    // 특정 집의 평면도 목록
    @Transactional(readOnly = true)
    public List<Floorplan> findByHome(Integer homeId) {
        if (homeId == null) {
            throw new IllegalArgumentException("homeId는 필수입니다.");
        }
        return floorplanRepository.findByHomeId(homeId);
    }
    
    // 사용자의 평면도 조회
    @Transactional(readOnly = true)
    public List<FloorplanResponse> getUserFloorplan(Integer userId, Integer homeId) {
        if (userId == null || homeId == null) {
            throw new IllegalArgumentException("userId, homeId는 필수입니다.");
        }
        UserHome userhome = userHomeRepository.findByUserIdAndHomeId(userId, homeId);
        
        Integer floorplanId = userhome.getFloorplanId();
        if (floorplanId == null)  return List.of();

        Floorplan fp = floorplanRepository.findById(floorplanId)
                .orElseThrow(() -> new IllegalArgumentException("평면도가 존재하지 않습니다. floorplanId=" + floorplanId));

        return List.of(new FloorplanResponse(fp.getFloorplanId(), fp.getImageUrl()));
    }
}


