package com.ns.wargame.Domain.dto;

import com.ns.wargame.Config.Numberic;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRequest {

    @NotBlank
    @Numberic
    private String email;

    @NotBlank
    @Numberic
    private String password;

}
