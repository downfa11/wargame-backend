package com.ns.feed.application.port.in.post;

import reactor.core.publisher.Mono;

public interface DeletePostUseCase {
    Mono<Void> deleteById(Long boardId);
}
