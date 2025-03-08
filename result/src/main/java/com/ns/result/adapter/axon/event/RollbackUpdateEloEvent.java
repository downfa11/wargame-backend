package com.ns.result.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RollbackUpdateEloEvent extends SelfValidating<RollbackUpdateEloEvent> {
    private String spaceId;
    private String aggregateIdentifier;
    private String membershipId;
    private Long oldElo;
    private Long curElo;

    public RollbackUpdateEloEvent(@NotNull String spaceId,
                                  @NotNull String aggregateIdentifier,
                             @NotNull String membershipId,
                             @NotNull Long oldElo, @NotNull Long curElo) {
        this.spaceId = spaceId;
        this.aggregateIdentifier=aggregateIdentifier;
        this.membershipId = membershipId;
        this.oldElo=oldElo;
        this.curElo=curElo;
        this.validateSelf();
    }
}
