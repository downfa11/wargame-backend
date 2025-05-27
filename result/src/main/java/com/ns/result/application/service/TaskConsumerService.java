package com.ns.result.application.service;


import com.ns.common.task.Task;
import com.ns.result.application.port.out.task.TaskConsumerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TaskConsumerService implements TaskConsumerPort {

    private final ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;

    @Override
    public Task getTaskResults(String taskId) {
        return taskResults.get(taskId);
    }


    public void handleTaskResponse(Task task) {
        taskResults.put(task.getTaskID(), task);

        if (taskResults.size() > MAX_TASK_RESULT_SIZE) {
            taskResults.clear();
        }
    }

}
