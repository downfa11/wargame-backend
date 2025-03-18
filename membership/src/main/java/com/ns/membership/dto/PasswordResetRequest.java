package com.ns.membership.dto;

import lombok.Getter;

@Getter
public class PasswordResetRequest {
    private String account;
    private String newPassword;
    private String verify;
}
