package com.ns.result.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CreatePlayerEvent extends SelfValidating<CreatePlayerEvent> {
    @NotNull private String id;
    @NotNull private String membershipId;

    public CreatePlayerEvent(@NotNull String id, @NotNull String membershipId) {
        this.id = id;
        this.membershipId = membershipId;
        this.validateSelf();
    }
}
