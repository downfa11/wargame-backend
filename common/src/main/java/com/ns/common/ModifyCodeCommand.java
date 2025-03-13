package com.ns.common;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class ModifyCodeCommand extends SelfValidating<ModifyCodeCommand> {
    @NotNull
    @TargetAggregateIdentifier
    private String aggregateIdentifier;

    @NotNull private String membershipId;
    @NotNull private String code;


    public ModifyCodeCommand(@NotNull String aggregateIdentifier,
                             @NotNull String membershipId,
                             @NotNull String code) {
        this.membershipId = membershipId;
        this.aggregateIdentifier = aggregateIdentifier;
        this.code = code;

        this.validateSelf();
    }
}