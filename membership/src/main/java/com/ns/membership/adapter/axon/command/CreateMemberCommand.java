package com.ns.membership.adapter.axon.command;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;


@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CreateMemberCommand extends SelfValidating<CreateMemberCommand> {

    @TargetAggregateIdentifier // command용
    private String aggregateIdentifier;

    private String account;
    private String name;
    private String email;
    private String password;

    public CreateMemberCommand(@NotNull String aggregateIdentifier, @NotNull String account, @NotNull String name, @NotNull String email, @NotNull String password) {
        this.aggregateIdentifier = aggregateIdentifier;
        this.account = account;
        this.name = name;
        this.email = email;
        this.password = password;

        this.validateSelf();
    }
}
