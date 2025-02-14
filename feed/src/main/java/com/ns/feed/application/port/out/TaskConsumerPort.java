package com.ns.feed.application.port.out;

import com.ns.common.task.Task;
import reactor.core.publisher.Mono;

public interface TaskConsumerPort {
    Task getTaskResults(String taskId);
    Mono<String> waitForGetUserNameTaskFeed(String taskId);
    Mono<String> waitForGetUserNameTaskComment(String taskId);
}
