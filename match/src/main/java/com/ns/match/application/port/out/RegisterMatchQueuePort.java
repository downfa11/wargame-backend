package com.ns.match.application.port.out;

import reactor.core.publisher.Mono;

public interface RegisterMatchQueuePort {
    Mono<Boolean> registerMatchQueue(final Long userId);
}
