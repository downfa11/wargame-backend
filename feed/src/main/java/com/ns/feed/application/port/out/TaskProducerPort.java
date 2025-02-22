package com.ns.feed.application.port.out;

import com.ns.common.task.Task;
import reactor.core.publisher.Mono;

public interface TaskProducerPort {
    Mono<Void> sendTask(String topic, Task task);

    Mono<String> getUserNameByPost(Long membershipId);
    Mono<String> getUserNameByComment(Long membershipId);
}

