package com.ns.match.application.port.in;

import reactor.core.publisher.Mono;

public interface RegisterMatchQueueUseCase {
    Mono<Boolean> registerMatchQueue(String queue, Long userId);
}
