package com.ns.result;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.result.application.service.TaskConsumerService;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TaskConsumerServiceTest {

    @InjectMocks
    private TaskConsumerService taskConsumerService;


    private String taskName = "taskId";
    private String subTaskData = "RequestUserName";

    @Test
    void HandleTaskResponse() {
        // given
        SubTask mockSubTask = mock(SubTask.class);
        when(mockSubTask.getData()).thenReturn(subTaskData);

        Task mockTask = mock(Task.class);
        when(mockTask.getTaskID()).thenReturn(taskName);
        when(mockTask.getSubTaskList()).thenReturn(Arrays.asList(mockSubTask));

        // when
        taskConsumerService.handleTaskResponse(mockTask);
        Task result = taskConsumerService.getTaskResults(taskName);

        // then
        assert result != null;
        assert result.getTaskID().equals(taskName);
        assert result.getSubTaskList().size() == 1;
        assert result.getSubTaskList().get(0).getData().equals(subTaskData);
    }

    @Test
    void TaskResults가_꽉_찬_경우() {
        // given
        SubTask mockSubTask = mock(SubTask.class);

        // when
        for (int i = 0; i <= 5001; i++) {
            Task task = Task.builder()
                    .taskID(taskName + i)
                    .subTaskList(Arrays.asList(mockSubTask))
                    .build();
            taskConsumerService.handleTaskResponse(task);
        }

        // then
        assertNull(taskConsumerService.getTaskResults("taskId5000"));
        assertNull(taskConsumerService.getTaskResults("taskId1"));
        assertNotNull(taskConsumerService.getTaskResults("taskId5001"));
    }
}
