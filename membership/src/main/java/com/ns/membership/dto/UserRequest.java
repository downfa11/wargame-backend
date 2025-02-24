package com.ns.membership.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserRequest {

    @NotBlank
    private String account;

    @NotBlank
    private String password;
}
