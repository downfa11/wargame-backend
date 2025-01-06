package com.ns.resultquery.axon;

import static com.ns.resultquery.ResultQueryMapper.getAllTeamClientRequests;
import static com.ns.resultquery.ResultQueryMapper.getMembershipResultEventDto;
import static com.ns.resultquery.ResultQueryMapper.getResultEventDto;

import com.ns.common.ClientRequest;
import com.ns.common.ResultRequestEvent;
import com.ns.resultquery.dto.MembershipResultEventDto;
import com.ns.resultquery.dto.ResultEventDto;
import com.ns.resultquery.service.ResultQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameResultEventHandler implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, ResultRequestEvent> reactiveKafkaConsumerTemplate;
    private final EventGateway eventGateway;


    @EventHandler
    public void handle(ResultRequestEvent event, ResultQueryService resultQueryService) {
        log.info("Result Event Received: " + event);

        List<ClientRequest> allClients = getAllTeamClientRequests(event);
        String winningTeam = event.getWinTeam();

        Flux.fromIterable(allClients)
                .flatMap(clientRequest -> {
                    Long winCount = clientRequest.getTeam().equals(winningTeam) ? 1L : 0L;

                    MembershipResultEventDto membershipResultEventDto = getMembershipResultEventDto(clientRequest, winCount);
                    ResultEventDto resultEventDto = getResultEventDto(clientRequest, winCount);

                    return resultQueryService.insertResultCountIncreaseEventByUserName(membershipResultEventDto)
                            .then(resultQueryService.insertResultCountIncreaseEventByChampName(resultEventDto));

                })
                .doOnError(throwable -> log.error("Error occurred: " + throwable.getMessage()))
                .subscribe();
    }


    @Override
    public void run(ApplicationArguments args){
        this.reactiveKafkaConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    eventGateway.publish(r.value());
                    log.info("ResultRequestEvent publish to eventGateway");
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

}

