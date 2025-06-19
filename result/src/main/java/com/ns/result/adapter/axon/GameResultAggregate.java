package com.ns.result.adapter.axon;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.axon.command.GameFinishedCommand;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

@Aggregate
@NoArgsConstructor
@Slf4j
public class GameResultAggregate {

    @AggregateIdentifier
    private String spaceId;

    @CommandHandler
    public void handle(GameFinishedCommand command) {
        log.info("Handling GameFinishedCommand for spaceId: {}", command.getSpaceId());

        apply(GameFinishedEvent.builder()
                .spaceId(command.getSpaceId())
                .state(command.getState())
                .channel(command.getChannel())
                .room(command.getRoom())
                .winTeam(command.getWinTeam())
                .loseTeam(command.getLoseTeam())
                .blueTeams(command.getBlueTeams())
                .redTeams(command.getRedTeams())
                .dateTime(command.getDateTime())
                .gameDuration(command.getGameDuration())
                .build());
    }

    @EventSourcingHandler
    public void on(GameFinishedEvent event) {
        this.spaceId = event.getSpaceId();
        log.info("Applied GameFinishedEvent for spaceId: {}", spaceId);
    }
}
