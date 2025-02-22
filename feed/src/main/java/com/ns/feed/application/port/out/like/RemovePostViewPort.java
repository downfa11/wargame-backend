package com.ns.feed.application.port.out.like;

import reactor.core.publisher.Mono;

public interface RemovePostViewPort {
    Mono<Long> removePostView(Long boardId);
}
