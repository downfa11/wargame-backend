package com.ns.resultquery.adapter.axon;

import com.ns.common.ClientRequest;
import com.ns.common.CreateResultQueryEvent;
import com.ns.common.ResultQueryUpdatedEvent;
import com.ns.common.RollbackUpdateQueryEvent;
import com.ns.resultquery.application.port.in.InsertChampStatisticsUseCase;
import com.ns.resultquery.application.port.in.InsertUserStatisticsUseCase;
import com.ns.resultquery.domain.dto.MembershipResultEventDto;
import com.ns.resultquery.domain.dto.ResultEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameResultEventHandler {
    private final InsertUserStatisticsUseCase insertUserStatisticsUseCase;
    private final InsertChampStatisticsUseCase insertChampStatisticsUseCase;
    private final EventGateway eventGateway; // todo. hexagonal migration

    @EventHandler
    public void handle(CreateResultQueryEvent event) {
        log.info("Result Event Received: " + event);

        List<ClientRequest> allClients = ResultQueryMapper.getAllTeamClientRequests(event);
        handleResultQuery(allClients, event.getWinTeam(), 1L, event);
    }

    @EventHandler
    public void handle(RollbackUpdateQueryEvent event) {
        log.info("Rollback Event Received: " + event);

        List<ClientRequest> allClients = ResultQueryMapper.getAllTeamClientRequests(event);
        handleResultQuery(allClients, event.getWinTeam(), -1L, event);
    }


    private void handleResultQuery(List<ClientRequest> allClients, String winningTeam, Long winCount, Object event) {
        Flux.fromIterable(allClients)
                .flatMap(clientRequest -> {
                    Long adjustedWinCount = clientRequest.getTeam().equals(winningTeam) ? winCount : 0L;

                    MembershipResultEventDto membershipResultEventDto = ResultQueryMapper.getMembershipResultEventDto(clientRequest, adjustedWinCount);
                    ResultEventDto resultEventDto = ResultQueryMapper.getResultEventDto(clientRequest, adjustedWinCount);

                    return insertUserStatisticsUseCase.insertResultCountIncreaseEventByUserName(membershipResultEventDto)
                            .then(insertChampStatisticsUseCase.insertResultCountIncreaseEventByChampName(resultEventDto));
                })
                .doOnError(throwable -> log.error("handleResultQuery error: " + throwable.getMessage()))
                .doOnTerminate(() -> {
                    ResultQueryUpdatedEvent resultQueryUpdatedEvent = createResultQueryUpdatedEvent(event);

                    Mono.fromRunnable(() -> eventGateway.publish(resultQueryUpdatedEvent))
                            .doOnSuccess(aVoid -> log.info("ResultQueryUpdatedEvent successfully published"))
                            .doOnError(throwable -> log.error("Failed to publish ResultQueryUpdatedEvent", throwable))
                            .subscribe();
                })
                .subscribe();
    }

    private ResultQueryUpdatedEvent createResultQueryUpdatedEvent(Object event) {
        if (event instanceof CreateResultQueryEvent) {
            CreateResultQueryEvent createEvent = (CreateResultQueryEvent) event;
            return new ResultQueryUpdatedEvent(
                    createEvent.getSpaceId(),
                    createEvent.getWinTeam(),
                    createEvent.getLoseTeam(),
                    createEvent.getBlueTeams(),
                    createEvent.getRedTeams()
            );
        } else if (event instanceof RollbackUpdateQueryEvent) {
            RollbackUpdateQueryEvent rollbackEvent = (RollbackUpdateQueryEvent) event;
            return new ResultQueryUpdatedEvent(
                    rollbackEvent.getSpaceId(),
                    rollbackEvent.getWinTeam(),
                    rollbackEvent.getLoseTeam(),
                    rollbackEvent.getBlueTeams(),
                    rollbackEvent.getRedTeams()
            );
        }
        throw new IllegalArgumentException("createResultQueryUpdatedEvent: " + event.getClass());
    }

}

