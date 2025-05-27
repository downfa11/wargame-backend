package com.ns.membership.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.AggregateIdentifier;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CreateMemberEvent  extends SelfValidating<CreateMemberEvent> {
    private String aggregateIdentifier;
    private Long membershipId;
    private String account;
    private String name;
    private String email;
    private String password;

    public CreateMemberEvent(@NotNull String aggregateIdentifier, @NotNull Long membershipId, @NotNull String account, @NotNull String name, @NotNull String email, @NotNull String password) {
        this.aggregateIdentifier = aggregateIdentifier;
        this.membershipId = membershipId;
        this.account = account;
        this.name = name;
        this.email = email;
        this.password = password;

        this.validateSelf();
    }
}

