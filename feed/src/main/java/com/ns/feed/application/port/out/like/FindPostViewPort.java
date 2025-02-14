package com.ns.feed.application.port.out.like;

import reactor.core.publisher.Mono;

public interface FindPostViewPort {
    Mono<Long> getPostViews(Long boardId);
}
