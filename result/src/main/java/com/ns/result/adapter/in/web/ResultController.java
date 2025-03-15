package com.ns.result.adapter.in.web;


import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.in.FindResultUseCase;
import com.ns.result.application.port.in.RegisterResultUseCase;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/result")
@Slf4j
@RequiredArgsConstructor
public class ResultController {

    private final RegisterResultUseCase registerResultUseCase;
    private final FindResultUseCase findResultUseCase;
    private final EventGateway eventGateway;

    @PostMapping("/test/event")
    public Mono<ResponseEntity<String>> publishTestEvent() {
        List<ClientRequest> blueTeam = List.of(
                ClientRequest.builder()
                        .membershipId(18L)
                        .socket(1)
                        .champindex(200L)
                        .user_name("BluePlayer1")
                        .team("blue")
                        .channel(1)
                        .room(101)
                        .kill(5)
                        .death(2)
                        .assist(3)
                        .gold(1500)
                        .level(10)
                        .maxhp(2000)
                        .maxmana(800)
                        .attack(150)
                        .critical(20)
                        .criProbability(10)
                        .attrange(5)
                        .attspeed(1.2f)
                        .movespeed(350)
                        .itemList(List.of(101, 102, 103))
                        .build()
        );

        List<ClientRequest> redTeam = List.of(
                ClientRequest.builder()
                        .membershipId(19L)
                        .socket(2)
                        .champindex(300L)
                        .user_name("RedPlayer1")
                        .team("red")
                        .channel(1)
                        .room(101)
                        .kill(2)
                        .death(5)
                        .assist(1)
                        .gold(1200)
                        .level(9)
                        .maxhp(1800)
                        .maxmana(700)
                        .attack(140)
                        .critical(15)
                        .criProbability(8)
                        .attrange(4)
                        .attspeed(1.1f)
                        .movespeed(340)
                        .itemList(List.of(201, 202, 203))
                        .build()
        );

        GameFinishedEvent event = GameFinishedEvent.builder()
                .spaceId(UUID.randomUUID().toString())
                .state("success")
                .channel(1)
                .room(101)
                .winTeam("blue")
                .loseTeam("red")
                .blueTeams(blueTeam)
                .redTeams(redTeam)
                .dateTime("2025-03-05T12:00:00Z")
                .gameDuration(300)
                .build();

        return Mono.fromRunnable(() -> {
            eventGateway.publish(event);
        }).thenReturn(ResponseEntity.ok("테스트가 성공했는가?"));
    }

    @GetMapping("/list")
    public Flux<Result> getResultList(){ return findResultUseCase.getResultList(); }

    @GetMapping("/search/name/{name}")
    public Flux<Result> getGameResultsByName(@PathVariable String name, @RequestParam int offset) {
        return findResultUseCase.getGameResultsByName(name, offset);
    }

    @GetMapping("/search/id/{membershipId}")
    public Flux<Result> getGameResultsByMembershipId(@PathVariable Long membershipId,  @RequestParam int offset) {
        return findResultUseCase.getGameResultsByMembershipId(membershipId, offset);
    }

    @PostMapping("/temp")
    public Mono<Result> createResultTemp(){
        return registerResultUseCase.createResultTemp();
    }

}

