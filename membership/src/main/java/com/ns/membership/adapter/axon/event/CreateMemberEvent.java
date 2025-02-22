package com.ns.membership.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CreateMemberEvent  extends SelfValidating<CreateMemberEvent> {

    private String account;
    private String name;
    private String email;
    private String password;

    public CreateMemberEvent(@NotNull String account, @NotNull String name, @NotNull String email, @NotNull String password) {
        this.account = account;
        this.name = name;
        this.email = email;
        this.password = password;

        this.validateSelf();
    }
}

