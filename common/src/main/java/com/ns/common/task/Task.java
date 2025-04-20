package com.ns.common.task;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    private String taskID;
    private String taskName;
    private String membershipId;
    private List<SubTask> subTaskList;
}