package com.ns.wargame.Domain.dto;

import com.ns.wargame.Domain.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String password;
    private Long elo;
    private String name;
    private String email;
    private String curGameSpaceCode;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    public static UserResponse of(User user){
        return UserResponse.builder()
                .id(user.getId())
                .password(user.getPassword())
                .name(user.getName())
                .email(user.getEmail())
                .elo(user.getElo())
                .createAt(user.getCreatedAt())
                .updateAt(user.getUpdatedAt())
                .build();
    }
}
