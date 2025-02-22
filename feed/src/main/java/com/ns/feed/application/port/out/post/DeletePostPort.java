package com.ns.feed.application.port.out.post;

import reactor.core.publisher.Mono;

public interface DeletePostPort {
    Mono<Void> deletePost(Long boardId);
}
