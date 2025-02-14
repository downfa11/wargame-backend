package com.ns.membership.adapter.axon.command;

import com.ns.common.utils.SelfValidating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class CreateMemberCommand extends SelfValidating<CreateMemberCommand> {
    private String account;
    private String name;
    private String email;
    private String password;

    public CreateMemberCommand(@NotNull String account, @NotNull String name, @NotNull String email, @NotNull String password) {
        this.account = account;
        this.name = name;
        this.email = email;
        this.password = password;

        this.validateSelf();
    }
}
