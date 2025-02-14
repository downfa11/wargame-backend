package com.ns.membership.adapter.axon.event;

import com.ns.common.utils.SelfValidating;
import com.ns.membership.adapter.axon.command.CreateMemberCommand;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
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

