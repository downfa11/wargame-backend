package com.ns.feed.application.service;


import static com.ns.feed.exception.ErrorCode.NOT_FOUND_COMMENT_ERROR_MESSAGE;
import static com.ns.feed.exception.ErrorCode.NOT_FOUND_POST_ERROR_MESSAGE;

import com.ns.common.anotation.UseCase;
import com.ns.feed.adapter.out.persistence.comment.Comment;
import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.application.port.in.comment.DeleteCommentUseCase;
import com.ns.feed.application.port.in.comment.FindCommentUseCase;
import com.ns.feed.application.port.in.comment.ModifyCommentUseCase;
import com.ns.feed.application.port.in.comment.RegisterCommentUseCase;
import com.ns.feed.application.port.out.TaskProducerPort;
import com.ns.feed.application.port.out.comment.DeleteCommentPort;
import com.ns.feed.application.port.out.comment.FindCommentPort;
import com.ns.feed.application.port.out.comment.ModifyCommentPort;
import com.ns.feed.application.port.out.comment.RegisterCommentPort;
import com.ns.feed.application.port.out.post.FindPostPort;
import com.ns.feed.application.port.out.post.ModifyPostPort;
import com.ns.feed.dto.CommentModifyRequest;
import com.ns.feed.dto.CommentRegisterRequest;
import com.ns.feed.dto.CommentResponse;
import com.ns.feed.exception.FeedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@UseCase
@Slf4j
@RequiredArgsConstructor
public class CommentService implements RegisterCommentUseCase, ModifyCommentUseCase, DeleteCommentUseCase,
        FindCommentUseCase {

    private final RegisterCommentPort registerCommentPort;
    private final ModifyCommentPort modifyCommentPort;
    private final DeleteCommentPort deleteCommentPort;
    private final FindCommentPort findCommentPort;

    private final ModifyPostPort modifyPostPort;
    private final FindPostPort findPostPort;
    private final TaskProducerPort taskProducerPort;

    @Override
    public Mono<CommentResponse> create(Long userId, CommentRegisterRequest request) {
        return findPostPort.findPostByPostId(request.getBoardId())
                .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_POST_ERROR_MESSAGE)))
                .flatMap(post -> taskProducerPort.getUserNameByComment(userId)
                                .flatMap(nickName -> registerCommentPort.registerComment(userId, nickName, request)
                                .zipWith(Mono.just(post)))
                .flatMap(tuple -> updateCommentsCount(tuple.getT2(), tuple.getT1()))
                                .map(CommentResponse::of));
    }

    private Mono<Comment> updateCommentsCount(Post post, Comment comment){
        Long curComments = post.getComments();
        post.setComments(curComments + 1);
        return modifyPostPort.update(post)
                .then(Mono.just(comment));
    }


    @Override
    public Mono<Void> delete(Long commentId) {
        return deleteCommentPort.deleteByCommentId(commentId);
    }

    @Override
    public Mono<CommentResponse> findByCommentId(Long commentId) {
        return findCommentPort.findCommentByCommentId(commentId)
                .map(comment -> {
                    CommentResponse commentResponse = CommentResponse.of(comment);
                    return commentResponse;
                });
    }

    @Override
    public Mono<CommentResponse> modify(Long membershipId, CommentModifyRequest request) {
        return findCommentPort.findCommentByCommentId(request.getCommentId())
                .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_COMMENT_ERROR_MESSAGE)))
                .flatMap(comment -> {
                    comment.setContent(request.getBody());

                    return taskProducerPort.getUserNameByComment(comment.getUserId())
                            .flatMap(nickname -> modifyCommentPort.modifyComment(membershipId, nickname, request));
                })
                .map(savedComment -> CommentResponse.of(savedComment));
    }

    @Override
    public Flux<CommentResponse> findAllByBoardId(Long boardId) {
        return findCommentPort.findCommentResponseByPostId(boardId);
    }

}
