package com.ns.match.adapter.in.web;


import com.ns.common.utils.MessageEntity;
import com.ns.match.adapter.out.RedisMatchAdapter;
import com.ns.match.application.port.in.CancleMatchQueueUseCase;
import com.ns.match.application.port.in.GetMatchQueueUseCase;
import com.ns.match.application.port.in.IntegrationTestMatchUseCase;
import com.ns.match.application.port.in.RegisterMatchQueueUseCase;
import com.ns.match.dto.MatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/game")
@Slf4j
@RequiredArgsConstructor
public class MatchController {

    private final RegisterMatchQueueUseCase registerMatchQueueUseCase;
    private final CancleMatchQueueUseCase cancleMatchQueueUseCase;
    private final GetMatchQueueUseCase getMatchQueueUseCase;
    private final IntegrationTestMatchUseCase integrationTestMatchUseCase;



    @PostMapping("/match")
    public Mono<ResponseEntity<MessageEntity>> queue(@RequestBody MatchRequest request, ServerWebExchange exchange) {
        return registerMatchQueueUseCase.registerMatchQueue("match", request.getMembershipId())
                .map(result -> {
                    if ("fail".equals(result)) {
                        return ResponseEntity.ok()
                                .body(new MessageEntity("Fail", "user " + request.getMembershipId() + " already has Code or Not found membershipId."));
                    }
                    return ResponseEntity.ok()
                            .body(new MessageEntity("Success", result));
                })
                .onErrorResume(error -> {
                    log.error("Error queue: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest()
                            .body(new MessageEntity("Fail", error.getMessage())));
                });
    }

    @PostMapping("/match/cancel")
    public Mono<ResponseEntity<MessageEntity>> queueCancel(@RequestBody MatchRequest request, ServerWebExchange exchange) {
        Long membershipId = request.getMembershipId();
        if (membershipId == null) {
            return Mono.just(ResponseEntity.badRequest().body(new MessageEntity("Error", "Invalid membership ID.")));
        }

        return cancleMatchQueueUseCase.cancelMatchQueue(membershipId)
                .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", "All queues have been deleted."))))
                .onErrorResume(error -> {
                    log.error("Error queueCancel: {}", error.getMessage());
                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Error", "Failed to cancel match queues.")));
                });
    }


    @GetMapping(path = "/match/rank/{memberId}")
    public Mono<ResponseEntity<MessageEntity>> getMatchResponse(@PathVariable Long memberId, ServerWebExchange exchange) {
        return getMatchQueueUseCase.getMatchResponse(memberId)
                .map(matchStatus -> {
                    if (matchStatus.getSpaceId()!=null)
                        return ResponseEntity.ok().body(new MessageEntity("Success", matchStatus));

                    return ResponseEntity.ok().body(new MessageEntity("Success", "matching.."));
                });
    }

    @PostMapping("/test/integration")
    public Mono<ResponseEntity<MessageEntity>> requestIntegrationTest(@RequestParam(defaultValue = "10") int threads,
                                                                      @RequestParam(defaultValue = "5") int requests) {
        return integrationTestMatchUseCase.requestIntegrationTest(threads, requests)
                .map(requestCount -> ResponseEntity.ok(new MessageEntity("success", requestCount)));
    }

    @GetMapping("/test/query")
    public Mono<ResponseEntity<MessageEntity>> getPlayerQueryTest(@RequestParam(defaultValue = "1") Long userId) {
        return getMatchQueueUseCase.getPlayerQuery(userId)
                .map(playerQuery -> ResponseEntity.ok(new MessageEntity("success", playerQuery)))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageEntity("not_found", "플레이어 정보를 찾을 수 없습니다."))))
                .onErrorResume(error -> {
                    log.error("getPlayerQuery 실패: {}", error.getMessage(), error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new MessageEntity("error", "내부 서버 오류")));
                });
    }

}

