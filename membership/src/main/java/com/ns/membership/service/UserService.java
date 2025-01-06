package com.ns.membership.service;


import static com.ns.common.TaskUseCase.createSubTask;
import static com.ns.common.TaskUseCase.createTask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.Utils.JwtToken;
import com.ns.common.Utils.JwtTokenProvider;
import com.ns.membership.Utils.VaultAdapter;
import com.ns.membership.axon.common.CreateMemberCommand;
import com.ns.membership.axon.common.ModifyMemberCommand;
import com.ns.membership.axon.common.ModifyMemberEloCommand;
import com.ns.membership.entity.User;
import com.ns.membership.entity.dto.PostSummary;
import com.ns.membership.entity.dto.UserCreateRequest;
import com.ns.membership.entity.dto.UserRequest;
import com.ns.membership.entity.dto.UserResponse;
import com.ns.membership.entity.dto.UserUpdateRequest;
import com.ns.membership.repository.UserR2dbcRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService{
    private final CommandGateway commandGateway;
    private final TaskService taskService;
    private final UserR2dbcRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final VaultAdapter vaultAdapter;

    public Mono<User> getMembershipById(Long membershipId){
        return userRepository.findById(membershipId);
    }

    public Mono<Void> createMemberByEvent(UserCreateRequest request) {
        CreateMemberCommand axonCommand = new CreateMemberCommand(request.getAccount(), request.getName(), request.getEmail(), request.getPassword());

        return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                .doOnSuccess(result -> create(request, (String) result)
                        .subscribe())
                .doOnError(throwable -> log.error("createMemberByEvent throwable : ", throwable))
                .then();
    }

    public Mono<User> create(UserCreateRequest request) {
        // String encryptedPassword = vaultAdapter.encrypt(request.getPassword());

        Flux<User> existingUsers = Flux.concat(
                userRepository.findByName(request.getName()),
                userRepository.findByEmail(request.getEmail())
        );

        return existingUsers.collectList()
                .flatMap(existingUserList -> {
                    if (existingUserList.isEmpty()) {
                        return userRepository.save(createUser(request));
                    } else {
                        return Mono.error(new RuntimeException("Duplicated data."));
                    }
                });
    }

    private User createUser(UserCreateRequest request){
        return User.builder()
                .aggregateIdentifier("")
                .account(request.getAccount())
                .password(request.getPassword())
                .name(request.getName())
                .email(request.getEmail())
                .elo(2000L)
                .code("")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Mono<User> create(UserCreateRequest request, String aggregateIdentifier){
        // String encryptedPassword = vaultAdapter.encrypt(request.getPassword());

        Flux<User> existingUsers = Flux.concat(
                userRepository.findByName(request.getName()),
                userRepository.findByEmail(request.getEmail())
        );

        return existingUsers.collectList()
                .flatMap(existingUserList -> {
                    if (existingUserList.isEmpty()) {
                        return userRepository.save(createUser(aggregateIdentifier, request));
                    } else {
                        return Mono.error(new RuntimeException("Duplicated data."));
                    }
                });
    }

    private User createUser(String aggregateIdentifier, UserCreateRequest request){
        return User.builder()
                .aggregateIdentifier(aggregateIdentifier)
                .account(request.getAccount())
                .password(request.getPassword())
                .name(request.getName())
                .email(request.getEmail())
                .elo(2000L)
                .code("")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Mono<UserResponse> login(UserRequest request) {
        // String encryptedPassword = vaultAdapter.encrypt(request.getPassword());
        return userRepository.findByAccountAndPassword(request.getAccount(), request.getPassword())
                .flatMap(this::handleUserLogin)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials :"+request.getAccount()+" pw:"+request.getPassword())));
    }

    private Mono<UserResponse> handleUserLogin(User user){
        String id = user.getId().toString();
        Mono<String> jwtMono = jwtTokenProvider.generateJwtToken(id);
        Mono<String> refreshMono = jwtTokenProvider.generateRefreshToken(id);

        return Mono.zip(jwtMono, refreshMono)
                .flatMap(tuple -> {
                    String jwtToken = tuple.getT1();
                    String refreshToken = tuple.getT2();

                    return updateTokens(user, jwtToken, refreshToken);
                });
    }

    private Mono<UserResponse> updateTokens(User user, String jwtToken, String refreshToken){
        user.setRefreshToken(refreshToken);

        return userRepository.save(user)
                .map(savedUser -> UserResponse.of(savedUser))
                .flatMap(userResponse -> {
                    userResponse.setJwtToken(jwtToken);
                    return Mono.just(userResponse);
                });
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

    public Mono<User> findByAccount(String account){
        return userRepository.findByAccount(account);
    }

    public Mono<User> update(Long id,String account, String name, String email,String password){
        // String encryptedPassword = vaultAdapter.encrypt(password);

        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setEmail(email);
                    u.setAccount(account);
                    u.setPassword(password);
                    u.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(u);
                });
    }

    public Mono<User> update(Long id,String account, String name, String email,String password, String aggregateIdentifier){
        // String encryptedPassword = vaultAdapter.encrypt(password);

        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setAggregateIdentifier(aggregateIdentifier);
                    u.setEmail(email);
                    u.setAccount(account);
                    u.setPassword(password);
                    u.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(u);
                });
    }

    public Mono<User> updateElo(Long id,Long elo, String aggregateIdentifer){
        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setAggregateIdentifier(aggregateIdentifer);
                    u.setElo(elo);
                    u.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(u);
                });
    }

    private Mono<User> decryptUserData(User user) {
        // String decryptedPassword = vaultAdapter.decrypt(user.getPassword());
        User decryptedUser = new User();
        decryptedUser.setId(user.getId());
        decryptedUser.setAccount(user.getAccount());
        decryptedUser.setEmail(user.getEmail());
        decryptedUser.setPassword(user.getPassword()); // decryptedPassword
        decryptedUser.setName(user.getName());
        decryptedUser.setElo(user.getElo());
        decryptedUser.setCode(user.getCode());
        return Mono.just(decryptedUser);
    }

    public Mono<User> validateJwtToken(String token) {
        return jwtTokenProvider.validateJwtToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new RuntimeException("Invalid token"));
                    }
                    return jwtTokenProvider.parseMembershipIdFromToken(token)
                            .flatMap(userRepository::findById)
                            .switchIfEmpty(Mono.error(new RuntimeException("User not found")));
                });
    }

    public Mono<JwtToken> refreshJwtToken(String refreshToken) {
        return validateJwtToken(refreshToken)
                .flatMap(membership -> {
                    if (!membership.getRefreshToken().equals(refreshToken)) {
                        return Mono.empty();
                    }

                    String membershipId = String.valueOf(membership.getId());

                    return jwtTokenProvider.generateJwtToken(membershipId)
                            .map(newJwtToken -> new JwtToken(membershipId, newJwtToken, refreshToken));
                                    });
    }

    public Mono<User> getMembershipByJwtToken(String token) {
        return validateJwtToken(token)
                .flatMap(membership -> {
                    if (!membership.getRefreshToken().equals(token)) {
                        return Mono.empty();
                    }
                    return Mono.just(membership);
                });
    }

    public Mono<Void> modifyMemberEloByEvent(String membershipId, Long elo) {
        return userRepository.findById(Long.parseLong(membershipId))
                .flatMap(user -> {
                    String memberAggregateIdentifier = user.getAggregateIdentifier();
                    ModifyMemberEloCommand axonCommand = new ModifyMemberEloCommand(memberAggregateIdentifier, membershipId, elo);

                    return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                            .doOnSuccess(result -> updateElo(Long.parseLong(membershipId), elo, result.toString())
                                    .subscribe())
                            .doOnError(throwable -> log.error("modifyMemberEloByEvent throwable : ", throwable))
                            .then();
                });
    }

    public Mono<User> modifyMemberElo(String membershipId, Long elo) {
        return userRepository.findById(Long.parseLong(membershipId))
                .flatMap(u -> {
                    u.setElo(elo);
                    u.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(u);
                });
    }

    public Mono<Void> modifyMemberByEvent(String membershipId, UserUpdateRequest request) {
        String account = request.getAccount();
        String name = request.getName();
        String email = request.getEmail();
        String password = request.getPassword();

        return userRepository.findById(Long.parseLong(membershipId))
                .flatMap(user -> {
                    String memberAggregateIdentifier = user.getAggregateIdentifier();
                    ModifyMemberCommand axonCommand = new ModifyMemberCommand(memberAggregateIdentifier, membershipId, account, name, email, password);

                    return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                            .doOnSuccess(result -> {
                                update(Long.parseLong(membershipId), account,name , email, password, result.toString())
                                        .subscribe();
                            })
                            .doOnError(throwable -> log.error("modifyMemberByEvent throwable : ", throwable))
                            .then();
                });
    }

    public Mono<User> updateSpaceId(Long membershipId, String spaceId){
        return userRepository.findById(membershipId)
                .flatMap(user -> {
                    user.setCode(spaceId);
                    log.info(user.getName()+" 사용자의 code는 "+spaceId+"입니다.");
                    return userRepository.save(user);
                });
    }

    public Mono<List<PostSummary>> getUserPosts(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListPostByMembershipId(membershipId);
        Task task = createTaskPostByMembershipId(membershipId, subTasks);

        return taskService.sendTask("task.post.response",task)
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
                .map(tick -> taskService.getTaskResults(taskId))
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
