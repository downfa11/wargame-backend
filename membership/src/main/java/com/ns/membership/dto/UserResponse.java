package com.ns.membership.dto;

import com.ns.membership.adapter.out.persistence.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String account;
    private String password;
    private String name;
    private String email;
    private String jwtToken;
    private String refreshToken;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    public static UserResponse of(User user){
        return UserResponse.builder()
                .id(user.getId())
                .account(user.getAccount())
                .password(user.getPassword())
                .name(user.getName())
                .email(user.getEmail())
                .refreshToken(user.getRefreshToken())
                .createAt(user.getCreatedAt())
                .updateAt(user.getUpdatedAt())
                .build();
    }
}
