package com.ns.membership.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
