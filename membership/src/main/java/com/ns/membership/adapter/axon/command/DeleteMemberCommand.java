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
public class DeleteMemberCommand extends SelfValidating<DeleteMemberCommand> {

    @NotNull
    @TargetAggregateIdentifier
    private String aggregateIdentifier;

    @NotNull
    private Long membershipId;

    public DeleteMemberCommand(String aggregateIdentifier, Long membershipId) {
        this.aggregateIdentifier = aggregateIdentifier;
        this.membershipId = membershipId;
        this.validateSelf();
    }
}
