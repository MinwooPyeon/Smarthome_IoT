package com.eeum.service;

import com.eeum.dto.response.FloorplanItemResponse;
import com.eeum.dto.response.FloorplanListResponse;
import com.eeum.entity.Floorplan;
import com.eeum.entity.UserHome;
import com.eeum.repository.FloorplanRepository;
import com.eeum.repository.UserHomeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FloorplanService {

    private final FloorplanRepository floorplanRepository;
    private final UserHomeRepository userHomeRepository;
    
    // 집의 평면도 목록
    public FloorplanListResponse listByAddressId(Integer userId, Integer addressId) {
        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");

        if (addressId != null && !userHomeRepository.existsByUserIdAndAddressId(userId, addressId)) {
            throw new IllegalArgumentException("해당 집에 대한 권한이 없습니다.");
        }

        List<Floorplan> list = floorplanRepository.findAllByUserIdAndAddressId(userId, addressId);

        List<FloorplanItemResponse> items = list.stream()
                .map(this::toItem)
                .toList();

        return new FloorplanListResponse(items);
    }
    
    
	  // 평면도 등록
	  @Transactional
	  public Integer create(Integer userId, Integer homeId) {
	      if (userId == null || homeId == null) {
	          throw new IllegalArgumentException("userId, homeId는 필수입니다.");
	      }
	      
	      if (userHomeRepository.existsByUserIdAndHomeId(userId, homeId)) {
	          return homeId;
	      }
	
	      UserHome userHome = UserHome.builder()
	    		  .userId(userId)
	    		  .homeId(homeId)
	    		  .build();
	      
	      userHomeRepository.save(userHome);
	      return homeId;
	  }
	  
	    @Transactional
	    public void deleteUserHomeByHome(Integer userId, Integer homeId) {
	        if (userId == null || homeId == null) {
	            throw new IllegalArgumentException("userId, homeId는 필수입니다.");
	        }
	        
	        if (!userHomeRepository.existsByUserIdAndHomeId(userId, homeId)) {
	            return;
	        }
	        
	        userHomeRepository.deleteByUserIdAndHomeId(userId, homeId);
	    }
	  
	    
	    // 특정 homeid로 평면도 조회
	    @Transactional(readOnly = true)
	    public FloorplanListResponse listByUserAndHome(Integer userId, Integer homeId) {
	        if (userId == null || homeId == null) {
	            throw new IllegalArgumentException("userId, homeId는 필수입니다.");
	        }
	        if (!userHomeRepository.existsByUserIdAndHomeId(userId, homeId)) {
	            throw new IllegalArgumentException("해당 집에 대한 접근 권한이 없습니다.");
	        }

	        List<Floorplan> list = floorplanRepository.findAllByHomeId(homeId);
	        List<FloorplanItemResponse> items = list.stream().map(this::toItem).collect(Collectors.toList());
	        return new FloorplanListResponse(items);
	    }
	    
	    // user가 등록한 집들의 평면도 목록 조회
//	    @Transactional(readOnly = true)
//	    public FloorplanListResponse listByUserHomes(Integer userId) {
//	        if (userId == null) throw new IllegalArgumentException("userId는 필수입니다.");
//
//	        List<Floorplan> list = floorplanRepository.findAllByUserHomes(userId);
//
//	        List<FloorplanItemResponse> items = list.stream()
//	                .map(this::toItem)
//	                .collect(Collectors.toList());
//
//	        return new FloorplanListResponse(items);
//	    }
	  

    private FloorplanItemResponse toItem(Floorplan f) {
        return new FloorplanItemResponse(
                f.getFloorplanId(),
                f.getImageUrl(),
                f.getCreatedAt().toInstant(),
                f.getSquare(),
                f.getFloorplansX(),
                f.getFloorplansY(),
                f.getHomeId()
        );
    }
}


