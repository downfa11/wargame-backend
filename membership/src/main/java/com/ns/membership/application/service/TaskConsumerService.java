package com.ns.membership.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.membership.application.port.out.TaskConsumerPort;
import com.ns.membership.dto.PostSummary;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TaskConsumerService implements TaskConsumerPort {

    private final ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int MAX_TASK_RESULT_SIZE = 5000;

    @Override
    public Task getTaskResults(String taskId) {
        return taskResults.get(taskId);
    }


    // 4. Feed 서비스로부터 원하는 데이터를 수신 ***
    @Override
    public Mono<List<PostSummary>> waitForUserPostsTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(this::convertToPostSummaries)
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserPostsTaskResult for taskId " + taskId)));
    }

    private List<PostSummary> convertToPostSummaries(Task task) {
        return task.getSubTaskList().stream()
                .filter(subTaskItem -> subTaskItem.getStatus().equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> objectMapper.convertValue(subTaskItem.getData(), PostSummary.class))
                .toList();
    }

    public void handleTaskResponse(Task task) {
        taskResults.put(task.getTaskID(), task);

        if (taskResults.size() > MAX_TASK_RESULT_SIZE) {
            taskResults.clear();
        }
    }
}