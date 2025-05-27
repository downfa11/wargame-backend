package com.ns.match.application.port.in;

import com.ns.common.PlayerQuery;
import com.ns.match.dto.MatchResponse;
import reactor.core.publisher.Mono;

public interface GetMatchQueueUseCase {
    Mono<MatchResponse> getMatchResponse(Long memberId);
    Mono<PlayerQuery> getPlayerQuery(Long memberId);
}
