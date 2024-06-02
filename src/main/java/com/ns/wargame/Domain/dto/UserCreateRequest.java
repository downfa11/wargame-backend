package com.ns.wargame.Domain.dto;

import com.ns.wargame.Utils.Numberic;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreateRequest {

    @NotBlank
    @Numberic
    private String id;

    @NotBlank
    @Numberic
    private String password;

    @NotBlank
    @Numberic
    private String name;

    @NotBlank
    @Numberic
    private String email;

}
