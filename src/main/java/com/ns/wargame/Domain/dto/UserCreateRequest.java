package com.ns.wargame.Domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreateRequest {
    private String id;
    private String password;
    private String name;
    private String email;

}
