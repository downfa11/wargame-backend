package com.ns.result.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class UpdateEloEvent extends SelfValidating<UpdateEloEvent> {
    private String aggregateIdentifier;
    private String membershipId;
    private Long elo;

    public UpdateEloEvent(@NotNull String aggregateIdentifier,
                          @NotNull String membershipId,
                          @NotNull Long elo) {
        this.aggregateIdentifier=aggregateIdentifier;
        this.membershipId = membershipId;
        this.elo=elo;
        this.validateSelf();
    }
}
