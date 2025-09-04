package com.eeum.service;

import org.springframework.stereotype.Service;

import com.eeum.dto.User;
import com.eeum.dto.response.UpdateNicknameResponse;
import com.eeum.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UpdateNicknameResponse updateNickname(Integer userId, String newNickname) {

        validateNickname(newNickname);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (user.getNickname().equalsIgnoreCase(newNickname)) {
            throw new IllegalArgumentException("현재 닉네임과 동일합니다.");
        }

        user.setNickname(newNickname);

        return UpdateNicknameResponse.builder()
                .userId(user.getUserId())
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
}
