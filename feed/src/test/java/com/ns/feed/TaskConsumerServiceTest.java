package com.ns.feed;

import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.feed.application.port.out.TaskConsumerPort;
import com.ns.feed.application.service.TaskConsumerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskConsumerServiceTest {

    @InjectMocks
    private TaskConsumerService taskConsumerService;

    @Mock
    private TaskConsumerPort taskConsumerPort;

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


    @Test
    void 회원_서비스로부터_사용자의_이름을_받아오는_메서드() {
        // given
        SubTask mockSubTask = mock(SubTask.class);
        Object subtaskData = "name";
        when(mockSubTask.getData()).thenReturn(subtaskData);

        Task mockTask = mock(Task.class);
        when(mockTask.getTaskID()).thenReturn(taskName);
        when(mockTask.getSubTaskList()).thenReturn(Arrays.asList(mockSubTask));

        when(taskConsumerPort.getTaskResults(taskName)).thenReturn(mockTask);

        // when
        Mono<String> result = taskConsumerService.waitForGetUserNameTaskFeed(taskName);

        // then
        StepVerifier.create(result)
                .expectNext((String) subtaskData)
                .verifyComplete();

        verify(taskConsumerPort, times(1)).getTaskResults(taskName);
    }

    @Test
    void 회원_서비스로부터_사용자의_이름을_받아올때_요청의_대기시간을_초과한_경우() {
        // given
        SubTask mockSubTask = mock(SubTask.class);
        when(mockSubTask.getData()).thenReturn(subTaskData);

        Task mockTask = mock(Task.class);
        when(mockTask.getTaskID()).thenReturn(taskName);
        when(mockTask.getSubTaskList()).thenReturn(Arrays.asList(mockSubTask));

        when(taskConsumerPort.getTaskResults(any())).thenReturn(mockTask);

        // when
        Mono<String> result = taskConsumerService.waitForGetUserNameTaskFeed(taskName);

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().contains("Timeout waitForGetUserNameTaskFeed for taskId: taskId1"))
                .verify();

        verify(taskConsumerPort, atLeast(1)).getTaskResults(taskName);
    }
}
