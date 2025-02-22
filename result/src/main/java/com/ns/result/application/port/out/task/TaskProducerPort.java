package com.ns.result.application.port.out.task;

import com.ns.common.task.Task;
import reactor.core.publisher.Mono;

public interface TaskProducerPort {
    Mono<Void> sendTask(String topic, Task task);
}

