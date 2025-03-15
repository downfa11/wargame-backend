package com.ns.result.application.port.out.player;

import com.ns.result.adapter.out.persistence.psql.Player;
import reactor.core.publisher.Mono;

public interface UpdatePlayerPort {
    Mono<Player> updatePlayerElo(String membershipId, Long newElo);
    Mono<Player> updatePlayerCode(String membershipId, String newCode);
}
