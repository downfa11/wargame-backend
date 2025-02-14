package com.ns.result.adapter.axon;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import com.ns.result.adapter.axon.command.CreatePlayerCommand;
import com.ns.result.adapter.axon.command.RollbackUpdateEloCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.event.CreatePlayerEvent;
import com.ns.result.adapter.axon.event.RollbackUpdateEloEvent;
import com.ns.result.adapter.axon.event.UpdateEloEvent;
import com.ns.result.application.service.PlayerService;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate(snapshotTriggerDefinition = "snapshotTrigger", cache = "snapshotCache")
@Data
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
        log.info("CreatePlayerCommand Handler: "+id);
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
    public String handleUpdateElo(@NotNull UpdateEloCommand command){
        log.info("UpdateEloCommand Handler: "+id);
        id = command.getAggregateIdentifier();

        apply(new UpdateEloEvent(id, command.getMembershipId(),command.getElo()));
        return id;
    }

    @EventSourcingHandler
    public void onUpdateEloEvent(UpdateEloEvent event, PlayerService playerService){
        log.info("UpdateEloEvent Sourcing Handler: " + elo +" to " + event.getElo());
        id = event.getAggregateIdentifier();
        membershipId = event.getMembershipId();
        elo=elo+event.getElo();
        code = "";

        playerService.updateElo(membershipId, event.getElo())
                .doOnSuccess(updatedPlayer -> log.info("실력점수 변동(readOnly) : " + updatedPlayer.getElo()))
                .subscribe();
    }

    // 게임 종료 이벤트에 대한 실력점수 업데이트(롤백)
    @CommandHandler
    public void handleRollbackElo(RollbackUpdateEloCommand command) {
        log.info("Rollback Elo Command Handler: " + command.getAggregateIdentifier());
        apply(new RollbackUpdateEloEvent(command.getAggregateIdentifier(), command.getMembershipId(), elo));
    }

    @EventHandler
    public void onRollbackEloEvent(RollbackUpdateEloEvent event){
        log.info("Rollback Elo Event Sourcing Handler: " + event.getElo());
        elo = elo - event.getElo();
    }
}
