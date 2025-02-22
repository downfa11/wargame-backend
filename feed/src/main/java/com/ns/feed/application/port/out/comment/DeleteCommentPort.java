package com.ns.feed.application.port.out.comment;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeleteCommentPort {
    Flux<Void> deleteByBoardId(Long boardId);
    Mono<Void> deleteByCommentId(Long commentId);
}
