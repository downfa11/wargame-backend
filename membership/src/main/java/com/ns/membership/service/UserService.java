package com.ns.membership.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.TaskUseCase;
import com.ns.membership.Utils.JwtTokenProvider;
import com.ns.membership.Utils.Vault.VaultAdapter;
import com.ns.membership.Utils.jwtToken;
import com.ns.membership.entity.User;
import com.ns.membership.entity.dto.PostSummary;
import com.ns.membership.entity.dto.UserCreateRequest;
import com.ns.membership.entity.dto.UserRequest;
import com.ns.membership.entity.dto.UserResponse;
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

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements ApplicationRunner {

    private final UserR2dbcRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final VaultAdapter vaultAdapter;


    private final ReactiveKafkaConsumerTemplate<String, Task> PostConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> PostProducerTemplate;

    private final TaskUseCase taskUseCase;

    public Mono<Void> sendTask(String topic, Task task){
        String key = task.getTaskID();
        return PostProducerTemplate.send(topic, key, task).then();
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
                                .password(encryptedPassword)
                                .name(request.getName())
                                .email(request.getEmail())
                                        .elo(2000L)
                                        .curGameSpaceCode("")
                                .build());
                    } else {
                        return Mono.error(new RuntimeException("Duplicated data."));
                    }
                });
    }
    public Mono<UserResponse> login(UserRequest request) {
        String encryptedPassword = vaultAdapter.encrypt(request.getPassword());
        log.info("encrypt password : " + encryptedPassword);

        return userRepository.findByEmailAndPassword(request.getEmail(), encryptedPassword)
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
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials id:"+request.getEmail()+" pw:"+request.getPassword())));
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
        return userRepository.findByName(name)
                .flatMap(user -> userRepository.deleteByName(name)
                            .thenReturn(user.getId()))
                .then(Mono.empty());
    }

    public Mono<User> update(Long id, String name, String email,String password){
        String encryptedPassword = vaultAdapter.encrypt(password);

        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setEmail(email);
                    u.setPassword(encryptedPassword);
                    return userRepository.save(u);
                });
                // map으로 하면 Mono<Mono<User>>를 반환
    }

    private Mono<User> decryptUserData(User user) {
        String decryptedPassword = vaultAdapter.decrypt(user.getPassword());

        User decryptedUser = new User();
        decryptedUser.setId(user.getId());
        decryptedUser.setEmail(user.getEmail());
        decryptedUser.setPassword(decryptedPassword);
        decryptedUser.setName(user.getName());
        decryptedUser.setElo(user.getElo());
        decryptedUser.setCurGameSpaceCode(user.getCurGameSpaceCode());
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

        this.PostConsumerTemplate
                .receiveAutoAck()
                .doOnNext(r -> {
                    Task task = r.value();

                    List<SubTask> subTasks = task.getSubTaskList();

                    for(var subtask : subTasks){
                        try {
                            switch (subtask.getSubTaskName()) {
                                case "MatchUserNameByMembershipId":
                                    handleMatchUserName(subtask);
                                    break;
                                case "MatchUserCodeByMembershipId":
                                    handleMatchUserCode(subtask);
                                    break;
                                case "ResultTeamEloRequest":
                                    handleResultTeamElo(subtask);
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
    }

    private void handleMatchUserName(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());

        userRepository.findById(membershipId)
                .flatMap(user -> {
                    String nickname = user.getName();

                    List<SubTask> subTasks = new ArrayList<>();

                    subTasks.add(
                            taskUseCase.createSubTask("MatchUserNameByMembershipId",
                            String.valueOf(membershipId),
                            SubTask.TaskType.match,
                            SubTask.TaskStatus.ready,
                            nickname));

                    return sendTask("Match", taskUseCase.createTask(
                            "Match Request - Nickname",
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
                    String spaceId = "someSpaceId";
                    user.setCurGameSpaceCode(spaceId);
                    return userRepository.save(user);
                })
                .doOnError(e -> log.error("Error handling MatchUserCode for membershipId {}: {}", membershipId, e.getMessage()))
                .subscribe();
    }

    private void handleResultTeamElo(SubTask subtask) {
        Long membershipId = Long.parseLong(subtask.getMembershipId());
        Long teamElo = (Long) subtask.getData();
        // todo.
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
                    user.setCurGameSpaceCode("");
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

        return sendTask("Post",task)
                .thenMany(waitForTaskResult(task.getTaskID()))
                .flatMap(result -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        Task taskData = objectMapper.readValue(result, Task.class);
                        return Flux.fromIterable(taskData.getSubTaskList())
                                .filter(subTaskItem -> "success".equals(subTaskItem.getStatus()))
                                .flatMap(subTaskItem -> {
                                    PostSummary postSummary = objectMapper.convertValue(subTaskItem.getData(), PostSummary.class);
                                    return Mono.just(postSummary);
                                });
                    } catch (JsonProcessingException e) {
                        return Flux.error(new RuntimeException("Error processing task result", e));
                    }
                })
                .collectList();

    }

    private Flux<String> waitForTaskResult(String taskId) {
        return PostConsumerTemplate
                .receive()
                .filter(record -> taskId.equals(record.key()))
                .map(record -> record.value().toString())
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(throwable -> {
                    return Flux.error(new RuntimeException("Timeout while waiting for task result", throwable));
                });
    }


}
