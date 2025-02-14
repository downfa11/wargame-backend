package com.ns.match.application.port.out.task;

import com.ns.common.task.Task;
import reactor.core.publisher.Mono;

public interface TaskConsumerPort {
    Task getTaskResults(String taskId);

    Mono<Long> waitForUserResponseTaskResult(String taskId);
    Mono<Boolean> waitForUserHasCodeTaskResult(String taskId);
    Mono<String> waitForUserNameTaskResult(String taskId);
}
