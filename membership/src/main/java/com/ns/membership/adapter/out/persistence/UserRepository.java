package com.ns.membership.adapter.out.persistence;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<User> save(User user);
    Flux<User> findAll();
    Mono<User> findByIndex(Long index);

    Mono<Integer> deleteByIndex(Long index);
}
