package com.ns.match.application.port.in;

import reactor.core.publisher.Mono;

public interface CancleMatchQueueUseCase {
    Mono<Void> cancelMatchQueue(Long userId);
}
