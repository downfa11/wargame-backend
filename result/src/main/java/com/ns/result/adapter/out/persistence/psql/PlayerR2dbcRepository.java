package com.ns.result.adapter.out.persistence.psql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PlayerR2dbcRepository extends ReactiveCrudRepository<Player, Long> {

    Mono<Player> findByMembershipId(String membershipId);
}
