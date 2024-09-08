package com.ns.membership.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordResetRequest {
    private String membershipId;
    private String newPassword;
    private String verify;
}
