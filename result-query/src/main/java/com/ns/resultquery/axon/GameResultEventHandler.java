package com.ns.resultquery.axon;

import com.ns.common.ClientRequest;
import com.ns.common.ResultRequestEvent;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameResultEventHandler implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, ResultRequestEvent> ReactiveKafkaConsumerTemplate;
    private final EventGateway eventGateway;

    @EventHandler
    public void handle(ResultRequestEvent event, ResultQueryService resultQueryService) {
        System.out.println("Result Event Received: " + event.toString());

        List<ClientRequest> allClients = new ArrayList<>();
        allClients.addAll(event.getBlueTeams());
        allClients.addAll(event.getRedTeams());

        String winningTeam = event.getWinTeam();

        Flux.fromIterable(allClients)
                .flatMap(clientRequest -> {
                    Long winCount = clientRequest.getTeam().equals(winningTeam) ? 1L : 0L;
                    Long loseCount = clientRequest.getTeam().equals(winningTeam) ? 0L : 1L;

                    ResultEventDto eventDto = ResultEventDto.builder()
                            .champIndex((long) clientRequest.getChampindex())
                            .champName(clientRequest.getUser_name()) // todo. champName 가져오기
                            .season(1L)
                            .resultCount(1L)
                            .winCount(winCount)
                            .loseCount(loseCount)
                            .build();

                    return resultQueryService.insertResultCountIncreaseEventByChampName(eventDto);
                })
                .doOnError(throwable -> System.err.println("Error occurred: " + throwable.getMessage()))
                .subscribe();
    }

    @Override
    public void run(ApplicationArguments args){

        this.ReactiveKafkaConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    ResultRequestEvent event = r.value();
                    eventGateway.publish(event);
                    log.info("ResultRequestEvent publish to eventGateway Successfully!");
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

}

