package com.ns.common.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@Builder
@AllArgsConstructor
public class ModifyMemberEloCommand {

    @NotNull
    @TargetAggregateIdentifier
    private String aggregateIdentifier;
    @NotNull
    private String membershipId;
    @NotNull
    private Long elo;
}
