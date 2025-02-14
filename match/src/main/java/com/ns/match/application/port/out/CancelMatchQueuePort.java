package com.ns.match.application.port.out;

import reactor.core.publisher.Mono;

public interface CancelMatchQueuePort {
    Mono<Void> cancelMatchQueue(Long userId);
}
