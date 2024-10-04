package com.ns.resultquery.axon;

import com.ns.common.ClientRequest;
import com.ns.common.ResultRequestEvent;
import com.ns.resultquery.dto.MembershipResultEventDto;
import com.ns.resultquery.dto.ResultEventDto;
import com.ns.resultquery.service.ResultQueryService;
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
public class GameResultEventHandler implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, ResultRequestEvent> reactiveKafkaConsumerTemplate;
    private final EventGateway eventGateway;
    private final Map<Long, String> champList = new HashMap<>();

    public GameResultEventHandler(ReactiveKafkaConsumerTemplate<String, ResultRequestEvent> reactiveKafkaConsumerTemplate,
                                  EventGateway eventGateway) {
        this.reactiveKafkaConsumerTemplate = reactiveKafkaConsumerTemplate;
        this.eventGateway = eventGateway;

        for (long i = 1; i <= 10; i++) {
            champList.put(i, "test-champ-" + i);
        }
    }


    @EventHandler
    public void handle(ResultRequestEvent event, ResultQueryService resultQueryService) {
        System.out.println("Result Event Received: " + event.toString());

        List<ClientRequest> allClients = new ArrayList<>();
        allClients.addAll(event.getBlueTeams());
        allClients.addAll(event.getRedTeams());

        String winningTeam = event.getWinTeam();

        Flux.fromIterable(allClients)
                .flatMap(clientRequest -> {
                    Long champIndex = clientRequest.getChampindex();
                    String champName = champList.get(champIndex);

                    Long winCount = clientRequest.getTeam().equals(winningTeam) ? 1L : 0L;
                    Long loseCount = clientRequest.getTeam().equals(winningTeam) ? 0L : 1L;

                    MembershipResultEventDto membershipResultEventDto =
                            MembershipResultEventDto.builder()
                                    .membershipId(clientRequest.getMembershipId())
                                    .userName(clientRequest.getUser_name())
                                    .champIndex(champIndex)
                                    .champName(champName)
                                    .resultCount(1L)
                                    .winCount(winCount)
                                    .loseCount(loseCount)
                                    .build();

                    Mono<Void> userMono = resultQueryService.insertResultCountIncreaseEventByUserName(membershipResultEventDto);

                    ResultEventDto eventDto = ResultEventDto.builder()
                            .champIndex(champIndex)
                            .champName(champName)
                            .resultCount(1L)
                            .winCount(winCount)
                            .loseCount(loseCount)
                            .build();

                    Mono<Void> champMono = resultQueryService.insertResultCountIncreaseEventByChampName(eventDto);

                    return userMono.then(champMono);

                })
                .doOnError(throwable -> System.err.println("Error occurred: " + throwable.getMessage()))
                .subscribe();
    }

    @Override
    public void run(ApplicationArguments args){

        this.reactiveKafkaConsumerTemplate
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

