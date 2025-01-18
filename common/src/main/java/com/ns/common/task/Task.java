package com.ns.common.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {
    private String taskID;
    private String taskName;
    private String membershipId;
    private List<SubTask> subTaskList;
}