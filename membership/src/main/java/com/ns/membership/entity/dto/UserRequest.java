package com.ns.membership.entity.dto;

import com.ns.wargame.Utils.Numberic;
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
