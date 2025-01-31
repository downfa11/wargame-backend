package com.ns.membership.adapter.axon.aggregate;


import com.ns.common.command.ModifyMemberEloCommand;
import com.ns.common.events.ModifyMemberEloEvent;
import com.ns.common.events.RollbackModifyMemberEloEvent;
import com.ns.membership.adapter.axon.command.CreateMemberCommand;
import com.ns.membership.adapter.axon.event.CreateMemberEvent;
import com.ns.membership.adapter.axon.event.ModifyMemberEvent;
import com.ns.membership.adapter.axon.command.ModifyMemberCommand;
import lombok.Data;
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
@Data
@Slf4j
@NoArgsConstructor
public class MemberAggregate {
    @AggregateIdentifier
    private String id;

    private Long membershipId;

    private Long previousElo;
    private Long elo;

    private String account;
    private String name;
    private String email;
    private String password;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @CommandHandler
    public MemberAggregate(CreateMemberCommand command){
        log.info("CreateMemberCommand Handler: "+id);
        apply(new CreateMemberEvent(command.getAccount(), command.getName(), command.getEmail(), command.getPassword()));
    }

    @CommandHandler
    public String handleModifyMember(ModifyMemberCommand command){
        log.info("ModifyMemberCommand Handler: "+id);
        id = command.getAggregateIdentifier();
        apply(new ModifyMemberEvent(id, command.getMembershipId(),command.getAccount(), command.getName(), command.getEmail(), command.getPassword()));
        return id;
    }

    @CommandHandler
    public String handleModifyMemberElo(ModifyMemberEloCommand command){
        log.info("ModifyMemberEloCommand Handler: "+id);
        id = command.getAggregateIdentifier();
        apply(new ModifyMemberEloEvent(id, command.getMembershipId(),command.getElo()));
        return id;
    }

    @EventSourcingHandler
    public void onCreateMemberEvent(CreateMemberEvent event){
        id=UUID.randomUUID().toString();
        elo = 2000L;
        account=event.getAccount();
        name = event.getName();
        email = event.getEmail();
        password = event.getPassword();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @EventSourcingHandler
    public void onModifyMemberEloEvent(ModifyMemberEloEvent event){
        log.info("ModifyMemberEloEvent Sourcing Handler");

        previousElo = this.elo;

        id = event.getAggregateIdentifier();
        membershipId = Long.parseLong(event.getMembershipId());
        elo=event.getElo();

        apply(new RollbackModifyMemberEloEvent(event.getAggregateIdentifier(), previousElo));
    }


    @EventSourcingHandler
    public void onModifyMemberEvent(ModifyMemberEvent event){
        log.info("ModifyMemberEvent Sourcing Handler");
        id = event.getAggregateIdentifier();
        membershipId = Long.parseLong(event.getMembershipId());
        account=event.getAccount();
        name = event.getName();
        email = event.getEmail();
        password = event.getPassword();
        updatedAt = LocalDateTime.now();
    }

    @EventSourcingHandler
    public void onRollbackModifyMemberEloEvent(RollbackModifyMemberEloEvent event) {
        log.info("Reverting Elo score for member: " + event.getAggregateIdentifier());
        elo = event.getPreviousElo();
    }

}

