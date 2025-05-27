package com.ns.player.application.port.in;

import com.ns.player.adapter.axon.query.QueryPlayer;
import com.ns.player.adapter.out.persistence.Player;
import reactor.core.publisher.Mono;

public interface UpdatePlayerUseCase {
    Mono<QueryPlayer> updateEloByEvent(String membershipId, Long balancedElo);
    Mono<Player> updateElo(String membershipId, Long newElo);
}
