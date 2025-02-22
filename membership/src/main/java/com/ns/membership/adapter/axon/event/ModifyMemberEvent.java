package com.ns.membership.adapter.axon.event;


import com.ns.common.utils.SelfValidating;
import com.ns.membership.adapter.axon.command.ModifyMemberCommand;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ModifyMemberEvent extends SelfValidating<ModifyMemberCommand> {

    private String aggregateIdentifier;
    private Long membershipId;

    private String account;
    private String name;
    private String email;
    private String password;

    public ModifyMemberEvent(String aggregateIdentifier, Long membershipId, String account, String name, String email, String password) {
        this.aggregateIdentifier = aggregateIdentifier;
        this.membershipId = membershipId;
        this.account = account;
        this.name = name;
        this.email = email;
        this.password = password;

        this.validateSelf();
    }
}
