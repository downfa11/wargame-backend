package com.ns.feed.adapter.out.kafka;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.feed.application.port.out.TaskConsumerPort;
import com.ns.feed.application.port.out.TaskProducerPort;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskProducerAdapter implements TaskProducerPort {

    private final TaskConsumerPort taskConsumerPort;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    @Override
    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    @Override
    public Mono<String> getUserNameByPost(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListPostUserName(membershipId);
        Task task = createTaskGetUserName(membershipId, subTasks);

        return sendTask("task.membership.response", task)
                .then(taskConsumerPort.waitForGetUserNameTaskFeed(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Task createTaskGetUserName(Long membershipId, List<SubTask> subTasks) {
        return createTask("Post Response", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListPostUserName(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskPostUserName(membershipId));

        return subTasks;
    }

    private SubTask createSubTaskPostUserName(Long membershipId) {
        return createSubTask("PostUserName",
                String.valueOf(membershipId),
                SubTask.TaskType.post,
                SubTask.TaskStatus.ready,
                membershipId);
    }

    @Override
    public Mono<String> getUserNameByComment(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListCommentUserName(membershipId);
        Task task = createTaskCommentUserName(membershipId, subTasks);

        return sendTask("task.membership.response",task)
                .then(taskConsumerPort.waitForGetUserNameTaskComment(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Task createTaskCommentUserName(Long membershipId, List<SubTask> subTasks){
        return createTask(
                "Comment Response",
                String.valueOf(membershipId),
                subTasks);
    }

    private List<SubTask> createSubTaskListCommentUserName(Long membershipId){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskCommentUserName(membershipId));

        return subTasks;
    }

    private SubTask createSubTaskCommentUserName(Long membershipId){
        return createSubTask("CommentUserName",
                String.valueOf(membershipId),
                SubTask.TaskType.post,
                SubTask.TaskStatus.ready,
                membershipId);
    }
}
