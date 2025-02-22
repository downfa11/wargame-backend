package com.ns.result.application.port.out.task;

import com.ns.common.task.Task;

public interface TaskConsumerPort {
    Task getTaskResults(String taskId);
}
