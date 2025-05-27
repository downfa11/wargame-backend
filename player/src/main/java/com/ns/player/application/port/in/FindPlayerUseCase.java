package com.ns.player.application.port.in;


import com.ns.player.adapter.axon.query.QueryPlayer;
import com.ns.player.adapter.out.persistence.Player;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPlayerUseCase {
    Mono<QueryPlayer> queryToPlayerByMembershipId(String membershipId);
    Flux<Player> findAll();
}
