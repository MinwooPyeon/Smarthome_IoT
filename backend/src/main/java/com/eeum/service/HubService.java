package com.eeum.service;

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
}
