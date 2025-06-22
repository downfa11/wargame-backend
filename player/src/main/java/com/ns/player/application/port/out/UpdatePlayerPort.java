package com.ns.player.application.port.out;


import com.ns.player.adapter.out.persistence.Player;
import com.ns.player.adapter.out.persistence.Tier;
import reactor.core.publisher.Mono;

public interface UpdatePlayerPort {
    Mono<Player> updatePlayerElo(String membershipId, Long newElo);
    Mono<Player> updateRankerTier(String membershipId, Tier tier);
    Mono<Player> updatePlayerCode(String membershipId, String newCode);
}