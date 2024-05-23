package com.ns.wargame.Repository;

import com.ns.wargame.Domain.Comment;
import com.ns.wargame.Domain.GameResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommentR2dbcRepository extends ReactiveCrudRepository<Comment,Long> {
    Mono<Comment> save(GameResult result);
    Mono<Comment> findById(Long id);
    Flux<Comment> findByBoardId(Long boardId);

}
