package com.ns.match.application.port.in;

import com.ns.match.adapter.out.RedisMatchAdapter.MatchStatus;
import com.ns.match.application.service.MatchResponse;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface GetMatchQueueUseCase {
    Mono<Tuple2<MatchStatus, MatchResponse>> getMatchResponse(Long memberId);
}
