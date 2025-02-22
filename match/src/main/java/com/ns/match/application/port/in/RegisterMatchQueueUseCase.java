package com.ns.match.application.port.in;

import reactor.core.publisher.Mono;

public interface RegisterMatchQueueUseCase {
    Mono<String> registerMatchQueue(String queue, Long userId);
}
