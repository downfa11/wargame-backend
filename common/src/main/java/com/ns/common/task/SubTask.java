package com.ns.common.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubTask {
    private String membershipId;
    private String subTaskName;

    private Object data;

    public enum TaskType{
        membership, post, match, result, player
    }
    private TaskType taskType;

    public enum TaskStatus{
        ready, success, fail
    }
    private TaskStatus status;
}