package com.ns.feed.application.port.in;

import reactor.core.publisher.Mono;

public interface UpdateLikeUseCase {
    Mono<Long> updateLikes(Long userId, Long boardId);
}
