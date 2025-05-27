package com.ns.match.application.service;

import com.ns.common.PlayerQuery;
import com.ns.common.anotation.UseCase;
import com.ns.match.application.port.in.CancleMatchQueueUseCase;
import com.ns.match.application.port.in.GetMatchQueueUseCase;
import com.ns.match.application.port.in.IntegrationTestMatchUseCase;
import com.ns.match.application.port.in.RegisterMatchQueueUseCase;
import com.ns.match.application.port.out.CancelMatchQueuePort;
import com.ns.match.application.port.out.GetMatchQueuePort;
import com.ns.match.application.port.out.IntegrationTestMatchPort;
import com.ns.match.application.port.out.RegisterMatchQueuePort;
import com.ns.match.dto.MatchResponse;
import com.ns.match.exception.MatchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

import static com.ns.match.exception.ErrorCode.ALREADY_EXIST_IN_QUEUE;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class MatchQueueService implements RegisterMatchQueueUseCase, CancleMatchQueueUseCase, GetMatchQueueUseCase, IntegrationTestMatchUseCase {

    private final RegisterMatchQueuePort registerMatchQueuePort;
    private final CancelMatchQueuePort cancelMatchQueuePort;
    private final GetMatchQueuePort getMatchQueuePort;
    private final IntegrationTestMatchPort integrationTestMatchPort;


    @Override
    public Mono<Boolean> registerMatchQueue(String queue, Long userId) {
        return getMatchQueuePort.calculateRank(userId)
                .flatMap(rank -> {
                    log.info("getRank count : " + rank);

                    if (rank < 0) {
                        return registerMatchQueuePort.registerMatchQueue(userId);
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
    public Mono<MatchResponse> getMatchResponse(Long memberId) {
        return getMatchQueuePort.getMatchResponse(memberId);
    }

    @Override
    public Mono<PlayerQuery> getPlayerQuery(Long memberId){
        return getMatchQueuePort.getPlayerQuery(memberId);
    }

    @Override
    public Mono<Long> requestIntegrationTest(int threads, int requests) {
        AtomicLong memberIdGenerator = new AtomicLong(1);

        return Flux.range(0, threads)
                .flatMap(thread -> Flux.range(0, requests)
                        .flatMap(request -> {
                            Long memberId = memberIdGenerator.getAndIncrement();
                            Long elo = 1200 + (long) (Math.random() * 500);
                            String nickName = "test" + memberId;

                            return integrationTestMatchPort.requestIntegrationTest(memberId, nickName, elo)
                                    .doOnError(error -> log.error("Error requestIntegrationTest: " + error.getMessage()))
                                    .onErrorResume(error -> Mono.empty());
                        })
                )
                .then(integrationTestMatchPort.getRequestCount());
    }
}
