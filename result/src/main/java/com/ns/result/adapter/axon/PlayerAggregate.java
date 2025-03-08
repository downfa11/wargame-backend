package com.ns.result.adapter.axon;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import com.ns.common.CreatePlayerCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.event.CreatePlayerEvent;
import com.ns.result.adapter.axon.event.UpdateEloEvent;
import com.ns.result.application.port.out.player.FindPlayerPort;
import com.ns.result.application.service.PlayerService;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate(snapshotTriggerDefinition = "snapshotTrigger", cache = "snapshotCache")
@Getter
@Slf4j
@NoArgsConstructor
public class PlayerAggregate {
    @AggregateIdentifier
    private String id;

    private String membershipId;
    private Long elo;
    private String code;

    @CommandHandler
    public PlayerAggregate(CreatePlayerCommand command){
        apply(new CreatePlayerEvent(command.getMembershipId()));
    }

    @EventSourcingHandler
    public void onCreateMemberEvent(CreatePlayerEvent event){
        id= UUID.randomUUID().toString();
        membershipId = event.getMembershipId();
        elo = 2000L;
        code = "";
    }

    @CommandHandler
    public void handleUpdateElo(@NotNull UpdateEloCommand command){
        id = command.getAggregateIdentifier();
        apply(new UpdateEloEvent(id, command.getMembershipId(), command.getElo()));
    }

    @EventSourcingHandler
    public void onUpdateEloEvent(UpdateEloEvent event){
        log.info("UpdateEloEvent "+ event.getMembershipId()+"'s elo: " + elo + " -> " + event.getElo());
        id = event.getAggregateIdentifier();
        membershipId = event.getMembershipId();
        elo = event.getElo();
        code = "";
    }
}
