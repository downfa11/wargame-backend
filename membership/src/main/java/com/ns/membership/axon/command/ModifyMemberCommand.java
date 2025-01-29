package com.ns.membership.axon.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@Builder
@AllArgsConstructor
public class ModifyMemberCommand {

    @NotNull
    @TargetAggregateIdentifier
    private String aggregateIdentifier;
    @NotNull
    private String membershipId;
    @NotNull
    private String account;
    @NotNull
    private String name;
    @NotNull
    private String email;
    @NotNull
    private String password;

    //todo. code, refreshToken 변동도 이벤트로 처리하나?
}
