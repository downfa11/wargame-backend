package com.ns.feed.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CategoryR2dbcRepository extends ReactiveCrudRepository<Category,Long> {
    Mono<Category> save(Category category);
    Mono<Category> findById(Long id);
}
