package com.eeum.dto.response;

import com.eeum.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {
 private Integer userId;
 private String nickname;
 private String email;
 private String img;

 public static UserResponse from(User user) {
     return new UserResponse(user.getUserId(), user.getNickname(),
             user.getEmail(), user.getImg());
 }
}

