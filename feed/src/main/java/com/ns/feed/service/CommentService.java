package com.ns.feed.service;


import static com.ns.common.TaskUseCase.createSubTask;
import static com.ns.common.TaskUseCase.createTask;
import static com.ns.feed.service.KafkaService.waitForGetUserNameTaskComment;

import com.ns.common.SubTask;
import com.ns.common.Task;
import com.ns.feed.entity.Comment;
import com.ns.feed.entity.Post;
import com.ns.feed.entity.dto.CommentModifyRequest;
import com.ns.feed.entity.dto.CommentRegisterRequest;
import com.ns.feed.entity.dto.CommentResponse;
import com.ns.feed.repository.CommentR2dbcRepository;
import com.ns.feed.repository.PostR2dbcRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private final String NOT_FOUND_COMMENT_ERROR_MESSAGE = "Comment not found";
    private final String NOT_FOUND_POST_ERROR_MESSAGE = "Post not found";

    private final ReactiveKafkaProducerTemplate<String, Task> taskProducerTemplate;

    private final CommentR2dbcRepository commentR2dbcRepository;
    private final PostR2dbcRepository postR2dbcRepository;


    public Mono<Void> sendTask(String topic, Task task){
        log.info("send ["+topic+"]: "+task.toString());
        String key = task.getTaskID();
        return taskProducerTemplate.send(topic, key, task).then();
    }

    public Mono<CommentResponse> create(Long userId, CommentRegisterRequest request) {
        long boardId = request.getBoardId();
        String content = request.getBody();

        return postR2dbcRepository.findById(boardId)
                .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_POST_ERROR_MESSAGE)))
                .flatMap(post -> getUserNameByComment(userId)
                            .flatMap(nickName -> {
                                Comment comment = createComment(userId, boardId, nickName, content);
                                return commentR2dbcRepository.save(comment)
                                    .flatMap(savedComment -> updateCommentsCount(post, savedComment))
                                    .map(CommentResponse::of);
                                }));
    }

    private Comment createComment(Long userId, Long boardId, String nickName, String content){
        return Comment.builder()
                .userId(userId)
                .nickname(nickName)
                .boardId(boardId)
                .content(content)
                .build();
    }

    private Mono<Comment> updateCommentsCount(Post post, Comment comment){
        Long curComments = post.getComments();
        post.setComments(curComments + 1);
        return postR2dbcRepository.save(post)
                .then(Mono.just(comment));
    }

    public Mono<CommentResponse> modify(CommentModifyRequest request) {
        String content = request.getBody();

        return commentR2dbcRepository.findById(request.getCommentId())
                .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_COMMENT_ERROR_MESSAGE)))
                .flatMap(comment -> {
                    comment.setContent(content);
                    return getUserNameByComment(comment.getUserId())
                            .doOnNext(nickname -> comment.setNickname(nickname))
                            .then(commentR2dbcRepository.save(comment)); })
                        .flatMap(savedComment -> Mono.just(CommentResponse.of(savedComment)));
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

    public Mono<Void> deleteByCommentId(Long commentId) {
        return commentR2dbcRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_COMMENT_ERROR_MESSAGE)))
                .flatMap(comment -> {
                    long boardId = comment.getBoardId();

                    return postR2dbcRepository.findById(boardId)
                            .switchIfEmpty(Mono.error(new RuntimeException(NOT_FOUND_POST_ERROR_MESSAGE)))
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
                .flatMap(comment -> deleteByCommentId(comment.getId()));
    }

    public Mono<String> getUserNameByComment(Long membershipId) {
        List<SubTask> subTasks = createSubTaskListCommentUserNameByMembershipId(membershipId);
        Task task = createTaskCommentUserNameByMembershipId(membershipId, subTasks);

        return sendTask("task.membership.response",task)
                .then(waitForGetUserNameTaskComment(task.getTaskID())
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Task createTaskCommentUserNameByMembershipId(Long membershipId, List<SubTask> subTasks){
        return createTask(
                "Comment Response",
                String.valueOf(membershipId),
                subTasks);
    }

    private List<SubTask> createSubTaskListCommentUserNameByMembershipId(Long membershipId){
        List<SubTask> subTasks = new ArrayList<>();
        subTasks.add(createSubTaskCommentUserNameByMembershipId(membershipId));

        return subTasks;
    }

    private SubTask createSubTaskCommentUserNameByMembershipId(Long membershipId){
        return createSubTask("CommentUserNameByMembershipId",
                String.valueOf(membershipId),
                SubTask.TaskType.post,
                SubTask.TaskStatus.ready,
                membershipId);
    }
}
