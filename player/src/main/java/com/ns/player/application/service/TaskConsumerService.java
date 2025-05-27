package com.ns.player.application.service;

import com.ns.common.task.Task;
import java.util.concurrent.ConcurrentHashMap;

import com.ns.player.application.port.out.task.TaskConsumerPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
