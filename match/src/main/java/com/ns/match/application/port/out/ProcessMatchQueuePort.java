package com.ns.match.application.port.out;

import java.util.List;
import reactor.core.publisher.Mono;

public interface ProcessMatchQueuePort {
    Mono<Void> process(String queueKey);
    Mono<Void> processAllQueue(List<String> queues);
    Mono<Void> processQueue(String queue);
    Mono<Void> processQueueInRange(String queue, int maxProcessCount);
}
