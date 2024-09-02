package com.ns.feed.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.common.TaskUseCase;
import com.ns.feed.entity.Comment;
import com.ns.feed.entity.dto.CommentModifyRequest;
import com.ns.feed.entity.dto.CommentRegisterRequest;
import com.ns.feed.entity.dto.CommentResponse;
import com.ns.feed.repository.CommentR2dbcRepository;
import com.ns.feed.repository.PostR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentR2dbcRepository commentR2dbcRepository;
    private final PostR2dbcRepository postR2dbcRepository;
    private final PostService postService;


    private final ReactiveKafkaConsumerTemplate<String, Task> MembershipConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> MembershipProducerTemplate;

    private final TaskUseCase taskUseCase;

    public Mono<Void> sendTask(String topic, Task task){
        String key = task.getTaskID();
        return MembershipProducerTemplate.send(topic, key, task).then();
    }

    public Mono<CommentResponse> create(Long userId, CommentRegisterRequest request) {
        long boardId = request.getBoardId();
        String content = request.getBody();

        return postService.findPostById(boardId)
                .flatMap(post -> {
                    Long curComments = post.getComments();
                    post.setComments(curComments + 1);
                    return postR2dbcRepository.save(post)
                            .then(commentR2dbcRepository.save(Comment.builder()
                                    .userId(userId)
                                    .boardId(boardId)
                                    .content(content)
                                    .build()));
                })
                .flatMap(savedComment -> getUserName(userId)
                        .map(username -> {
                            CommentResponse commentResponse = CommentResponse.of(savedComment);
                            commentResponse.setNickname(username);
                            return commentResponse;
                        }));
    }


    public Mono<CommentResponse> modify(CommentModifyRequest request) {
        String content = request.getBody();

        return commentR2dbcRepository.findById(request.getCommentId())
                .flatMap(post -> {
                    post.setContent(content);
                    return commentR2dbcRepository.save(post);
                }).flatMap(savedComment -> getUserName(savedComment.getUserId())
                        .map(username -> {
                            CommentResponse commentResponse = CommentResponse.of(savedComment);
                            commentResponse.setNickname(username);
                            return commentResponse;
                        }))
                .switchIfEmpty(Mono.error(new RuntimeException("Comment or category not found")));
    }
    public Mono<CommentResponse> findById(Long id){
        return commentR2dbcRepository.findById(id)
                .flatMap(comments -> getUserName(comments.getUserId())
                .map(username -> {
                    CommentResponse commentResponse = CommentResponse.of(comments);
                    commentResponse.setNickname(username);
                    return commentResponse;
                }));
    }

    public Flux<Comment> findAllByBoardId(Long boardId) {return commentR2dbcRepository.findByBoardId(boardId);}

    public Mono<Void> deleteById(Long commentId) {
        return commentR2dbcRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new RuntimeException("Comment not found")))
                .flatMap(comment -> {
                    long boardId = comment.getBoardId();
                    return postService.findPostById(boardId)
                            .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                            .flatMap(post -> {
                                Long curComments = post.getComments();
                                post.setComments(curComments - 1);
                                return postR2dbcRepository.save(post);
                            })
                            .then(commentR2dbcRepository.deleteById(commentId));
                });
    }

    public Mono<String> getUserName(Long membershipId) {
        List<SubTask> subTasks = new ArrayList<>();

        subTasks.add(
                taskUseCase.createSubTask("CommentUserNameByMembershipId",
                        String.valueOf(membershipId),
                        SubTask.TaskType.post,
                        SubTask.TaskStatus.ready,
                        membershipId));

        Task task = taskUseCase.createTask(
                "Comment Response",
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
                                    String username = (String) subTaskItem.getData();
                                    return Mono.just(username);
                                });
                    } catch (JsonProcessingException e) {
                        return Flux.error(new RuntimeException("Error processing task result", e));
                    }
                }).single();

    }

    private Flux<String> waitForTaskResult(String taskId) {
        return MembershipConsumerTemplate
                .receive()
                .filter(record -> taskId.equals(record.key()))
                .map(record -> record.value().toString())
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(throwable -> {
                    return Flux.error(new RuntimeException("Timeout while waiting for task result", throwable));
                });
        }
}
