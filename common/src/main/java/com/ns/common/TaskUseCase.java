package com.ns.common;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TaskUseCase {

    public SubTask createSubTask(String subTaskName, String membershipId, SubTask.TaskType taskType, SubTask.TaskStatus taskStatus, Object data){
        return SubTask.builder()
                        .subTaskName(subTaskName)
                        .membershipId(String.valueOf(membershipId))
                        .taskType(taskType)
                        .status(taskStatus)
                        .data(data)
                        .build();
    }

    public Task createTask(String taskName, String membershipId, List<SubTask> subTasks) {
        return createTask(null, taskName, membershipId, subTasks);
    }

    public Task createTask(String taskID, String taskName, String membershipId, List<SubTask> subTasks) {
        if (taskID == null || taskID.isEmpty()) {
            taskID = UUID.randomUUID().toString();
        }

        return Task.builder()
                .taskID(taskID)
                .taskName(taskName)
                .membershipId(membershipId)
                .subTaskList(subTasks)
                .build();
    }

}
