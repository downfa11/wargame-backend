package com.ns.feed.application.port.in.comment;

import com.ns.feed.dto.CommentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindCommentUseCase {
    Mono<CommentResponse> findByCommentId(Long commentId);
    Flux<CommentResponse> findAllByBoardId(Long boardId);
}
