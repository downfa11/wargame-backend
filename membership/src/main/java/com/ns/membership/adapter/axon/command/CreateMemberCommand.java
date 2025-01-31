package com.ns.membership.adapter.axon.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMemberCommand {
    private String account;
    private String name;
    private String email;
    private String password;

}
