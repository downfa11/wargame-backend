package com.ns.result.adapter.in.web;


import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.in.FindResultUseCase;
import com.ns.result.application.port.in.RegisterResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/result")
@Slf4j
@RequiredArgsConstructor
public class ResultController {

    private final RegisterResultUseCase registerResultUseCase;
    private final FindResultUseCase findResultUseCase;

    @GetMapping("/list")
    public Flux<Result> getResultList(){ return findResultUseCase.getResultList(); }

    @GetMapping("/search/name/{name}")
    public Flux<MatchDto> getGameResultsByName(@PathVariable String name, @RequestParam int offset) {
        return findResultUseCase.getGameResultsByName(name, offset)
                .map(MatchDto::from);
    }

    @GetMapping("/search/id/{membershipId}")
    public Flux<MatchDto> getGameResultsByMembershipId(@PathVariable Long membershipId,  @RequestParam int offset) {
        return findResultUseCase.getGameResultsByMembershipId(membershipId, offset)
                .map(MatchDto::from);
    }

    @PostMapping("/temp")
    public Mono<MatchDto> createResultTemp(){
        return registerResultUseCase.createResultTemp().map(MatchDto::from);
    }

    @PostMapping("/test/event")
    public Mono<GameFinishedEvent> publishTestEvent() {
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
        // Mono.fromRunnable(() -> eventGateway.publish(event)).thenReturn()
        return Mono.just(event);
    }
}

