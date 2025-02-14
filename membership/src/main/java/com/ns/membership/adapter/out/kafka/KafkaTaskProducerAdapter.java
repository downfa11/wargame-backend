package com.ns.membership.adapter.out.kafka;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.membership.application.port.out.TaskConsumerPort;
import com.ns.membership.application.port.out.TaskProducerPort;
import com.ns.membership.application.service.TaskConsumerService;
import com.ns.membership.dto.PostSummary;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskProducerAdapter implements TaskProducerPort {

    private final TaskConsumerPort taskConsumerPort;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    @Override
    public Mono<Void> sendTask(String topic, Task task) {
        log.info("send [" + topic + "]: " + task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    // 해당 사용자가 작성한 게시글 목록(PostSummary)를 조회
    @Override
    public Mono<List<PostSummary>> getUserPosts(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListPostByMembershipId(membershipId);
        Task task = createTaskPostByMembershipId(membershipId, subTasks);

        return sendTask("task.post.response",task)
                .then(taskConsumerPort.waitForUserPostsTaskResult(task.getTaskID())
                .subscribeOn(Schedulers.boundedElastic()));
    }

    // 1. Feed 서비스로 전달하기 위한 Task를 생성
    private Task createTaskPostByMembershipId(Long membershipId, List<SubTask> subTasks) {
        return createTask("Post Response", String.valueOf(membershipId), subTasks);
    }

    // 2. Feed 서비스로 전달하기 위한 List<subTask>를 생성
    private List<SubTask> createSubTaskListPostByMembershipId(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubPostByMembershipId(membershipId));
        return subTasks;
    }

    // 3. Feed 서비스로 전달하기 위한 SubTask를 생성
    private SubTask createSubPostByMembershipId(Long membershipId) {
        return createSubTask("PostByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.post,
                SubTask.TaskStatus.ready,
                membershipId);
    }

}
