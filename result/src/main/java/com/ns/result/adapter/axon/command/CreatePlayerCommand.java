package com.ns.result.adapter.axon.command;

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
public class CreatePlayerCommand extends SelfValidating<CreatePlayerCommand> {

    @NotNull
    private String membershipId;

    public CreatePlayerCommand(@NotNull String membershipId) {
        this.membershipId = membershipId;
        this.validateSelf();
    }

}
