package com.ns.wargame.Repository;

import com.ns.wargame.Domain.GameResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ResultR2dbcRepository extends ReactiveCrudRepository<GameResult,Long> {
    Mono<GameResult> save(GameResult result);
    Flux<GameResult> findAll();
    Mono<GameResult> findById(Long id);

}
