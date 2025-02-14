package com.ns.result.adapter.axon.command;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class UpdateEloCommand extends SelfValidating<UpdateEloCommand> {

    @NotNull
    @TargetAggregateIdentifier
    private String aggregateIdentifier;
    @NotNull
    private String membershipId;
    @NotNull
    private Long elo;


    public UpdateEloCommand(@NotNull String aggregateIdentifier,
                            @NotNull String membershipId,
                            @NotNull Long elo) {
        this.membershipId = membershipId;
        this.aggregateIdentifier = aggregateIdentifier;
        this.elo = elo;

        this.validateSelf();
    }
}