package com.ns.feed.service;


import static com.ns.common.TaskUseCase.createSubTask;
import static com.ns.common.TaskUseCase.createTask;

import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.feed.entity.Post;
import com.ns.feed.entity.dto.PostModifyRequest;
import com.ns.feed.entity.dto.PostRegisterRequest;
import com.ns.feed.entity.dto.PostResponse;
import com.ns.feed.entity.dto.PostSummary;
import com.ns.feed.repository.PostR2dbcRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final String NOT_FOUND_CATEGORY_ERROR_MESSAGE = "Category not found";
    private final String NOT_FOUND_POST_ERROR_MESSAGE = "Post not found";

    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ReactiveRedisTemplate<String, Long> reactiveRedisTemplate_long;
    private final String BOARD_LIKES_KEY ="boards:likes:%s";
    private final String BOARD_VIEWS_KEY ="boards:views:%s";

    private final PostR2dbcRepository postR2dbcRepository;

    private final TaskService taskService;
    private final CommentService commentService;
    private final CategoryService categoryService;


    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    public Mono<Post> create(Long userId, PostRegisterRequest request){
        long categoryId = request.getCategoryId();

        return categoryService.findById(categoryId)
                .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_CATEGORY_ERROR_MESSAGE)))
                .flatMap(category -> getUserNameByPost(userId)
                        .flatMap(userName -> postR2dbcRepository.save(createPost(request, userId, userName))
                ));
    }

    private Post createPost(PostRegisterRequest request, Long userId, String userName){
        return Post.builder()
                .userId(userId)
                .nickname(userName)
                .categoryId(request.getCategoryId())
                .sortStatus(request.getSortStatus())
                .title(request.getTitle())
                .content(request.getContent())
                .comments(0L)
                .eventStartDate(request.getEventStartDate())
                .eventEndDate(request.getEventEndDate())
                .build();
    }

    public Mono<Post> modify(PostModifyRequest request){
        long boardId = request.getBoardId();
        return postR2dbcRepository.findById(boardId)
                .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_POST_ERROR_MESSAGE)))
                .flatMap(post -> getUserNameByPost(post.getUserId())
                        .flatMap(nickname -> categoryService.findById(request.getCategoryId())
                                .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_CATEGORY_ERROR_MESSAGE)))
                        .flatMap(category -> {
                            post.setCategoryId(request.getCategoryId());
                            post.setNickname(nickname);
                            post.setSortStatus(request.getSortStatus());
                            post.setTitle(request.getTitle());
                            post.setContent(request.getContent());
                            post.setEventStartDate(request.getEventStartDate());
                            post.setEventEndDate(request.getEventEndDate());
                            return postR2dbcRepository.save(post);
                        }))
                );

    }

    public Flux<Post> findAll(){
        return postR2dbcRepository.findAll();
    }

    public Mono<Page<PostSummary>> findPostAllPagination(Long categoryId, PageRequest pageRequest) {
        Mono<List<PostSummary>> postsMono = postR2dbcRepository.findAllByCategoryId(categoryId, pageRequest)
                .flatMap(post -> getPostViews(post.getId())
                                .flatMap(views -> getLikesCount(post.getId())
                                        .map(likes -> updatePostSummary(post, views, likes)))
                ).collectList();

        Mono<Long> countMono = postR2dbcRepository.countByCategoryId(categoryId);

        return Mono.zip(postsMono, countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageRequest, tuple.getT2()));
    }

    private PostSummary updatePostSummary(Post post, Long views, Long likes){
        PostSummary summary = PostSummary.of(post);
        summary.setViews(views);
        summary.setLikes(likes);
        return summary;
    }

    public Mono<PostResponse> findPostResponseById(Long membershipId){
        return postR2dbcRepository.findById(membershipId)
                .flatMap(post -> incrPostViews(membershipId)
                        .flatMap(views -> getLikesCount(membershipId)
                                .map(likes -> updatePostResponse(post, views, likes))
                        ))
                .flatMap(this::fetchCommentsForPost);
    }

    public Mono<PostResponse> updatePostResponseById(Long membershipId){
        return postR2dbcRepository.findById(membershipId)
                .flatMap(post -> getPostViews(membershipId)
                        .flatMap(views -> getLikesCount(membershipId)
                                .map(likes -> updatePostResponse(post, views, likes))
                        ))
                .flatMap(this::fetchCommentsForPost);
    }

    private PostResponse updatePostResponse(Post post, Long views, Long likes){
        PostResponse response = PostResponse.of(post);
        response.setViews(views);
        response.setLikes(likes);
        return response;
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
        return commentService.findByBoardId(post.getId())
                .collectList()
                .map(comments -> {
                    post.setCommentList(comments);
                    return post;
                });
    }

    public Mono<List<PostSummary>> findAllByUserId(Long membershipId) {
        return postR2dbcRepository.findByUserId(membershipId)
                .flatMap(post -> getPostViews(post.getId())
                        .flatMap(views -> getLikesCount(post.getId())
                                .map(likes -> {
                                    PostSummary summary = PostSummary.of(post);
                                    summary.setViews(views);
                                    summary.setLikes(likes);
                                    return summary;
                                })
                        )
                ).collectList()
                .doOnSuccess(posts -> {
                    if (posts.isEmpty()) {
                        log.warn("No posts found {}", membershipId);
                    }
                    else log.info("success findAllByUserId");
                });
    }

    public Mono<Void> deleteById(Long boardId) {
        return postR2dbcRepository.findById(boardId)
                        .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_POST_ERROR_MESSAGE)))
                        .flatMap(post ->{
                                // redis에 기록된 조회수도 지워야한다.
                                reactiveRedisTemplate_long.unlink(BOARD_VIEWS_KEY.formatted(boardId));
                                reactiveRedisTemplate.unlink(BOARD_LIKES_KEY.formatted(boardId));
                                return commentService.deleteByBoardId(boardId)
                                        .then(postR2dbcRepository.deleteById(boardId));
                        });
    }

    // 해당 사용자는 좋아요를 눌렀는가?
    public Mono<Boolean> isUserLiked(Long boardId, Long userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().isMember(key, userId);
    }

    public Mono<Long> addLike(Long boardId, String userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().add(key, userId);
    }

    public Mono<Long> removeLike(Long boardId, Long userId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().remove(key, userId);
    }

    public Mono<Long> getLikesCount(Long boardId) {
        String key = String.format(BOARD_LIKES_KEY, boardId);
        return reactiveRedisTemplate.opsForSet().size(key);
    }

    public Mono<Long> updateLikes(Long userId, Long boardId) {
        return isUserLiked(boardId, userId)
                .flatMap(userLiked -> {
                    if (!userLiked) { // 좋아요
                        return addLike(boardId, String.valueOf(userId))
                                .then(getLikesCount(boardId));
                    } else if (userLiked) { // 좋아요 취소
                        return removeLike(boardId, userId)
                                .then(getLikesCount(boardId));
                    } else {
                        return getLikesCount(boardId);
                    }
                });
    }

    public Mono<String> getUserNameByPost(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListPostUserNameByMembershipId(membershipId);
        Task task = createTaskGetUserName(membershipId, subTasks);

        return sendTask("task.membership.response",task)
                .then(waitForGetUserNameTaskFeed(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Task createTaskGetUserName(Long membershipId, List<SubTask> subTasks){
        return createTask("Post Response", String.valueOf(membershipId), subTasks);
    }

    private List<SubTask> createSubTaskListPostUserNameByMembershipId(Long membershipId){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskPostUserNameByMembershipId(membershipId));

        return subTasks;
    }

    private SubTask createSubTaskPostUserNameByMembershipId(Long membershipId){
        return  createSubTask("PostUserNameByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.post,
                SubTask.TaskStatus.ready,
                membershipId);
    }


    public Mono<Void> postByMembershipId(String taskId, String membershipId) {
        Long memberId = Long.parseLong(membershipId);

        return findAllByUserId(memberId)
                .flatMap(this::createSubTaskPostSummaryList)
                .flatMap(subTasks -> sendPostByMembershipIdTask(taskId, membershipId, subTasks));
    }

    private Mono<List<SubTask>> createSubTaskPostSummaryList(List<PostSummary> posts){
        return Flux.fromIterable(posts)
                .map(this::createSubTaskPostSummary)
                .collectList();
    }

    private SubTask createSubTaskPostSummary(PostSummary postSummary){
        return createSubTask("PostSummary",
                String.valueOf(postSummary.getId()),
                SubTask.TaskType.post,
                SubTask.TaskStatus.success,
                postSummary);
    }

    private Mono<Void> sendPostByMembershipIdTask(String taskId, String membershipId, List<SubTask> subTasks){
        return sendTask("task.membership.request",
                createTask(taskId,
                        "Post Response",
                        membershipId,
                        subTasks));
    }

    public Mono<List<PostResponse>> findLatestAnnounces(Integer count){
        return postR2dbcRepository.findLatestAnnounces(count)
                .collectList()
                .map(posts -> posts.stream()
                        .map(PostResponse::of)
                        .collect(Collectors.toList()));
    }

    public Mono<List<PostResponse>> findInProgressEvents(){
        return postR2dbcRepository.findInProgressEvents(LocalDateTime.now())
                .collectList()
                .map(posts -> posts.stream()
                        .map(PostResponse::of)
                        .collect(Collectors.toList()));
    }


    private Mono<String> waitForGetUserNameTaskFeed(String taskId) {
        return Flux.interval(Duration.ofMillis(500))
                .map(tick -> taskService.getTaskResults(taskId))
                .filter(Objects::nonNull)
                .take(1)
                .map(task -> {
                    SubTask subTask = task.getSubTaskList().get(0);
                    return String.valueOf(subTask.getData());
                })
                .next()
                .timeout(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.error(new RuntimeException("Timeout waitForGetUserNameTaskFeed for taskId " + taskId)));
    }
}
