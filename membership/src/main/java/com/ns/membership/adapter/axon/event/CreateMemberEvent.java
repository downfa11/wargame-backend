package com.ns.membership.adapter.axon.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMemberEvent {

    private String account;
    private String name;
    private String email;
    private String password;
}

