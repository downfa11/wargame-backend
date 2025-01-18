package com.ns.common.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {
    private String membershipId;
    private String subTaskName;

    private Object data;

    public enum TaskType{
        membership, post, match, result
    }
    private TaskType taskType;

    public enum TaskStatus{
        ready, success, fail
    }
    private TaskStatus status;
}