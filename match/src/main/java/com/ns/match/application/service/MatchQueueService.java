package com.ns.match.application.service;

import static com.ns.match.exception.ErrorCode.ALREADY_EXIST_IN_QUEUE;

import com.ns.common.anotation.UseCase;
import com.ns.match.adapter.out.RedisMatchAdapter.MatchStatus;
import com.ns.match.application.port.in.CancleMatchQueueUseCase;
import com.ns.match.application.port.in.GetMatchQueueUseCase;
import com.ns.match.application.port.in.RegisterMatchQueueUseCase;
import com.ns.match.application.port.out.CancelMatchQueuePort;
import com.ns.match.application.port.out.GetMatchQueuePort;
import com.ns.match.application.port.out.RegisterMatchQueuePort;
import com.ns.match.exception.MatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class MatchQueueService implements RegisterMatchQueueUseCase, CancleMatchQueueUseCase, GetMatchQueueUseCase {

    private final RegisterMatchQueuePort registerMatchQueuePort;
    private final CancelMatchQueuePort cancelMatchQueuePort;
    private final GetMatchQueuePort getMatchQueuePort;


    @Override
    public Mono<String> registerMatchQueue(String queue, Long userId) {
        return getMatchQueuePort.getRank("match", userId)
                .flatMap(rank -> {
                    log.info("getRank count : " + rank);

                    if (rank < 0) {
                        return registerMatchQueuePort.registerMatchQueue("match", userId)
                                .defaultIfEmpty("fail");
                    } else {
                        return Mono.error(new MatchException(ALREADY_EXIST_IN_QUEUE));
                    }
                });
    }

    @Override
    public Mono<Void> cancelMatchQueue(Long userId) {
        return cancelMatchQueuePort.cancelMatchQueue(userId);
    }

    @Override
    public Mono<Tuple2<MatchStatus, MatchResponse>> getMatchResponse(Long memberId) {
        return getMatchQueuePort.getMatchResponse(memberId);
    }
}
