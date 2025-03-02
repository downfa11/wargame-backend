package com.ns.membership.usecase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.membership.application.port.out.TaskConsumerPort;
import com.ns.membership.application.service.TaskConsumerService;
import com.ns.membership.dto.PostSummary;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
    void 피드_서비스로부터_사용자가_작성한_게시글_목록을_받아오는_메서드() {
        // given
        List<PostSummary> summaries = Arrays.asList(PostSummary.builder().build(), PostSummary.builder().build());

        SubTask mockSubTask = mock(SubTask.class);
        when(mockSubTask.getData()).thenReturn(summaries);

        Task mockTask = mock(Task.class);
        when(mockTask.getTaskID()).thenReturn(taskName);
        when(mockTask.getSubTaskList()).thenReturn(Collections.singletonList(mockSubTask));

        when(taskConsumerPort.getTaskResults(any())).thenReturn(mockTask);

        // when
        Mono<List<PostSummary>> result = taskConsumerService.waitForUserPostsTaskResult(taskName);

        // then
        StepVerifier.create(result)
                .expectNext(summaries)
                .verifyComplete();

        verify(taskConsumerPort, times(1)).getTaskResults(taskName);
    }

    @Test
    void 피드_서비스로부터_사용자가_작성한_게시글_목록을_받아올때_요청의_대기시간을_초과한_경우() {
        // given
        SubTask mockSubTask = mock(SubTask.class);
        List<PostSummary> subTaskData = Arrays.asList(PostSummary.builder().build());
        when(mockSubTask.getData()).thenReturn(subTaskData);

        Task mockTask = mock(Task.class);
        when(mockTask.getTaskID()).thenReturn(taskName);
        when(mockTask.getSubTaskList()).thenReturn(Collections.singletonList(mockSubTask));

        when(taskConsumerPort.getTaskResults(any())).thenReturn(mockTask);

        // when
        Mono<List<PostSummary>> result = taskConsumerService.waitForUserPostsTaskResult(taskName)
                .timeout(Duration.ofSeconds(1));

        // then
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof TimeoutException &&
                        throwable.getMessage().contains("Timeout waitForUserPostsTaskResult for taskId: " + taskName))
                .verify();

        verify(taskConsumerPort, atLeast(1)).getTaskResults(taskName);
    }
}
