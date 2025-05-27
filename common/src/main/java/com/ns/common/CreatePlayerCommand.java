package com.ns.common;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CreatePlayerCommand extends SelfValidating<CreatePlayerCommand> {

    @NotNull
    private String membershipId;
    @NotNull
    private String nickname;

    public CreatePlayerCommand(@NotNull String membershipId, @NotNull String nickname) {
        this.membershipId = membershipId;
        this.nickname = nickname;
        this.validateSelf();
    }

}
