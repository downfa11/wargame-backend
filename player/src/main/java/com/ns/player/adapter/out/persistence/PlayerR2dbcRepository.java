package com.ns.player.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlayerR2dbcRepository extends ReactiveCrudRepository<Player, Long> {
    Mono<Player> findByMembershipId(String membershipId);
    Mono<Player> findByNickname(String nickname);
    @Query("SELECT * FROM players ORDER BY elo DESC")
    Flux<Player> findTopPlayersByElo(Pageable pageable);
}
