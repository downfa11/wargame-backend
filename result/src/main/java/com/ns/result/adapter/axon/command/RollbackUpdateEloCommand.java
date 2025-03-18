package com.ns.result.adapter.axon.command;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RollbackUpdateEloCommand extends SelfValidating<RollbackUpdateEloCommand> {

    private String spaceId;

    @TargetAggregateIdentifier
    private String aggregateIdentifier;

    private String membershipId;
    private Long elo;

    public RollbackUpdateEloCommand(@NotNull String spaceId, @NotNull String aggregateIdentifier, @NotNull String membershipId, @NotNull Long elo) {
        this.spaceId = spaceId;
        this.aggregateIdentifier = aggregateIdentifier;
        this.membershipId = membershipId;
        this.elo = elo;
        this.validateSelf();
    }
}
