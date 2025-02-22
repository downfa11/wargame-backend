package com.ns.result.adapter.axon.command;

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
public class CreatePlayerCommand extends SelfValidating<CreatePlayerCommand> {

    @NotNull
    private String membershipId;

    public CreatePlayerCommand(@NotNull String membershipId) {
        this.membershipId = membershipId;
        this.validateSelf();
    }

}
