package com.ns.result.adapter.axon;

import com.ns.result.adapter.axon.query.FindPlayerAggregateQuery;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.PlayerR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerQueryHandler {

    private final PlayerR2dbcRepository playerR2dbcRepository;

    @QueryHandler
    public QueryPlayer handle(FindPlayerAggregateQuery query) {
        log.info("FindPlayerAggregateQuery for membershipId: {}", query.getMembershipId());

        return playerR2dbcRepository.findByMembershipId(query.getMembershipId())
                .map(player -> QueryPlayer.builder()
                        .membershipId(player.getMembershipId())
                        .code(player.getCode())
                        .elo(player.getElo()).build())
                .doOnError(error -> log.error("Error loading player for " + query.getMembershipId() + ": " + error))
                .block();
    }
}
