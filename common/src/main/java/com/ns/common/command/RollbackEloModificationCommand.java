package com.ns.common.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RollbackEloModificationCommand {

    @TargetAggregateIdentifier
    private String aggregateIdentifier;

    private Long previousElo;
}
