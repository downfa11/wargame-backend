package com.ns.match.application.port.out.task;

import com.ns.common.PlayerQuery;
import com.ns.common.task.Task;
import reactor.core.publisher.Mono;

public interface TaskConsumerPort {
    Task getTaskResults(String taskId);

    Mono<PlayerQuery> waitForPlayerQuery(String taskId);
}
