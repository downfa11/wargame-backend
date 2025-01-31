package com.ns.feed.adapter.out.persistence;

import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostR2dbcRepository extends ReactiveCrudRepository<Post,Long> {

    Flux<Post> findByUserId(Long id);
    Flux<Post> findAllByCategoryId(Long categoryId, Pageable pageable);
    Mono<Long> countByCategoryId(Long categoryId);

    @Query("""
            SELECT p.id, p.user_id, p.nickname, p.category_id, p.title, p.content, p.sort_status, p.created_at, p.updated_at
            FROM posts p
            WHERE p.sort_status = 'EVENT'
              AND p.event_start_date <= :now
              AND p.event_end_date >= :now
            ORDER BY p.event_start_date DESC
            """)
    Flux<Post> findInProgressEvents(LocalDateTime now);

    @Query("""
            SELECT p.id, p.user_id, p.nickname, p.category_id, p.title, p.content, p.sort_status, p.created_at, p.updated_at
            FROM posts p
            WHERE p.sort_status = 'ANNOUNCE'
            ORDER BY p.created_at DESC
            LIMIT :count
            """)
    Flux<Post> findLatestAnnounces(Integer count);
}
