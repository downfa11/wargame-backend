package com.ns.result.adapter.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.CreatePlayerCommand;
import com.ns.result.adapter.axon.command.GameFinishedCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.query.FindPlayerAggregateQuery;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.application.port.out.SendCommandPort;
import com.ns.result.application.port.out.SendQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class PlayerEventSourcingAdapter implements SendCommandPort, SendQueryPort {
    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    private final ObjectMapper objectMapper;

    @Override
    public void sendReceivedGameFinishedEvent(SubTask subtask) {
        GameFinishedCommand axonCommand = objectMapper.convertValue(subtask.getData(), GameFinishedCommand.class);

        Mono.fromFuture(() -> commandGateway.send(axonCommand))
                .doOnSuccess(success -> log.info("GameFinishedCommand sent successfully: " + success))
                .doOnError(throwable -> log.error("Failed to send GameFinishedCommand: " + throwable))
                .then().subscribe();
    }

    @Override
    public Mono<String> sendCreatePlayer(CreatePlayerCommand command) {
        log.info("CreatePlayerCommand를 전달.");
        return Mono.fromFuture(() -> commandGateway.send(command));
    }

    @Override
    public Mono<String> sendUpdatePlayerElo(UpdateEloCommand command) {
        log.info("sendUpdatePlayerElo 전달. "+command.getMembershipId() + " : " + command.getElo());
        return Mono.fromFuture(() -> commandGateway.send(command));
    }

    @Override
    public Mono<QueryPlayer> sendPlayerQuery(String membershipId) {
        return Mono.fromFuture(() -> queryGateway.query(new FindPlayerAggregateQuery(membershipId), QueryPlayer.class));
    }
}
