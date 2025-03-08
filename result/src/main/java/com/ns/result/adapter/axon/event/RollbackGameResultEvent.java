package com.ns.result.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class RollbackGameResultEvent extends SelfValidating<RollbackGameResultEvent> {
    private String spaceId;

    public RollbackGameResultEvent(@NotNull String spaceId) {
        this.spaceId = spaceId;
        this.validateSelf();
    }
}
