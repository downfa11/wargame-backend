package com.ns.match.controller;


import com.ns.match.Utils.JwtTokenProvider;
import com.ns.match.dto.MatchRequest;
import com.ns.match.dto.messageEntity;
import com.ns.match.service.MatchQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/game")
@Slf4j
@RequiredArgsConstructor
public class MatchController {

    private final MatchQueueService matchQueueService;
    private final JwtTokenProvider jwtTokenProvider;


    @PostMapping("/match")
    public Mono<ResponseEntity<messageEntity>> queue(@RequestBody MatchRequest request, ServerWebExchange exchange) {

        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }
                    return matchQueueService.getRank("match", request.getMembershipId())
                .flatMap(rank -> {
                    if (rank < 0)
                        return matchQueueService.registerMatchQueue("match", request.getMembershipId())
                                .map(result -> {
                                    if(result == "fail")
                                        return ResponseEntity.ok()
                                                .body(new messageEntity("Fail","already user"+request.getMembershipId()+" has curGameSpaceCode."));
                                    return ResponseEntity.ok()
                                        .body(new messageEntity("Success", result));
                                });
                    else return Mono.error(new RuntimeException("Membership ID already in the queue"));

                })
                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().body(new messageEntity("Fail", error.getMessage()))));
        });
    }

    @PostMapping("/match/cancel")
    public Mono<ResponseEntity<messageEntity>> queueCancel(@RequestBody MatchRequest request, ServerWebExchange exchange) {

        Long membershipId = request.getMembershipId();
        if (membershipId == null) {
            return Mono.just(ResponseEntity.badRequest().body(new messageEntity("Error", "Invalid membership ID.")));
        }

        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }
                    return matchQueueService.cancelMatchQueue(membershipId)
                            .then(Mono.just(ResponseEntity.ok().body(new messageEntity("Success", "All queues have been deleted."))))
                            .onErrorResume(error -> {
                                log.error("Error occurred while cancelling match queues: {}", error.getMessage());
                                return Mono.just(ResponseEntity.ok().body(new messageEntity("Error", "Failed to cancel match queues.")));
                            });
                });
    }


    @GetMapping(path="/match/rank/{memberId}")
    public Mono<ResponseEntity<messageEntity>> getRank(@PathVariable Long memberId, ServerWebExchange exchange){

        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }

                    return matchQueueService.getMatchResponse(memberId)
                            .map(matchStatus -> {
                                if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCH_FOUND)
                                    return ResponseEntity.ok().body(new messageEntity("Success", matchStatus.getT2()));
                                else if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCHING)
                                    return ResponseEntity.ok().body(new messageEntity("Success", "matching.."));
                                else
                                    return ResponseEntity.ok().body(new messageEntity("Fail", "Request is not correct."));
                            });
                });
    }
}

