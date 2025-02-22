package com.ns.feed.application.port.out.like;

import reactor.core.publisher.Mono;

public interface RemoveLikePort {
    Mono<Long> removeLike(Long boardId, Long userId);
    Mono<Long> removeLikeAllByPostId(Long boardId);
}
