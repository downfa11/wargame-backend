package com.ns.result.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RollbackUpdateEloEvent extends SelfValidating<RollbackUpdateEloEvent> {
    private String aggregateIdentifier;
    private String membershipId;
    private Long elo;

    public RollbackUpdateEloEvent(@NotNull String aggregateIdentifier,
                             @NotNull String membershipId,
                             @NotNull Long elo) {
        this.aggregateIdentifier=aggregateIdentifier;
        this.membershipId = membershipId;
        this.elo=elo;
        this.validateSelf();
    }
}
