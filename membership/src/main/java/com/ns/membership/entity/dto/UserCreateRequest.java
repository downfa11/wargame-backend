package com.ns.membership.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank
    private String account;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String email;

}
