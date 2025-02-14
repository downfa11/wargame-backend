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
public class CreatePlayerEvent extends SelfValidating<CreatePlayerEvent> {
    @NotNull
    private String membershipId;

    public CreatePlayerEvent(@NotNull String membershipId) {
        this.membershipId = membershipId;
        this.validateSelf();
    }
}
