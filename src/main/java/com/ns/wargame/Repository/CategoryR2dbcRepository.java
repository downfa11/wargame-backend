package com.ns.wargame.Repository;

import com.ns.wargame.Domain.Category;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CategoryR2dbcRepository extends ReactiveCrudRepository<Category,Long> {
    Mono<Category> save(Category category);
    Mono<Category> findById(Long id);
}
