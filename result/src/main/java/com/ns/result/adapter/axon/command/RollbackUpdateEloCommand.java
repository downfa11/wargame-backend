package com.ns.result.adapter.axon.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RollbackUpdateEloCommand {

    private String rollbackId;

    @TargetAggregateIdentifier
    private String aggregateIdentifier;

    private String membershipId;
    private Long Elo;
}
