package com.ns.feed.repository;

import com.ns.feed.entity.Post;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostR2dbcRepository extends ReactiveCrudRepository<Post,Long>,  PostCustomR2dbcRepository {

    Flux<Post> findByuserId(Long id);
    Flux<Post> findAllByCategoryId(Long categoryId, Pageable pageable);
    Mono<Long> countByCategoryId(Long categoryId);
}
