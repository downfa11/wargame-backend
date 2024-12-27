package com.ns.membership.service;

import static com.ns.common.TaskUseCase.createSubTask;
import static com.ns.common.TaskUseCase.createTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.MembershipEloRequest;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.membership.entity.User;
import com.ns.membership.entity.dto.MatchUserResponse;
import com.ns.membership.entity.dto.PostSummary;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService implements ApplicationRunner {

    private final ReactiveKafkaConsumerTemplate<String, Task> taskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> taskResponseConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final ObjectMapper objectMapper;


    private final UserService userService;
    private ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;

    public Mono<Void> sendTask(String topic, Task task){
        return taskProducerTemplate.send(topic, task.getTaskID(), task)
                .then();
    }

    @Override
    public void run(ApplicationArguments args){
        doTaskResponseConsumerTemplate();
        doTaskRequestConsumerTemplate();
    }

    private void doTaskResponseConsumerTemplate(){
         taskResponseConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();
                    List<SubTask> subTasks = task.getSubTaskList();

                    for(var subtask : subTasks){
                        mapSubTaskToMembership(task.getTaskID(), subtask);
                        log.info("TaskResponseConsumerTemplate received : "+subtask);
                    }
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

    private void doTaskRequestConsumerTemplate(){
        taskRequestConsumerTemplate
                .receiveAutoAck()
                .doOnNext(record -> {
                    log.info("received: "+record);
                    handleTaskRequest(record.value());
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

    private void mapSubTaskToMembership(String taskId, SubTask subtask){
        switch (subtask.getSubTaskName()) {
            case "MatchUserNameByMembershipId":
                handleMatchUserName(taskId, subtask);
                break;
            case "PostUserNameByMembershipId":
                handlePostUserName(taskId, subtask);
                break;
            case "CommentUserNameByMembershipId":
                handleCommentUserName(taskId, subtask);
                break;
            case "MatchUserCodeByMembershipId":
                handleMatchUserCode(subtask);
                break;
            case "MatchUserHasCodeByMembershipId":
                handleMatchUserHasCode(taskId, subtask);
                break;
            case "MatchUserResponseByMembershipId":
                handleMatchUserResponse(taskId, subtask);
                break;
            case "RequestMembershipElo":
                handleResultMembershipElo(taskId, subtask);
                break;
            case "ResultUserEloUpdate":
                handleResultUserEloUpdate(subtask);
                break;
            case "ResultUserDodge":
                handleResultUserDodge(subtask);
                break;
            default:
                log.warn("Unknown subtask: {}", subtask.getSubTaskName());
                break;
        }
    }

    private void handleTaskRequest(Task task){
        if (taskResults.size() >= MAX_TASK_RESULT_SIZE) {
            taskResults.clear();
            log.info("taskResults cleared due to exceeding the maximum size.");
        }

        taskResults.put(task.getTaskID(), task);
    }

    private void handleMatchUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userService.getMembershipById(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = createSubTaskListMatchUserNameByMembershipId(membershipId, user.getName());
                    return sendTask("task.match.request", createTaskMatchUserNameByMembershipId(taskId, membershipId, subTasks));
                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskMatchUserNameByMembershipId(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Match Request - Nickname", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchUserNameByMembershipId(Long membershipId, String nickname){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserNameByMembershipId(membershipId, nickname));

        return subTasks;
    }

    private SubTask createSubTaskMatchUserNameByMembershipId(Long membershipId, String nickname){
        return createSubTask("MatchUserNameByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.success,
                nickname);
    }

    private void handlePostUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userService.getMembershipById(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = createSubTaskListPostUserNameByMembershipId(membershipId, user.getName());
                    return sendTask("task.post.request",
                            createTaskPostUserNameByMembershipId(taskId, membershipId, subTasks));

                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskPostUserNameByMembershipId(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Post Request - Nickname", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListPostUserNameByMembershipId(Long membershipId, String nickname){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskPostUserNameByMembershipId(membershipId, nickname));
        return subTasks;
    }

    private SubTask createSubTaskPostUserNameByMembershipId(Long membershipId, String nickname){
        return createSubTask("PostUserNameByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.post,
                SubTask.TaskStatus.success,
                nickname);
    }

    private void handleCommentUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userService.getMembershipById(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = createSubTaskListCommentUserNameByMembershipId(membershipId, user.getName());
                    return sendTask("task.post.request", createTaskCommentUserNameByMembershipId(taskId, membershipId, subTasks));
                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskCommentUserNameByMembershipId(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Comment Request - Nickname", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListCommentUserNameByMembershipId(Long membershipId, String nickname){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskCommentUserNameByMembershipId(membershipId, nickname));
        return subTasks;
    }

    private SubTask createSubTaskCommentUserNameByMembershipId(Long membershipId, String nickname){
        return createSubTask("CommentUserNameByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.post,
                SubTask.TaskStatus.success,
                nickname);
    }

    private void handleMatchUserResponse(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());
        log.info("userRepso.findByid("+membershipId+") match");

        userService.getMembershipById(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = createSubTaskListMatchUserResponseByMembershipId(user, membershipId);
                    return sendTask("task.match.request",createTaskMatchUserResponseByMembershipId(taskId,membershipId,subTasks));
                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private Task createTaskMatchUserResponseByMembershipId(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Match Request - UserResponse", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchUserResponseByMembershipId(User user, Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserResponseByMembershipId(user, membershipId));
        return subTasks;
    }
    private SubTask createSubTaskMatchUserResponseByMembershipId(User user, Long membershipId){
        return createSubTask("MatchUserResponseByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.success,
                convertToMatchUserResponse(user));
    }

    private void handleMatchUserHasCode(String taskId, SubTask subTask){
        Long membershipId = Long.parseLong(subTask.getMembershipId());

        userService.getMembershipById(membershipId)
                .flatMap(user -> {
                    Boolean hasCode = user.getCode().equals("");
                    List<SubTask> subTasks = createSubTaskListMatchUserHasCodeByMembershipId(membershipId, hasCode);
                    return sendTask("task.match.request", createTaskMatchUserHasCodeByMembershipId(taskId, membershipId, subTasks));
                });
    }

    private Task createTaskMatchUserHasCodeByMembershipId(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Match Request - UserHasCode", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListMatchUserHasCodeByMembershipId(Long membershipId, Boolean hasCode) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskMatchUserHasCodeByMembershipId(membershipId, hasCode));
        return subTasks;
    }
    private SubTask createSubTaskMatchUserHasCodeByMembershipId(Long membershipId, Boolean hasCode){
        return createSubTask("MatchUserHasCodeByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.match,
                SubTask.TaskStatus.success,
                hasCode);
    }

    private void handleMatchUserCode(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());
        String spaceId = String.valueOf(subtask.getData());

        userService.updateSpaceId(membershipId, spaceId)
                .doOnError(e -> log.error("Error handling MatchUserCode for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handleResultMembershipElo(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userService.getMembershipById(membershipId)
                .flatMap(user -> {
                    MembershipEloRequest membershipEloRequest = createMembershipEloRequest(user);
                    List<SubTask> subTasks = createSubTaskListResultMembershipElo(membershipId, membershipEloRequest);

                    log.info("membershipEloRequest result : "+membershipEloRequest);
                    return sendTask("task.result.request", createTaskMembershipEloRequest(taskId, membershipId, subTasks));
                })
                .doOnError(e -> log.error("Error handling handleResultMembershipElo for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private MembershipEloRequest createMembershipEloRequest(User user){
        return MembershipEloRequest.builder()
                .membershipId(user.getId())
                .elo(user.getElo()).build();
    }

    private List<SubTask> createSubTaskListResultMembershipElo(Long membershipId, MembershipEloRequest membershipEloRequest) {
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskResultMembershipElo(membershipId, membershipEloRequest));
        return subTasks;
    }

    private SubTask createSubTaskResultMembershipElo(Long membershipId, MembershipEloRequest membershipEloRequest){
        return createSubTask("ResultMembershipElo",
                String.valueOf(membershipId),
                SubTask.TaskType.result,
                SubTask.TaskStatus.success,
                membershipEloRequest);
    }

    private Task createTaskMembershipEloRequest(String taskId, Long membershipId, List<SubTask> subTasks){
        return createTask(taskId, "Result Request - membershipEloRequest", String.valueOf(membershipId), subTasks);
    }

    private void handleResultUserEloUpdate(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());
        Long updatedElo = (Long) subtask.getData();
        userService.modifyMemberEloByEvent(String.valueOf(membershipId), updatedElo);
    }

    private void handleResultUserDodge(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userService.updateSpaceId(membershipId, "")
                .doOnError(e -> log.error("Error handling ResultUserDodge for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private MatchUserResponse convertToMatchUserResponse(User user){
        return MatchUserResponse.builder()
                .membershipId(user.getId())
                .elo(user.getElo())
                .name(user.getName())
                .spaceCode(user.getCode()).build();
    }

    public Mono<List<PostSummary>> getUserPosts(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListPostByMembershipId(membershipId);
        Task task = createTaskPostByMembershipId(membershipId, subTasks);

        return sendTask("task.post.response",task)
                .then(waitForUserPostsTaskResult(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Task createTaskPostByMembershipId(Long membershipId, List<SubTask> subTasks){
        return createTask(
                "Post Response",
                String.valueOf(membershipId),
                subTasks);
    }
    private List<SubTask> createSubTaskListPostByMembershipId(Long membershipId){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubPostByMembershipId(membershipId));

        return subTasks;
    }

    private SubTask createSubPostByMembershipId(Long membershipId){
        return createSubTask("PostByMembershipId",
                        String.valueOf(membershipId),
                        SubTask.TaskType.post,
                        SubTask.TaskStatus.ready,
                        membershipId);
    }

    private Mono<List<PostSummary>> waitForUserPostsTaskResult(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                        .map(tick -> taskResults.get(taskId))
                        .filter(Objects::nonNull)
                        .take(1)
                        .map(this::convertToPostSummaries)
                        .next()
                        .timeout(Duration.ofSeconds(3))
                        .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForUserPostsTaskResult for taskId " + taskId)));
    }


    private List<PostSummary> convertToPostSummaries(Task task) {
        return task.getSubTaskList().stream()
                .filter(subTaskItem -> subTaskItem.getStatus().equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> objectMapper.convertValue(subTaskItem.getData(), PostSummary.class))
                .toList();
    }

}
