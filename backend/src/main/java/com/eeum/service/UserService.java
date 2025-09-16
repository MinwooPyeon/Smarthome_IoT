package com.eeum.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.eeum.dto.request.PasswordUpdateRequest;
import com.eeum.dto.request.UserImageUpdateRequest;
import com.eeum.dto.response.UpdateNicknameResponse;
import com.eeum.dto.response.UserResponse;
import com.eeum.entity.User;
import com.eeum.repository.UserHomeRepository;
import com.eeum.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 닉네임 변경
    public UpdateNicknameResponse updateNickname(Integer userId, String newNickname) {

        validateNickname(newNickname);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (user.getNickname().equalsIgnoreCase(newNickname)) {
            throw new IllegalArgumentException("현재 닉네임과 동일합니다.");
        }

        user.setNickname(newNickname);

        return UpdateNicknameResponse.builder()
                .nickname(user.getNickname())
                .build();
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
        }

        // 허용 문자: 한글/영문/숫자/._-
        if (!nickname.matches("^[A-Za-z0-9가-힣._\\-]{2,20}$")) {
            throw new IllegalArgumentException("닉네임 형식이 올바르지 않습니다. (공백 불가)");
        }
    }
    
    
    // 유저 정보 조회
    public UserResponse getUserInfo(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }
    
    // 유저 이미지 변경
    @Transactional
    public void updateUserImage(Integer userId, UserImageUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setImg(req.getImgUrl());
    }
    
    // 비밀번호 변경
    public void updatePassword(Integer userId, PasswordUpdateRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        if (!req.getNewPassword().equals(req.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호가 기존 비밀번호와 동일합니다.");
        }
        
        String encoded = passwordEncoder.encode(req.getNewPassword());
        user.setPassword(encoded);
    }


    // 회원 탈퇴
    @Transactional
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
    }
}
