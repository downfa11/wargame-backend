package com.ns.common.task;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TaskUseCase {

    public static SubTask createSubTask(String subTaskName, String membershipId, SubTask.TaskType taskType, SubTask.TaskStatus taskStatus, Object data){
        return SubTask.builder()
                        .subTaskName(subTaskName)
                        .membershipId(String.valueOf(membershipId))
                        .taskType(taskType)
                        .status(taskStatus)
                        .data(data)
                        .build();
    }

    public static Task createTask(String taskName, String membershipId, List<SubTask> subTasks) {
        return createTask(null, taskName, membershipId, subTasks);
    }

    public static Task createTask(String taskID, String taskName, String membershipId, List<SubTask> subTasks) {
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
