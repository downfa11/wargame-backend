package com.ns.match.application.port.out;

import reactor.core.publisher.Mono;

public interface ProcessMatchQueuePort {
    Mono<Void> process();
    Mono<Void> processQueueInRange(int maxProcessCount);
}
