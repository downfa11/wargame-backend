package com.ns.feed.repository;

import com.ns.feed.entity.Category;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CategoryR2dbcRepository extends ReactiveCrudRepository<Category,Long> {
    Mono<Category> save(Category category);
    Mono<Category> findById(Long id);
}
