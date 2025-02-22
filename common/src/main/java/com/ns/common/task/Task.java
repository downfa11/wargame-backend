package com.ns.common.task;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Task {
    private String taskID;
    private String taskName;
    private String membershipId;
    private List<SubTask> subTaskList;
}