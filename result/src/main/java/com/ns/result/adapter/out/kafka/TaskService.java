package com.ns.result.adapter.out.kafka;

import com.ns.common.task.Task;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskService {

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
}
