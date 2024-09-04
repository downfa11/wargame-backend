package com.ns.feed.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.TaskUseCase;
import com.ns.feed.entity.Post;
import com.ns.feed.entity.dto.PostModifyRequest;
import com.ns.feed.entity.dto.PostRegisterRequest;
import com.ns.feed.entity.dto.PostResponse;
import com.ns.feed.entity.dto.PostSummary;
import com.ns.feed.repository.CommentR2dbcRepository;
import com.ns.feed.repository.PostR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService implements ApplicationRunner {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate_long;
    private final String BOARD_LIKES_KEY ="boards:likes:%s";
    private final String BOARD_VIEWS_KEY ="boards:views:%s";

    private final PostR2dbcRepository postR2dbcRepository;
    private final CommentR2dbcRepository commentR2dbcRepository;

    private final CategoryService categoryService;


    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final TaskUseCase taskUseCase;
    private final ObjectMapper objectMapper;

    public static final ConcurrentHashMap<String, Task> taskResults = new ConcurrentHashMap<>();
    private final int MAX_TASK_RESULT_SIZE = 5000;

    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    public Mono<Post> create(Long userId, PostRegisterRequest request){
        long categoryId = request.getCategoryId();

        return categoryService.findById(categoryId)
                .flatMap(category -> {
                    Post.SortStatus status = request.getSortStatus();
                    String title = request.getTitle();
                    String content = request.getContent();

                    return getUserName(userId)
                            .flatMap(nickname -> postR2dbcRepository.save(Post.builder()
                                    .userId(userId)
                                    .nickname(nickname)
                                    .categoryId(categoryId)
                                    .sortStatus(status)
                                    .title(title)
                                    .content(content)
                                    .comments(0L)
                                    .build()));
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Category not found")));
    }

    public Mono<Post> modify(PostModifyRequest request){
        long boardId = request.getBoardId();
        return postR2dbcRepository.findById(boardId)
                .flatMap(post -> getUserName(post.getUserId())
                        .flatMap(nickname -> categoryService.findById(request.getCategoryId())
                        .flatMap(category -> {
                            post.setCategoryId(request.getCategoryId());
                            post.setNickname(nickname);
                            post.setSortStatus(request.getSortStatus());
                            post.setTitle(request.getTitle());
                            post.setContent(request.getContent());
                            return postR2dbcRepository.save(post);
                        }))
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Post or category not found")));
    }

    public Flux<Post> findAll(){
        return postR2dbcRepository.findAll();
    }

    public Mono<Page<PostSummary>> findPostAllPagination(Long categoryId, PageRequest pageRequest) {
        Mono<List<PostSummary>> postsMono = postR2dbcRepository.findAllByCategoryId(categoryId, pageRequest)
                .flatMap(post -> getPostViews(post.getId())
                                .flatMap(views -> getLikesCount(post.getId().toString())
                                        .map(likes -> {
                                            PostSummary summary = PostSummary.of(post);
                                            summary.setViews(views);
                                            summary.setLikes(likes);
                                            return summary;
                                        })
                        )
                ).collectList();

        Mono<Long> countMono = postR2dbcRepository.countByCategoryId(categoryId);

        return Mono.zip(postsMono, countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageRequest, tuple.getT2()));
    }

    public Mono<PostResponse> findPostResponseById(Long id){
        return postR2dbcRepository.findById(id)
                .flatMap(post -> incrPostViews(id)
                        .flatMap(views -> getLikesCount(id.toString())
                                .map(likes -> {
                                    PostResponse response = PostResponse.of(post);
                                    response.setViews(views);
                                    response.setLikes(likes);
                                    return response;
                                }))).flatMap(this::fetchCommentsForPost);
    }

    public Mono<PostResponse> updatePostResponseById(Long id){
        return postR2dbcRepository.findById(id)
                .flatMap(post -> getPostViews(id)
                        .flatMap(views -> getLikesCount(id.toString())
                                .map(likes -> {
                                    PostResponse response = PostResponse.of(post);
                                    response.setViews(views);
                                    response.setLikes(likes);
                                    return response;
                                }))).flatMap(this::fetchCommentsForPost);
    }

    public Mono<Post> findPostById(Long id){
        return postR2dbcRepository.findById(id);
    }

    public Mono<Long> incrPostViews(Long boardId){
         return reactiveRedisTemplate_long.opsForValue()
                 .increment(BOARD_VIEWS_KEY.formatted(boardId))
                 .switchIfEmpty(Mono.just(0L));
    }

    public Mono<Long> getPostViews(Long boardId){
        return reactiveRedisTemplate_long.opsForValue()
                .get(BOARD_VIEWS_KEY.formatted(boardId))
                .switchIfEmpty(Mono.just(0L));
    }

    private Mono<PostResponse> fetchCommentsForPost(PostResponse post) {
        return commentR2dbcRepository.findByBoardId(post.getId())
                .collectList()
                .map(comments -> {
                    post.setCommentList(comments);
                    return post;
                });
    }

    public Mono<List<PostSummary>> findAllByUserId(Long membershipId) {
        return postR2dbcRepository.findByUserId(membershipId)
                .flatMap(post -> getPostViews(post.getId())
                        .flatMap(views -> getLikesCount(post.getId().toString())
                                .map(likes -> {
                                    PostSummary summary = PostSummary.of(post);
                                    summary.setViews(views);
                                    summary.setLikes(likes);
                                    return summary;
                                })
                        )
                ).collectList();
    }

    public Mono<Void> deleteById(Long boardId) {
        return postR2dbcRepository.findById(boardId)
                        .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                        .flatMap(post ->{
                                // redis에 기록된 조회수도 지워야한다.
                            reactiveRedisTemplate_long.unlink(BOARD_VIEWS_KEY.formatted(boardId));
                                reactiveRedisTemplate.unlink(BOARD_LIKES_KEY.formatted(boardId));
                                 return postR2dbcRepository.deleteById(boardId);
                        });
    }

    // 해당 사용자는 좋아요를 눌렀는가?
    public Mono<Boolean> isUserLiked(String boardId, String userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().isMember(key, userId);
    }

    public Mono<Long> addLike(String boardId, String userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().add(key, userId);
    }

    public Mono<Long> removeLike(String boardId, String userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().remove(key, userId);
    }

    public Mono<Long> getLikesCount(String boardId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().size(key);
    }

    public Mono<Long> updateLikes(Long userId, Long boardId) {
        String userIdString = String.valueOf(userId);
        String boardIdString = String.valueOf(boardId);

        return isUserLiked(boardIdString, userIdString)
                .flatMap(userLiked -> {

                    if (!userLiked) { // 좋아요
                        return addLike(boardIdString, userIdString)
                                .then(getLikesCount(boardIdString));
                    }

                    else if (userLiked) { // 좋아요 취소
                        return removeLike(boardIdString, userIdString)
                                .then(getLikesCount(boardIdString));
                    }

                    else {
                        return getLikesCount(boardIdString);
                    }
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
                                case "PostByMembershipId":
                                    String membershipId = String.valueOf(subtask.getData());

                                    if (membershipId != null) {
                                        postByMembershipId(task.getTaskID(),membershipId)
                                                .doOnError(e -> log.error("PostByMembershipId 처리 중 오류 발생", e))
                                                .subscribe();
                                    }

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

    private Mono<Void> postByMembershipId(String taskId, String membershipId) {
        Long memberId = Long.parseLong(String.valueOf(membershipId));

        return findAllByUserId(memberId)
                .flatMap(posts -> {
                    List<SubTask> subTasks = posts.stream()
                            .map(postSummary ->
                                    taskUseCase.createSubTask("PostSummary",
                                                    String.valueOf(postSummary.getId()),
                                                            SubTask.TaskType.post,
                                                            SubTask.TaskStatus.success,
                                                            postSummary))
                            .toList();

                    return sendTask("task.membership.request",taskUseCase.createTask(
                            taskId,
                            "Post Response",
                            String.valueOf(membershipId),
                            subTasks));
                });
    }

    public Mono<String> getUserName(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();

        subTasks.add(
                taskUseCase.createSubTask("PostUserNameByMembershipId",
                        String.valueOf(membershipId),
                        SubTask.TaskType.post,
                        SubTask.TaskStatus.ready,
                        membershipId));

        Task task = taskUseCase.createTask(
                "Post Response",
                String.valueOf(membershipId),
                subTasks);

        return sendTask("task.membership.response",task)
                .then(waitForGetUserNameTaskResult(task.getTaskID()));
    }

    private Mono<String> waitForGetUserNameTaskResult(String taskId) {
        return Mono.defer(() -> {
            return Mono.fromCallable(() -> {
                        while (true) {
                            Task resultTask = taskResults.get(taskId);
                            if (resultTask != null) {
                                SubTask subTask = resultTask.getSubTaskList().get(0);
                                return String.valueOf(subTask.getData());
                            }
                            Thread.sleep(50);
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofSeconds(3))
                    .onErrorResume(e -> Mono.error(new RuntimeException("waitForUserPostsTaskResult error : ", e)));
        });
    }

}
