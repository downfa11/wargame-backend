package com.ns.membership.adapter.out.kafka;


import com.ns.common.task.Task;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;
    private ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();

    private final int MAX_TASK_RESULT_SIZE = 5000;

    public Task getTaskResults(String taskId){
        return taskResults.get(taskId);
    }

    public void handleTaskRequest(Task task){
        taskResults.put(task.getTaskID(), task);

        if (taskResults.size() > MAX_TASK_RESULT_SIZE) {
            taskResults.clear();
        }
    }

    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }
}

