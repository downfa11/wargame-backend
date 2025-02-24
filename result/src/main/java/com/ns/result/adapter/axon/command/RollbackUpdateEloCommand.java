package com.ns.result.adapter.axon.command;

import com.ns.common.utils.SelfValidating;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RollbackUpdateEloCommand extends SelfValidating<RollbackUpdateEloCommand> {

    private String rollbackId;

    @TargetAggregateIdentifier
    private String aggregateIdentifier;

    private String membershipId;
    private Long Elo;

    public RollbackUpdateEloCommand(String rollbackId, String aggregateIdentifier, String membershipId, Long Elo) {
        this.rollbackId = rollbackId;
        this.aggregateIdentifier = aggregateIdentifier;
        this.membershipId = membershipId;
        this.Elo = Elo;
        this.validateSelf();
    }
}
