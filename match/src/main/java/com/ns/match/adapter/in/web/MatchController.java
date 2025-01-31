package com.ns.match.adapter.in.web;


import com.ns.common.utils.MessageEntity;
import com.ns.common.utils.JwtTokenProvider;
import com.ns.match.application.service.MatchQueueService;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/game")
@Slf4j
@RequiredArgsConstructor
public class MatchController {

    private final MatchQueueService matchQueueService;
    private final JwtTokenProvider jwtTokenProvider;


    @PostMapping("/match")
    public Mono<ResponseEntity<MessageEntity>> queue(@RequestBody MatchRequest request, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return matchQueueService.getRank("match", request.getMembershipId())
//                .flatMap(rank -> {
//                    if (rank < 0)
//                        return matchQueueService.registerMatchQueue("match", request.getMembershipId())
//                                .map(result -> {
//                                    if(result == "fail")
//                                        return ResponseEntity.ok()
//                                                .body(new MessageEntity("Fail","already user"+request.getMembershipId()+" has curGameSpaceCode."));
//                                    return ResponseEntity.ok()
//                                        .body(new MessageEntity("Success", result));
//                                });
//                    else return Mono.error(new RuntimeException("Membership ID already in the queue"));
//
//                })
//                .onErrorResume(error -> Mono.just(ResponseEntity.badRequest().body(new MessageEntity("Fail", error.getMessage()))));
//        });
        return matchQueueService.getRank("match", request.getMembershipId())
                .flatMap(rank -> {
                    log.info("getRank count : " + rank);

                    if (rank < 0) {
                        return matchQueueService.registerMatchQueue("match", request.getMembershipId())
                                .defaultIfEmpty("fail")
                                .map(result -> {
                                    log.info(request.getMembershipId() + "'s match queue register logic  :" + result);
                                    if ("fail".equals(result)) {
                                        return ResponseEntity.ok()
                                                .body(new MessageEntity("Fail", "already user " + request.getMembershipId() + " has curGameSpaceCode."));
                                    }
                                    return ResponseEntity.ok()
                                            .body(new MessageEntity("Success", result));
                                });
                    } else {
                        return Mono.error(new RuntimeException("MembershipId 이미 큐에 존재"));
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error queue: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().body(new MessageEntity("Fail", error.getMessage())));
                });
    }

    @PostMapping("/match/cancel")
    public Mono<ResponseEntity<MessageEntity>> queueCancel(@RequestBody MatchRequest request, ServerWebExchange exchange) {

        Long membershipId = request.getMembershipId();
        if (membershipId == null) {
            return Mono.just(ResponseEntity.badRequest().body(new MessageEntity("Error", "Invalid membership ID.")));
        }

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return matchQueueService.cancelMatchQueue(membershipId)
//                            .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", "All queues have been deleted."))))
//                            .onErrorResume(error -> {
//                                log.error("Error occurred while cancelling match queues: {}", error.getMessage());
//                                return Mono.just(ResponseEntity.ok().body(new MessageEntity("Error", "Failed to cancel match queues.")));
//                            });
//                });
                    return matchQueueService.cancelMatchQueue(membershipId)
                            .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", "All queues have been deleted."))))
                            .onErrorResume(error -> {
                                log.error("Error queueCancel: {}", error.getMessage());
                                return Mono.just(ResponseEntity.ok().body(new MessageEntity("Error", "Failed to cancel match queues.")));
                            });
    }


    @GetMapping(path="/match/rank/{memberId}")
    public Mono<ResponseEntity<MessageEntity>> getRank(@PathVariable Long memberId, ServerWebExchange exchange){

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//
//                    return matchQueueService.getMatchResponse(memberId)
//                            .map(matchStatus -> {
//                                if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCH_FOUND)
//                                    return ResponseEntity.ok().body(new MessageEntity("Success", matchStatus.getT2()));
//                                else if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCHING)
//                                    return ResponseEntity.ok().body(new MessageEntity("Success", "matching.."));
//                                else
//                                    return ResponseEntity.ok().body(new MessageEntity("Fail", "Request is not correct."));
//                            });
//                });
                    return matchQueueService.getMatchResponse(memberId)
                            .map(matchStatus -> {
                                if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCH_FOUND)
                                    return ResponseEntity.ok().body(new MessageEntity("Success", matchStatus.getT2()));
                                else if (matchStatus.getT1() == MatchQueueService.MatchStatus.MATCHING)
                                    return ResponseEntity.ok().body(new MessageEntity("Success", "matching.."));
                                else
                                    return ResponseEntity.ok().body(new MessageEntity("Fail", "Request is not correct."));
                            });
    }

    @PostMapping("/test/integration")
    public Mono<ResponseEntity<MessageEntity>> requestIntegrationTest(@RequestParam(defaultValue = "10") int threads,
                                                                      @RequestParam(defaultValue = "5") int requests){


        AtomicLong memberIdGenerator = new AtomicLong(1);

        return Flux.range(0, threads)
                .flatMap(thread -> Flux.range(0, requests)
                        .flatMap(request -> {
                            Long memberId = memberIdGenerator.getAndIncrement();
                            Long elo = 1200 + (long) (Math.random() * 500);
                            String nickName = "test" + memberId;

                            return matchQueueService.requestIntegrationTest(memberId, nickName, elo)
                                    .doOnError(error -> log.error("Error requestIntegrationTest: " + error.getMessage()))
                                    .retry(3)
                                    .onErrorResume(error -> Mono.empty());
                        }, 1)
                )
                .then(matchQueueService.getRequestCount()
                        .map(requestCount -> ResponseEntity.ok(new MessageEntity("success", requestCount)))
                );
    }
}

