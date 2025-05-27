package com.ns.match.application.port.out;

import com.ns.common.PlayerQuery;
import com.ns.match.dto.MatchResponse;
import reactor.core.publisher.Mono;

public interface GetMatchQueuePort {
    Mono<Long> calculateRank(final Long userId);
    Mono<PlayerQuery> getPlayerQuery(Long userId);
    Mono<MatchResponse> getMatchResponse(Long memberId);

}
