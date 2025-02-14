package com.ns.feed.application.port.out.like;

import reactor.core.publisher.Mono;

public interface FindLikePort {
    Mono<Long> getLikesCount(Long boardId);
    Mono<Boolean> isUserLiked(Long boardId, Long userId);
}
