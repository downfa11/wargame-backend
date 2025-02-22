package com.ns.feed.adapter.out.persistence.comment;

import static com.ns.feed.exception.ErrorCode.NOT_FOUND_COMMENT_ERROR_MESSAGE;
import static com.ns.feed.exception.ErrorCode.NOT_FOUND_POST_ERROR_MESSAGE;

import com.ns.common.anotation.PersistanceAdapter;
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

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class CommentPersistenceAdapter implements RegisterCommentPort, ModifyCommentPort, DeleteCommentPort,
        FindCommentPort {

    private final CommentR2dbcRepository commentR2dbcRepository;

    private final FindPostPort findPostPort;
    private final ModifyPostPort modifyPostPort;

    @Override
    public Mono<Comment> registerComment(Long userId, String nickName, CommentRegisterRequest request) {
        return commentR2dbcRepository.save(createComment(userId, request.getBoardId(), nickName, request.getBody()));
    }

    private Comment createComment(Long userId, Long boardId, String nickName, String content) {
        return Comment.builder()
                .userId(userId)
                .nickname(nickName)
                .boardId(boardId)
                .content(content)
                .build();
    }

    @Override
    public Mono<Comment> modifyComment(Long userId, String nickName, CommentModifyRequest request) {
        String content = request.getBody();

        return commentR2dbcRepository.findById(request.getCommentId())
                .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_COMMENT_ERROR_MESSAGE)))
                .flatMap(comment -> {
                    comment.setContent(content);
                    comment.setNickname(nickName);
                    return commentR2dbcRepository.save(comment);
                });
    }

    @Override
    public Flux<Void> deleteByBoardId(Long boardId) {
        return commentR2dbcRepository.findByBoardId(boardId)
                .flatMap(comment -> deleteByCommentId(comment.getId()));
    }

    public Mono<Void> deleteByCommentId(Long commentId) {
        return commentR2dbcRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_COMMENT_ERROR_MESSAGE)))
                .flatMap(comment -> {
                    long boardId = comment.getBoardId();

                    return findPostPort.findPostByPostId(boardId)
                            .switchIfEmpty(Mono.error(new FeedException(NOT_FOUND_POST_ERROR_MESSAGE)))
                            .flatMap(post -> {
                                Long curComments = post.getComments();
                                post.setComments(curComments - 1);
                                return modifyPostPort.update(post);
                            })
                            .then(commentR2dbcRepository.deleteById(commentId));
                });
    }

    @Override
    public Mono<Comment> findCommentByCommentId(Long commentId) {
        return commentR2dbcRepository.findById(commentId);
    }

    @Override
    public Flux<CommentResponse> findCommentResponseByPostId(Long postId) {
        return commentR2dbcRepository.findByBoardId(postId)
                .map(comment -> CommentResponse.of(comment));
    }
}
