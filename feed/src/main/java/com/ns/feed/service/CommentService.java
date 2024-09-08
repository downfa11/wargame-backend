package com.ns.feed.service;


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
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.ns.feed.service.PostService.taskResults;


@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final CommentR2dbcRepository commentR2dbcRepository;
    private final PostR2dbcRepository postR2dbcRepository;


    private final ReactiveKafkaConsumerTemplate<String, Task> TaskRequestConsumerTemplate;
    private final ReactiveKafkaConsumerTemplate<String, Task> TaskResponseConsumerTemplate;
    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final TaskUseCase taskUseCase;
    private final ObjectMapper objectMapper;


    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    public Mono<CommentResponse> create(Long userId, CommentRegisterRequest request) {
        long boardId = request.getBoardId();
        String content = request.getBody();

        return postR2dbcRepository.findById(boardId)
                .flatMap(post -> {
                    return getUserName(userId)
                            .flatMap(nickname -> commentR2dbcRepository.save(Comment.builder()
                                    .userId(userId)
                                    .nickname(nickname)
                                    .boardId(boardId)
                                    .content(content)
                                    .build()))
                            .flatMap(savedComment -> {
                                Long curComments = post.getComments();
                                post.setComments(curComments + 1);
                                return postR2dbcRepository.save(post)
                                        .then(Mono.just(savedComment));
                            });
                })
                .flatMap(savedComment -> Mono.just(CommentResponse.of(savedComment)))
                .switchIfEmpty(Mono.error(new RuntimeException("Post not found")));
    }

    public Mono<CommentResponse> modify(CommentModifyRequest request) {
        String content = request.getBody();

        return commentR2dbcRepository.findById(request.getCommentId())
                .flatMap(comment -> {
                    comment.setContent(content);
                    return getUserName(comment.getUserId())
                            .doOnNext(nickname -> comment.setNickname(nickname))
                            .then(commentR2dbcRepository.save(comment)); })
                        .flatMap(savedComment -> Mono.just(CommentResponse.of(savedComment)))
                .switchIfEmpty(Mono.error(new RuntimeException("Comment or category not found")));
    }
    public Mono<CommentResponse> findById(Long id){
        return commentR2dbcRepository.findById(id)
                .map(comment -> {
                    CommentResponse commentResponse = CommentResponse.of(comment);
                    return commentResponse;
                });
    }

    public Flux<CommentResponse> findByBoardId(Long boardId){
        return commentR2dbcRepository.findByBoardId(boardId)
                .map(comment -> CommentResponse.of(comment));
    }

    public Flux<Comment> findAllByBoardId(Long boardId) {return commentR2dbcRepository.findByBoardId(boardId);}

    public Mono<Void> deleteById(Long commentId) {
        return commentR2dbcRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new RuntimeException("Comment not found")))
                .flatMap(comment -> {
                    long boardId = comment.getBoardId();
                    return postR2dbcRepository.findById(boardId)
                            .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                            .flatMap(post -> {
                                Long curComments = post.getComments();
                                post.setComments(curComments - 1);
                                return postR2dbcRepository.save(post);
                            })
                            .then(commentR2dbcRepository.deleteById(commentId));
                });
    }

    public Flux<Void> deleteByBoardId(Long boardId){
        return commentR2dbcRepository.findByBoardId(boardId)
                .flatMap(comment -> {
                    Long commentId = comment.getId();
                    return commentR2dbcRepository.deleteById(commentId);
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

        return sendTask("task.membership.response",task)
                .then(waitForGetUserNameTaskResult(task.getTaskID()));
    }

    private Mono<String> waitForGetUserNameTaskResult(String taskId) {
        return Mono.defer(() -> {
            return Mono.fromCallable(() -> {
                        while (true) {
                            Task resultTask = taskResults.get(taskId);
                            if (resultTask != null) {
                                log.info("resultTask : " + resultTask);
                                SubTask subTask = resultTask.getSubTaskList().get(0);
                                return String.valueOf(subTask.getData());
                            }
                            Thread.sleep(500);
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .timeout(Duration.ofSeconds(3))
                    .onErrorResume(e -> Mono.error(new RuntimeException("waitForUserPostsTaskResult error : ", e)));
        });
    }

}
