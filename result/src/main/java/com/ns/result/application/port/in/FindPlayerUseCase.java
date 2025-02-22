package com.ns.result.application.port.in;

import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.Player;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPlayerUseCase {
    Mono<QueryPlayer> queryToPlayerByMembershipId(String membershipId);
    Flux<Player> findAll();
}
