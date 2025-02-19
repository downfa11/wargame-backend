package com.ns.result.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RollbackGameResultEvent extends SelfValidating<RollbackGameResultEvent> {
    private String rollbackId;
    private String aggregateIdentifier;

    public RollbackGameResultEvent(@NotNull String rollbackId, @NotNull String aggregateIdentifier) {
        this.rollbackId = rollbackId;
        this.aggregateIdentifier = aggregateIdentifier;
        this.validateSelf();
    }
}
