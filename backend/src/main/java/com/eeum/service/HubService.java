package com.eeum.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.request.HubRegisterRequest;
import com.eeum.dto.response.HubRegisterResponse;
import com.eeum.entity.UserHome;
import com.eeum.repository.HubDeviceRepository;
import com.eeum.repository.UserHomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HubService {

    private final HubDeviceRepository hubDeviceRepo;
    private final UserHomeRepository userHomeRepo;
    
    // 허브 등록
    @Transactional
    public HubRegisterResponse registerHub(Integer userId, HubRegisterRequest req) {
    	    	
        if (req.getHubDeviceId() == null || req.getHubDeviceId().isBlank() || req.getHomeId() == null) {
            throw new IllegalArgumentException("hubDeviceId, homeId는 필수입니다.");
        }
        
        Integer userHomeId = userHomeRepo.findByUserIdAndHomeId(userId, req.getHomeId())
                .map(UserHome::getUserHomeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 집에 대한 권한이 없습니다."));

        int updated = hubDeviceRepo.bindHubBySerial(req.getHubDeviceId(), userHomeId);
        if (updated == 0) {
            throw new IllegalArgumentException("허브를 찾을 수 없거나 이미 등록되었습니다.");
        }


        return new HubRegisterResponse(userHomeId);
    }
    
    // 허브 목록 조회
    @Transactional(readOnly = true)
    public List<String> listHubs(Integer userId, Integer homeId) {
        if (userId == null || homeId == null) {
            throw new IllegalArgumentException("userId, homeId는 필수입니다.");
        }
        if (!userHomeRepo.existsByUserIdAndHomeId(userId, homeId)) {
            throw new IllegalArgumentException("해당 집에 대한 권한이 없습니다.");
        }
        return hubDeviceRepo.findHubIdsByUserAndHome(userId, homeId);
    }
    
    // 허브 수정
    @Transactional
    public HubRegisterResponse reassignHub(Integer userId, HubRegisterRequest req) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
        if (req == null || req.getHubDeviceId() == null || req.getHubDeviceId().isBlank())
            throw new IllegalArgumentException("hubDeviceId는 필수입니다.");
        if (req.getHomeId() == null)
            throw new IllegalArgumentException("homeId는 필수입니다.");
        
        // 대상 homeId 권한 확인
        Integer targetUserHomeId = userHomeRepo.findByUserIdAndHomeId(userId, req.getHomeId())
                .map(UserHome::getUserHomeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 집에 대한 권한이 없습니다."));

        // 허브 존재 확인
        if (!hubDeviceRepo.existsById(req.getHubDeviceId())) {
            throw new IllegalArgumentException("허브를 찾을 수 없습니다: " + req.getHubDeviceId());
        }
        
        Integer currentUserHomeId = hubDeviceRepo.findBoundUserHomeIdOrNull(req.getHubDeviceId());

        if (currentUserHomeId != null) {
            boolean ownedByMe = userHomeRepo.existsByUserIdAndHomeId(currentUserHomeId, userId);
            if (!ownedByMe) throw new IllegalArgumentException("다른 사용자의 허브는 이동할 수 없습니다.");
            if (currentUserHomeId.equals(targetUserHomeId)) {
                return new HubRegisterResponse(targetUserHomeId);
            }
        }
        
        int updated = hubDeviceRepo.swapBind(req.getHubDeviceId(), targetUserHomeId, currentUserHomeId);

        if (updated == 0) {
            Integer now = hubDeviceRepo.findBoundUserHomeIdOrNull(req.getHubDeviceId());
            if (now != null && now.equals(targetUserHomeId)) {
                return new HubRegisterResponse(targetUserHomeId);
            }
            throw new IllegalStateException("허브 재할당에 실패했습니다. hubDeviceId=" + req.getHubDeviceId());
        }
		return new HubRegisterResponse(targetUserHomeId);
    }
}
