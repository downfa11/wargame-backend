package com.ns.membership.adapter.axon.command;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@Builder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ModifyMemberCommand extends SelfValidating<ModifyMemberCommand> {

    @NotNull
    @TargetAggregateIdentifier
    private String aggregateIdentifier;

    @NotNull
    private Long membershipId;
    @NotNull
    private String account;
    @NotNull
    private String name;
    @NotNull
    private String email;
    @NotNull
    private String password;

    public ModifyMemberCommand(String aggregateIdentifier, Long membershipId, String account, String name, String email, String password) {
        this.aggregateIdentifier = aggregateIdentifier;
        this.membershipId = membershipId;
        this.account = account;
        this.name = name;
        this.email = email;
        this.password = password;

        this.validateSelf();
    }
}
