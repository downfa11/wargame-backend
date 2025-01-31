package com.ns.membership.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordResetRequest {
    private String account;
    private String newPassword;
    private String verify;
}
