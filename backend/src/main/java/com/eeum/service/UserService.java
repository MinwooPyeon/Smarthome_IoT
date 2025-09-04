package com.eeum.service;

import org.springframework.security.crypto.password.PasswordEncoder;
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


    // 비밀번호 변경
    public void updatePassword(Integer userId, String newPassword) {
 
        validateNewPassword(newPassword);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호가 기존 비밀번호와 동일합니다.");
        }

        String encoded = passwordEncoder.encode(newPassword);
        user.setPassword(encoded);
    }

    private void validateNewPassword(String pw) {
        if (pw == null || pw.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호는 비어 있을 수 없습니다.");
        }
        if (pw.length() < 8 || pw.length() > 64) {
            throw new IllegalArgumentException("새 비밀번호는 8~64자여야 합니다.");
        }
        if (pw.contains(" ")) {
            throw new IllegalArgumentException("비밀번호에는 공백을 사용할 수 없습니다.");
        }
        // 영문/숫자 각 1개 이상
        boolean hasLetter = pw.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = pw.chars().anyMatch(Character::isDigit);
        if (!(hasLetter && hasDigit)) {
            throw new IllegalArgumentException("비밀번호는 영문자와 숫자를 각각 1자 이상 포함해야 합니다.");
        }
    }
}
