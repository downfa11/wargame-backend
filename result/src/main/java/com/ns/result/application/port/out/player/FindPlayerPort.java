package com.ns.result.application.port.out.player;

import com.ns.result.adapter.out.persistence.psql.Player;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindPlayerPort {
    Mono<Player> findByMembershipId(String membershipId);
    Flux<Player> findAll();
}
