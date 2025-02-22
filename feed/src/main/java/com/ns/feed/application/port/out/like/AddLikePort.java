package com.ns.feed.application.port.out.like;

import reactor.core.publisher.Mono;

public interface AddLikePort {
    Mono<Long> addLike(Long boardId, String userId);
}
