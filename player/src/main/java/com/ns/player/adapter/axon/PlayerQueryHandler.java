package com.ns.player.adapter.axon;

import com.ns.player.adapter.axon.query.FindPlayerAggregateInfo;
import com.ns.player.adapter.axon.query.FindPlayerAggregateQuery;
import com.ns.player.adapter.axon.query.QueryPlayer;
import com.ns.player.adapter.out.persistence.PlayerR2dbcRepository;
import com.ns.player.application.port.in.PlayerInfo;
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
                        .nickname(player.getNickname())
                        .elo(player.getElo()).build())
                .doOnError(error -> log.error("Error loading player for " + query.getMembershipId() + ": " + error))
                .block();
    }

    @QueryHandler
    public PlayerInfo handle(FindPlayerAggregateInfo query) {
        log.info("FindPlayerAggregateInfo for nickname: {}", query.getNickname());

        return playerR2dbcRepository.findByNickname(query.getNickname())
                .map(player -> PlayerInfo.builder()
                        .nickname(player.getNickname())
                        .elo(player.getElo())
                        .tier(player.getTier().getName())
                        .lastGameTime(player.getLastGameTime())
                        .build())
                .doOnError(error -> log.error("Error loading player for " + query.getNickname() + ": " + error))
                .block();
    }
}
