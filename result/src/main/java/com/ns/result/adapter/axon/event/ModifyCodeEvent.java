package com.ns.result.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class ModifyCodeEvent extends SelfValidating<ModifyCodeEvent> {
    private String aggregateIdentifier;
    private String membershipId;
    private String code;

    public ModifyCodeEvent(@NotNull String aggregateIdentifier,
                           @NotNull String membershipId,
                           @NotNull String code) {
        this.aggregateIdentifier=aggregateIdentifier;
        this.membershipId = membershipId;
        this.code=code;
        this.validateSelf();
    }
}
