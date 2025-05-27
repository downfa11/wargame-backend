package com.ns.player.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PlayerR2dbcRepository extends ReactiveCrudRepository<Player, Long> {
    Mono<Player> findByMembershipId(String membershipId);
}
