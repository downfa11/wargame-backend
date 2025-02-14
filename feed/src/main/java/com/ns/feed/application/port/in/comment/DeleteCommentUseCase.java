package com.ns.feed.application.port.in.comment;

import reactor.core.publisher.Mono;

public interface DeleteCommentUseCase {
    Mono<Void> delete(Long commentId);
}
