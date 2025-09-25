package com.eeum.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eeum.dto.response.RoomItemResponse;
import com.eeum.dto.response.UserHomeItemResponse;
import com.eeum.entity.Address;
import com.eeum.entity.Home;
import com.eeum.entity.UserHome;
import com.eeum.repository.AddressRepository;
import com.eeum.repository.FloorplanRepository;
import com.eeum.repository.HomeRepository;
import com.eeum.repository.UserHomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserHomeService {
    
    private final UserHomeRepository userHomeRepository;
    private final HomeRepository homeRepository;
    private final AddressRepository addressRepository;
    private final FloorplanRepository floorplanRepository;

    // 유저의 집 목록 조회
    public List<UserHomeItemResponse> listUserHomes(Integer userId) {
        List<UserHomeRepository.HomeIdNameSquareProjection> rows = userHomeRepository.findHomeIdNameAndSquareByUserId(userId);
        
        return rows.stream()
        	    .map(r -> new UserHomeItemResponse(
        	        r.getHomeId(),
        	        r.getHomeName() + (r.getSquare() == null ? "" : " " +
        	            java.math.BigDecimal.valueOf(r.getSquare())
        	                .stripTrailingZeros()
        	                .toPlainString() + "평")
        	    ))
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
    
    // 유저 대표집 조회
    @Transactional(readOnly = true)
    public UserHomeItemResponse getPrimaryHome(Integer userId) {
        UserHome uh = userHomeRepository.findByUserIdAndIsPrimaryTrue(userId)
                .orElseThrow(() -> new IllegalArgumentException("대표 집이 설정되어 있지 않습니다."));

        Home home = homeRepository.findById(uh.getHomeId())
            .orElseThrow(() -> new IllegalArgumentException("homeId=" + uh.getHomeId() + " 집 정보를 찾을 수 없습니다."));

        Address addr = addressRepository.findById(home.getAddressId())
            .orElseThrow(() -> new IllegalArgumentException("addressId=" + home.getAddressId() + " 주소를 찾을 수 없습니다."));

        String squareText = "";
        Optional<Double> squareOpt = floorplanRepository.findSquareByHomeId(uh.getHomeId());
        if (squareOpt.isPresent()) {
            squareText = " " + java.math.BigDecimal.valueOf(squareOpt.get())
                    .stripTrailingZeros()
                    .toPlainString() + "평";
        }

        return UserHomeItemResponse.builder()
                .homeId(uh.getHomeId())
                .homeName(addr.getHomeName() + squareText)
                .build();
    }
    
    public Optional<Integer> getIsPrimaryHomeId(Integer userId) {
        return userHomeRepository.findIsPrimaryHomeId(userId);
    }

    private static String toHex(Integer rgb) {
        if (rgb == null) return null;
        return String.format("#%06X", (rgb & 0xFFFFFF));
    }
}
