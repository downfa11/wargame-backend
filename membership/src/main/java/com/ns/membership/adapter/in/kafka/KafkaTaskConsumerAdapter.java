package com.ns.membership.adapter.in.kafka;

import static com.ns.common.task.TaskUseCase.createSubTask;
import static com.ns.common.task.TaskUseCase.createTask;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.common.task.SubTask;
import com.ns.common.task.SubTask.TaskStatus;
import com.ns.common.task.SubTask.TaskType;
import com.ns.common.task.Task;
import com.ns.membership.application.port.out.FindUserPort;
import com.ns.membership.application.port.out.TaskProducerPort;
import com.ns.membership.application.service.TaskConsumerService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class KafkaTaskConsumerAdapter implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate;

    private final FindUserPort findUserPort;
    private final TaskProducerPort taskProducerPort;
    private final TaskConsumerService taskConsumerService;

    @Override
    public void run(ApplicationArguments args){
        doTaskResponseConsumerTemplate();
        doTaskRequestConsumerTemplate();
    }

    private void doTaskResponseConsumerTemplate(){
        this.taskResponseConsumerTemplate
                .receive()
                .doOnNext(r -> {
                    Task task = r.value();

                    for(var subtask : task.getSubTaskList()){
                        mapSubTaskToMembership(task.getTaskID(), subtask);
                        log.info("TaskResponseConsumerTemplate received : "+subtask);
                    }
                    r.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error doTaskResponseConsumerTemplate: " + e))
                .subscribe();
    }

    private void mapSubTaskToMembership(String taskId, SubTask subtask){
        switch (subtask.getSubTaskName()) {
            case "MatchUserName":
                handleMatchUserName(taskId, subtask);
                break;
            case "PostUserName":
                handlePostUserName(taskId, subtask);
                break;
            case "CommentUserName":
                handleCommentUserName(taskId, subtask);
                break;
            default:
                log.warn("Unknown subtask: {}", subtask.getSubTaskName());
                break;
        }
    }

    private void doTaskRequestConsumerTemplate(){
        this.taskRequestConsumerTemplate
                .receive()
                .doOnNext(record -> {
                    taskConsumerService.handleTaskResponse(record.value());
                    record.receiverOffset().acknowledge();
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

    private void handleMatchUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        findUserPort.findUserByMembershipId(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = createSubTaskListMatchUserName(membershipId, user.getName());
                    return taskProducerPort.sendTask("task.match.request", createTaskMatchUserName(taskId, membershipId, subTasks));
                })
                .doOnError(e -> log.error("Error handleMatchUserName {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskMatchUserName(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Match Request - Nickname", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchUserName(Long membershipId, String nickname){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserName(membershipId, nickname));

        return subTasks;
    }

    private SubTask createSubTaskMatchUserName(Long membershipId, String nickname){
        return createSubTask("MatchUserName",
                String.valueOf(membershipId),
                TaskType.match,
                TaskStatus.success,
                nickname);
    }

    private void handlePostUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        findUserPort.findUserByMembershipId(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = createSubTaskListPostUserName(membershipId, user.getName());
                    return taskProducerPort.sendTask("task.post.request",
                            createTaskPostUserName(taskId, membershipId, subTasks));

                })
                .doOnError(e -> log.error("Error handlePostUserName {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskPostUserName(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Post Request - Nickname", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListPostUserName(Long membershipId, String nickname){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskPostUserName(membershipId, nickname));
        return subTasks;
    }

    private SubTask createSubTaskPostUserName(Long membershipId, String nickname){
        return createSubTask("PostUserName",
                String.valueOf(membershipId),
                TaskType.post,
                TaskStatus.success,
                nickname);
    }

    private void handleCommentUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        findUserPort.findUserByMembershipId(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = createSubTaskListCommentUserName(membershipId, user.getName());
                    return taskProducerPort.sendTask("task.post.request", createTaskCommentUserName(taskId, membershipId, subTasks));
                })
                .doOnError(e -> log.error("Error handleCommentUserName {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskCommentUserName(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Comment Request - Nickname", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListCommentUserName(Long membershipId, String nickname){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskCommentUserName(membershipId, nickname));
        return subTasks;
    }

    private SubTask createSubTaskCommentUserName(Long membershipId, String nickname){
        return createSubTask("CommentUserName",
                String.valueOf(membershipId),
                TaskType.post,
                TaskStatus.success,
                nickname);
    }


}
