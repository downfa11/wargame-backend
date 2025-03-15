package com.ns.result.adapter.axon;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import com.ns.common.CreatePlayerCommand;
import com.ns.common.ModifyCodeCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.event.CreatePlayerEvent;
import com.ns.result.adapter.axon.event.ModifyCodeEvent;
import com.ns.result.adapter.axon.event.UpdateEloEvent;
import com.ns.result.application.port.out.player.RegisterPlayerPort;
import com.ns.result.application.port.out.player.UpdatePlayerPort;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
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
    public PlayerAggregate(CreatePlayerCommand command, RegisterPlayerPort registerPlayerPort){
        log.info("CreatePlayerCommand command handler: "+ command.getMembershipId());

        id= UUID.randomUUID().toString();
        registerPlayerPort.registerPlayer(command.getMembershipId(), id)
                .subscribe();

        apply(new CreatePlayerEvent(command.getMembershipId(), id));
    }

    @EventSourcingHandler
    public void onCreateMemberEvent(CreatePlayerEvent event){
        id = event.getId();
        membershipId = event.getMembershipId();
        elo = 2000L;
        code = "";


        log.info("CreatePlayerEvent handler: "+ membershipId + "="+elo + "   : " + id);
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

    @CommandHandler
    public void handleModifyCode(@NotNull ModifyCodeCommand command, UpdatePlayerPort updatePlayerPort){
        id = command.getAggregateIdentifier();

        updatePlayerPort.updatePlayerCode(command.getMembershipId(), command.getCode());
        apply(new ModifyCodeEvent(id, command.getMembershipId(), command.getCode()));
    }

    @EventSourcingHandler
    public void onModifyCodeEvent(ModifyCodeEvent event){
        log.info("onModifyCodeEvent "+ event.getMembershipId()+"'s code: " + code + " -> " + event.getCode());
        id = event.getAggregateIdentifier();
        code = event.getCode();
    }
}
