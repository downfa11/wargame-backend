package com.ns.wargame.Controller;


import com.ns.wargame.Domain.dto.MatchRequest;
import com.ns.wargame.Domain.dto.MatchResponse;
import com.ns.wargame.Domain.dto.UserResponse;
import com.ns.wargame.Domain.dto.messageEntity;
import com.ns.wargame.Service.GameResultService;
import com.ns.wargame.Service.GameService;
import com.ns.wargame.Service.MatchQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/game")
@Slf4j
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final MatchQueueService matchQueueService;


    @GetMapping(value = "/rank", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<messageEntity>> rankUsers() {

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success", gameService.getLeaderboard())))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }

    @PostMapping("/match")
    public Mono<ResponseEntity<messageEntity>> queue(@RequestBody MatchRequest request) {
        return matchQueueService.getRank("match", request.getMembershipId())
                .flatMap(rank -> {
                    if (rank < 0)
                        return matchQueueService.registerMatchQueue("match", request.getMembershipId())
                                .map(result -> ResponseEntity.ok()
                                        .body(new messageEntity("Success", result)));

                    else return Mono.error(new RuntimeException("Membership ID already in the queue"));

                })
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().body(new messageEntity("Fail", error.getMessage()))));
    }


    @GetMapping(path="/match/rank/{memberId}")
    public Mono<ResponseEntity<messageEntity>> getRank(@PathVariable Long memberId){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",matchQueueService.getMatchResponse(memberId))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }

    @GetMapping(path="/match/{memberId}")
    public Mono<ResponseEntity<messageEntity>> isMatch(@PathVariable Long memberId){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",matchQueueService.getMatchResponse(memberId))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }


    @GetMapping(path="/kafka/async/test")
    public Mono<ResponseEntity<messageEntity>> create(@RequestParam String data){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",gameService.CommonSendMessage("test","key",data))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }
}

