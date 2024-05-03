package com.ns.wargame.Controller;


import com.ns.wargame.Domain.dto.MatchRequest;
import com.ns.wargame.Domain.dto.messageEntity;
import com.ns.wargame.Service.GameResultService;
import com.ns.wargame.Service.GameService;
import com.ns.wargame.Service.MatchQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/game")
@Slf4j
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;
    private final GameResultService gameResultService;
    private final MatchQueueService matchQueueService;


    @GetMapping(value = "/rank", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<messageEntity>> rankUsers() {
        return gameResultService.getLeaderboard()
                .collectList()
                .map(users -> ResponseEntity.ok()
                        .body(new messageEntity("Success", users.stream()
                        .collect(Collectors.toList()))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @PostMapping("/match")
    public Mono<ResponseEntity<messageEntity>> queue(@RequestBody MatchRequest request) {
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
    }

    @PostMapping("/match/cancel")
    public Mono<ResponseEntity<messageEntity>> queueCancel(@RequestBody MatchRequest request) {
        Long membershipId = request.getMembershipId();
        if (membershipId == null) {
            return Mono.just(ResponseEntity.badRequest().body(new messageEntity("Error", "Invalid membership ID.")));
        }

        return matchQueueService.cancelMatchQueue(membershipId)
                .then(Mono.just(ResponseEntity.ok().body(new messageEntity("Success", "All queues have been deleted."))))
                .onErrorResume(error -> {
                    log.error("Error occurred while cancelling match queues: {}", error.getMessage());
                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Error", "Failed to cancel match queues.")));
                });
    }




    @GetMapping(path="/match/rank/{memberId}")
    public Mono<ResponseEntity<messageEntity>> getRank(@PathVariable Long memberId){
        return matchQueueService.getMatchResponse(memberId)
                .map(matchStatus -> {
                    if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCH_FOUND)
                        return ResponseEntity.ok().body(new messageEntity("Success", matchStatus.getT2()));
                    else if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCHING)
                        return ResponseEntity.ok().body(new messageEntity("Success", "matching.."));
                    else return ResponseEntity.ok().body(new messageEntity("Fail", "Request is not correct."));
                });
    }



    @GetMapping(path="/kafka/async/test")
    public Mono<ResponseEntity<messageEntity>> create(@RequestParam String data){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",gameService.CommonSendMessage("test","key",data))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }
}

