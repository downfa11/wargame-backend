package com.ns.feed.application.port.out.like;

import reactor.core.publisher.Mono;

public interface IncreasePostViewPort {
    Mono<Long> incrPostViews(Long boardId);
}
