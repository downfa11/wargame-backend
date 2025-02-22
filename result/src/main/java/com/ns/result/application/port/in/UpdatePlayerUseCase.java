package com.ns.result.application.port.in;

import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.Player;
import reactor.core.publisher.Mono;

public interface UpdatePlayerUseCase {
    Mono<QueryPlayer> updateEloByEvent(String membershipId, Long balancedElo);
    Mono<Player> updateElo(String membershipId, Long increase);
}
