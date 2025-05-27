package com.ns.membership.adapter.axon;


import com.ns.membership.adapter.axon.command.CreateMemberCommand;
import com.ns.membership.adapter.axon.command.ModifyMemberCommand;
import com.ns.membership.adapter.axon.event.CreateMemberEvent;
import com.ns.membership.adapter.axon.event.ModifyMemberEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate(snapshotTriggerDefinition = "snapshotTrigger", cache = "snapshotCache")
@Getter
@Slf4j
@NoArgsConstructor
public class MemberAggregate {
    private Long membershipId;

    @AggregateIdentifier
    private String id;

    private String account;
    private String name;
    private String email;
    private String password;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @CommandHandler
    public MemberAggregate(CreateMemberCommand command){
        this.id = command.getAggregateIdentifier();
        this.membershipId = UUID.randomUUID().getMostSignificantBits();
        log.info("CreateMemberCommand Handler: "+id);
        apply(new CreateMemberEvent(id, membershipId, command.getAccount(), command.getName(), command.getEmail(), command.getPassword()));
    }

    @CommandHandler
    public String handleModifyMember(ModifyMemberCommand command){
        log.info("ModifyMemberCommand Handler: "+id);
        id = command.getAggregateIdentifier();
        apply(new ModifyMemberEvent(id, command.getMembershipId(),command.getAccount(), command.getName(), command.getEmail(), command.getPassword()));
        return id;
    }

    @EventSourcingHandler
    public void onCreateMemberEvent(CreateMemberEvent event){
        id=event.getAggregateIdentifier();
        membershipId = event.getMembershipId();
        account=event.getAccount();
        name = event.getName();
        email = event.getEmail();
        password = event.getPassword();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }


    @EventSourcingHandler
    public void onModifyMemberEvent(ModifyMemberEvent event){
        log.info("ModifyMemberEvent Sourcing Handler");
        id = event.getAggregateIdentifier();
        membershipId = event.getMembershipId();
        account=event.getAccount();
        name = event.getName();
        email = event.getEmail();
        password = event.getPassword();
        updatedAt = LocalDateTime.now();
    }
}

