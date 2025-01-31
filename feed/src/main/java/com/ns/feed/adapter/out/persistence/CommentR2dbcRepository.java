package com.ns.feed.adapter.out.persistence;


import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentR2dbcRepository extends ReactiveCrudRepository<Comment,Long> {
    Mono<Comment> save(Comment result);
    Mono<Comment> findById(Long id);
    Flux<Comment> findByBoardId(Long boardId);

}
