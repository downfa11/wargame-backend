package com.ns.wargame.Repository;

import com.ns.wargame.Domain.Client;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ClientR2dbcRepository extends ReactiveCrudRepository<Client,Long> {
    Mono<Client> save(Client client);
    Mono<Client> findById(Long id);

    Flux<Client> findByGameResultId(Long resultId);
}
