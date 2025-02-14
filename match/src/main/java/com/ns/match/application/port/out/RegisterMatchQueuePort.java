package com.ns.match.application.port.out;

import reactor.core.publisher.Mono;

public interface RegisterMatchQueuePort {
    Mono<String> registerMatchQueue(final String queue, final Long userId);
}
