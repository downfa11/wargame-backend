package com.ns.feed.application.service;

import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.feed.application.port.out.TaskConsumerPort;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TaskConsumerService implements TaskConsumerPort {

    private final ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;

    @Override
    public Task getTaskResults(String taskId) {
        return taskResults.get(taskId);
    }

    @Override
    public Mono<String> waitForGetUserNameTaskFeed(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(task -> {
                    SubTask subTask = task.getSubTaskList().get(0);
                    return String.valueOf(subTask.getData());
                })
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForGetUserNameTaskFeed for taskId: " + taskId)));
    }

    @Override
    public Mono<String> waitForGetUserNameTaskComment(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(task -> {
                    SubTask subTask = task.getSubTaskList().get(0);
                    return String.valueOf(subTask.getData());
                })
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserPostsTaskComment for taskId " + taskId)));
    }

    public void handleTaskResponse(Task task) {
        taskResults.put(task.getTaskID(), task);

        if (taskResults.size() > MAX_TASK_RESULT_SIZE) {
            taskResults.clear();
        }
    }
}