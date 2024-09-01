package com.ns.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTask {
    private String membersrhipId;
    private String subTaskName;
    private String taskType;
    private String status; //ready, success, fail
    private Object data;
}