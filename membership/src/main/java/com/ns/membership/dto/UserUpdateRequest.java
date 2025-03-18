package com.ns.membership.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String account;

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
