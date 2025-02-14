package com.ns.resultquery.adapter.axon;

import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.resultquery.application.port.in.InsertChampStatisticsUseCase;
import com.ns.resultquery.application.port.in.InsertUserStatisticsUseCase;
import com.ns.resultquery.domain.dto.MembershipResultEventDto;
import com.ns.resultquery.domain.dto.ResultEventDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameResultEventHandler {
    private final InsertUserStatisticsUseCase insertUserStatisticsUseCase;
    private final InsertChampStatisticsUseCase insertChampStatisticsUseCase;

    @EventHandler
    public void handle(GameFinishedEvent event) {
        log.info("Result Event Received: " + event);

        List<ClientRequest> allClients = ResultQueryMapper.getAllTeamClientRequests(event);
        String winningTeam = event.getWinTeam();

        Flux.fromIterable(allClients)
                .flatMap(clientRequest -> {
                    Long winCount = clientRequest.getTeam().equals(winningTeam) ? 1L : 0L;

                    MembershipResultEventDto membershipResultEventDto = ResultQueryMapper.getMembershipResultEventDto(clientRequest, winCount);
                    ResultEventDto resultEventDto = ResultQueryMapper.getResultEventDto(clientRequest, winCount);

                    return insertUserStatisticsUseCase.insertResultCountIncreaseEventByUserName(membershipResultEventDto)
                            .then(insertChampStatisticsUseCase.insertResultCountIncreaseEventByChampName(resultEventDto));

                })
                .doOnError(throwable -> log.error("Error : " + throwable.getMessage()))
                .subscribe();
    }
}

