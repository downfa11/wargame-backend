package com.ns.result.adapter.axon;

import com.ns.common.*;
import com.ns.result.application.port.out.search.DeleteResultPort;
import com.ns.result.application.port.out.search.RegisterResultPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ResultEventHandler {
    private final RegisterResultPort registerResultPort;
    private final DeleteResultPort deleteResultPort;
    private final EventGateway eventGateway; // todo. hexagonal migration

    @EventHandler
    public void handle(CreateResultEvent event) {
        log.info("CreateResultEvent Received: " + event);
        registerResultPort.saveResult(event)
                .doOnSuccess(result -> {
                    log.info(event.getSpaceId() + "에 해당하는 전적을 생성합니다. " + result);
                    eventGateway.publish(new GameResultSavedEvent(event.getSpaceId(), event.getWinTeam(), event.getLoseTeam(), event.getBlueTeams(), event.getRedTeams()));
                })
                .subscribe();
    }

    @EventHandler
    public void handle(RollbackGameResultEvent event) {
        log.info("RollbackGameResultEvent Received: " + event);

        deleteResultPort.deleteResult(event.getSpaceId())
                .doOnSuccess(bool -> log.info(event.getSpaceId() + "에 해당하는 전적을 삭제합니다. " + bool))
                .subscribe();
    }

}

