package com.ns.membership.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.MembershipEloRequest;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.TaskUseCase;
import com.ns.membership.Utils.JwtTokenProvider;
import com.ns.membership.Utils.Vault.VaultAdapter;
import com.ns.membership.Utils.jwtToken;
import com.ns.membership.entity.User;
import com.ns.membership.entity.dto.*;
import com.ns.membership.repository.UserR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements ApplicationRunner {

    private final UserR2dbcRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final VaultAdapter vaultAdapter;


    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> TaskProducerTemplate;

    private final TaskUseCase taskUseCase;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;

    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return TaskProducerTemplate.send(topic, key, task).then();
    }
    public Mono<User> create(UserCreateRequest request) {

        String encryptedPassword = vaultAdapter.encrypt(request.getPassword());

        // name, email의 중복 여부를 확인
        Flux<User> existingUsers = Flux.concat(
                userRepository.findByName(request.getName()),
                userRepository.findByEmail(request.getEmail())
        );

        return existingUsers.collectList()
                .flatMap(existingUserList -> {
                    if (existingUserList.isEmpty()) {
                        return userRepository.save(User.builder()
                                .account(request.getAccount())
                                .password(encryptedPassword)
                                .name(request.getName())
                                .email(request.getEmail())
                                        .elo(2000L)
                                        .code("")
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                .build());
                    } else {
                        return Mono.error(new RuntimeException("Duplicated data."));
                    }
                });
    }
    public Mono<UserResponse> login(UserRequest request) {
        String encryptedPassword = vaultAdapter.encrypt(request.getPassword());
        log.info("encrypt password : " + encryptedPassword);

        return userRepository.findByAccountAndPassword(request.getAccount(), encryptedPassword)
                .flatMap(user -> {
                    String id = user.getId().toString();
                    Mono<String> jwtMono = jwtTokenProvider.generateJwtToken(id);
                    Mono<String> refreshMono = jwtTokenProvider.generateRefreshToken(id);

                    return Mono.zip(jwtMono, refreshMono)
                            .flatMap(tuple -> {
                                String jwt = tuple.getT1();
                                String refreshToken = tuple.getT2();

                                user.setRefreshToken(refreshToken);

                                return userRepository.save(user)
                                        .map(savedUser -> UserResponse.of(savedUser))
                                        .flatMap(userResponse -> {
                                            userResponse.setJwtToken(jwt);
                                            return Mono.just(userResponse);
                                        });
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials id:"+request.getAccount()+" pw:"+request.getPassword())));
    }
    public Flux<User> findAll(){
        return userRepository.findAll().flatMap(user -> decryptUserData(user));
    }
    public Mono<User> findById(Long id){
        return userRepository.findById(id).flatMap(this::decryptUserData);
    }
    public Mono<Void> deleteById(Long id){
        return userRepository.deleteById(id)
                .then(Mono.empty());
    }
    public Mono<Void> deleteByName(String name) {
        return userRepository.deleteByName(name)
                .then(Mono.empty());
    }
    public Mono<User> update(Long id,String account, String name, String email,String password){
        String encryptedPassword = vaultAdapter.encrypt(password);

        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setEmail(email);
                    u.setAccount(account);
                    u.setPassword(encryptedPassword);
                    u.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(u);
                });
                // map으로 하면 Mono<Mono<User>>를 반환
    }
    private Mono<User> decryptUserData(User user) {
        String decryptedPassword = vaultAdapter.decrypt(user.getPassword());

        User decryptedUser = new User();
        decryptedUser.setId(user.getId());
        decryptedUser.setAccount(user.getAccount());
        decryptedUser.setEmail(user.getEmail());
        decryptedUser.setPassword(decryptedPassword);
        decryptedUser.setName(user.getName());
        decryptedUser.setElo(user.getElo());
        decryptedUser.setCode(user.getCode());
        return Mono.just(decryptedUser);
    }
    public Mono<jwtToken> refreshJwtToken(String refreshToken) {

        return jwtTokenProvider.validateJwtToken(refreshToken)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.empty();
                    }
                    return jwtTokenProvider.parseMembershipIdFromToken(refreshToken)
                            .flatMap(membershipId -> userRepository.findById(membershipId)
                                    .flatMap(membership -> {
                                        if (!membership.getRefreshToken().equals(refreshToken)) {
                                            return Mono.empty();
                                        }
                                        return jwtTokenProvider.generateJwtToken(String.valueOf(membershipId))
                                                .map(newJwtToken -> new jwtToken(
                                                        membershipId.toString(),
                                                        newJwtToken,
                                                        refreshToken
                                                ));
                                    })
                            );
                });
    }
    public Mono<Boolean> validateJwtToken(String token) {
        return jwtTokenProvider.validateJwtToken(token);
    }
    public Mono<User> getMembershipByJwtToken(String token) {

        return validateJwtToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.empty();
                    }
                    Long membershipId = jwtTokenProvider.parseMembershipIdFromToken(token).block();

                    return userRepository.findById(membershipId)
                            .flatMap(membership -> {
                                if (!membership.getRefreshToken().equals(token)) {
                                    return Mono.empty();
                                }
                                return Mono.just(membership);
                            });
                });
    }

    @Override
    public void run(ApplicationArguments args){

        this.TaskResponseConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();

                    List<SubTask> subTasks = task.getSubTaskList();

                    for(var subtask : subTasks){

                        log.info("TaskResponseConsumerTemplate received : "+subtask.toString());
                        try {
                            switch (subtask.getSubTaskName()) {
                                case "MatchUserNameByMembershipId":
                                    handleMatchUserName(task.getTaskID(), subtask);
                                    break;
                                case "PostUserNameByMembershipId":
                                    handlePostUserName(task.getTaskID(), subtask);
                                    break;
                                case "CommentUserNameByMembershipId":
                                    handleCommentUserName(task.getTaskID(), subtask);
                                    break;
                                case "MatchUserCodeByMembershipId":
                                    handleMatchUserCode(subtask);
                                    break;
                                case "MatchUserResponseByMembershipId":
                                    handleMatchUserResponse(task.getTaskID(), subtask);
                                    break;
                                case "RequestMembershipElo":
                                    handleResultMembershipElo(task.getTaskID(), subtask);
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
                        } catch (Exception e) {
                            log.error("Error processing subtask {}: {}", subtask.getSubTaskName(), e.getMessage());
                        }
                    }
                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();

        this.TaskRequestConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();
                    taskResults.put(task.getTaskID(),task);

                    if(taskResults.size() > MAX_TASK_RESULT_SIZE){
                        taskResults.clear();
                        log.info("taskResults clear.");
                    }

                })
                .doOnError(e -> log.error("Error receiving: " + e))
                .subscribe();
    }

    private void handleMatchUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    String nickname = user.getName();

                    List<SubTask> subTasks = new ArrayList<>();

                    subTasks.add(
                            taskUseCase.createSubTask("MatchUserNameByMembershipId",
                            String.valueOf(membershipId),
                            SubTask.TaskType.match,
                            SubTask.TaskStatus.success,
                            nickname));

                    return sendTask("task.match.request", taskUseCase.createTask(
                            taskId,
                            "Match Request - Nickname",
                            String.valueOf(membershipId),
                            subTasks));

                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handlePostUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    String nickname = user.getName();

                    List<SubTask> subTasks = new ArrayList<>();

                    subTasks.add(
                            taskUseCase.createSubTask("PostUserNameByMembershipId",
                                    String.valueOf(membershipId),
                                    SubTask.TaskType.post,
                                    SubTask.TaskStatus.success,
                                    nickname));

                    return sendTask("task.post.request", taskUseCase.createTask(
                            taskId,
                            "Post Request - Nickname",
                            String.valueOf(membershipId),
                            subTasks));

                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handleCommentUserName(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    String nickname = user.getName();

                    List<SubTask> subTasks = new ArrayList<>();

                    subTasks.add(
                            taskUseCase.createSubTask("CommentUserNameByMembershipId",
                                    String.valueOf(membershipId),
                                    SubTask.TaskType.post,
                                    SubTask.TaskStatus.success,
                                    nickname));

                    return sendTask("task.post.request", taskUseCase.createTask(
                            taskId,
                            "Comment Request - Nickname",
                            String.valueOf(membershipId),
                            subTasks));

                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handleMatchUserResponse(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        log.info("userRepso.findByid("+membershipId+") match");

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = new ArrayList<>();

                    subTasks.add(
                            taskUseCase.createSubTask("MatchUserResponseByMembershipId",
                                    String.valueOf(membershipId),
                                    SubTask.TaskType.match,
                                    SubTask.TaskStatus.success,
                                    convertToMatchUserResponse(user)));

                    log.info("user match : "+user);
                    return sendTask("task.match.request", taskUseCase.createTask(
                            taskId,
                            "Match Request - UserResponse",
                            String.valueOf(membershipId),
                            subTasks));

                })
                .doOnError(e -> log.error("Error handling MatchUserName for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }


    private void handleMatchUserCode(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    String spaceId = String.valueOf(subtask.getData());
                    user.setCode(spaceId);
                    log.info(user.getName()+" 사용자의 code는 "+spaceId+"입니다.");
                    return userRepository.save(user);
                })
                .doOnError(e -> log.error("Error handling MatchUserCode for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handleResultMembershipElo(String taskId, SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    List<SubTask> subTasks = new ArrayList<>();

                    MembershipEloRequest membershipEloRequest = MembershipEloRequest.builder()
                            .membershipId(user.getId())
                            .elo(user.getElo()).build();

                    subTasks.add(
                            taskUseCase.createSubTask("ResultMembershipElo",
                                    String.valueOf(membershipId),
                                    SubTask.TaskType.result,
                                    SubTask.TaskStatus.success,
                                    membershipEloRequest));

                    log.info("membershipEloRequest result : "+membershipEloRequest);
                    return sendTask("task.result.request", taskUseCase.createTask(
                            taskId,
                            "Result Request - membershipEloRequest",
                            String.valueOf(membershipId),
                            subTasks));

                })
                .doOnError(e -> log.error("Error handling handleResultMembershipElo for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handleResultUserEloUpdate(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());
        Long updatedElo = (Long) subtask.getData();

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    user.setElo(updatedElo);
                    return userRepository.save(user);
                })
                .doOnError(e -> log.error("Error handling ResultUserEloUpdate for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handleResultUserDodge(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    user.setCode("");
                    return userRepository.save(user);
                })
                .doOnError(e -> log.error("Error handling ResultUserDodge for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    public Mono<List<PostSummary>> getUserPosts(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();


        subTasks.add(
                taskUseCase.createSubTask("PostByMembershipId",
                        String.valueOf(membershipId),
                        SubTask.TaskType.post,
                        SubTask.TaskStatus.ready,
                        membershipId));

        Task task = taskUseCase.createTask(
                "Post Response",
                String.valueOf(membershipId),
                subTasks);

        return sendTask("task.post.response",task)
               .then(waitForUserPostsTaskResult(task.getTaskID()));
//                .thenMany(waitForTaskResult(task.getTaskID()))
//                .flatMap(result -> Flux.fromIterable(result.getSubTaskList())
//                        .filter(subTaskItem -> subTaskItem.getStatus().equals(SubTask.TaskStatus.success)))
//                .flatMap(subTaskItem -> {
//                    log.info(subTaskItem.toString());
//                    PostSummary postSummary = objectMapper.convertValue(subTaskItem.getData(), PostSummary.class);
//                    return Mono.just(postSummary);
//                }).collectList();

    }

//    private Flux<Task> waitForTaskResult(String taskId) {
//        return TaskRequestConsumerTemplate
//                .receiveAutoAck()
//                .filter(record -> taskId.equals(record.key()))
//                .map(record -> record.value())
//                .onErrorResume(throwable -> {
//                    log.error("Error occurred: {}", throwable.getMessage());
//                    return Flux.empty();
//                });
//    }

    private Mono<List<PostSummary>> waitForUserPostsTaskResult(String taskId) {
        return Mono.defer(() -> {
            return Mono.fromCallable(() -> {
                        while (true) {
                            Task resultTask = taskResults.get(taskId);
                            if (resultTask != null) {
                                List<PostSummary> postSummaries = convertToPostSummaries(resultTask);
                                return postSummaries;
                            }
                            Thread.sleep(500);
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofSeconds(3))
                    .onErrorResume(e -> Mono.error(new RuntimeException("waitForUserPostsTaskResult error : ", e)));
        });
    }

    private List<PostSummary> convertToPostSummaries(Task task) {
        return task.getSubTaskList().stream()
                .filter(subTaskItem -> subTaskItem.getStatus().equals(SubTask.TaskStatus.success))
                .map(subTaskItem -> objectMapper.convertValue(subTaskItem.getData(), PostSummary.class))
                .toList();
    }

    private MatchUserResponse convertToMatchUserResponse(User user){
        return MatchUserResponse.builder()
                .membershipId(user.getId())
                .elo(user.getElo())
                .name(user.getName())
                .spaceCode(user.getCode()).build();

    }

}
