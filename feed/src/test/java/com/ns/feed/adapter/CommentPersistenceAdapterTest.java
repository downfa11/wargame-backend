package com.ns.feed.adapter;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.feed.adapter.out.persistence.comment.Comment;
import com.ns.feed.adapter.out.persistence.comment.CommentPersistenceAdapter;
import com.ns.feed.adapter.out.persistence.comment.CommentR2dbcRepository;
import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.application.port.out.post.FindPostPort;
import com.ns.feed.application.port.out.post.ModifyPostPort;
import com.ns.feed.dto.CommentModifyRequest;
import com.ns.feed.dto.CommentRegisterRequest;
import com.ns.feed.exception.FeedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CommentPersistenceAdapterTest {

    @Mock private CommentR2dbcRepository commentR2dbcRepository;
    @Mock private FindPostPort findPostPort;
    @Mock private ModifyPostPort modifyPostPort;

    private CommentPersistenceAdapter commentPersistenceAdapter;
    private Comment comment;

    @BeforeEach
    void init() {
        commentPersistenceAdapter = new CommentPersistenceAdapter(commentR2dbcRepository, findPostPort, modifyPostPort);

        Long userId = 1L;
        Long boardId = 10L;
        String nickname = "testUser";
        String content = "댓글 내용";

        comment = Comment.builder()
                .id(1L)
                .userId(userId)
                .boardId(boardId)
                .nickname(nickname)
                .content(content)
                .build();
    }

    @Test
    void 댓글을_등록하는_메서드() {
        // given
        String nickname = comment.getNickname();
        Long userId = comment.getUserId();
        Long boardId = comment.getBoardId();
        String content = comment.getContent();

        CommentRegisterRequest request = CommentRegisterRequest.builder()
                .boardId(boardId)
                .body(content)
                .build();

        when(commentR2dbcRepository.save(any(Comment.class))).thenReturn(Mono.just(comment));

        // when
        Comment result = commentPersistenceAdapter.registerComment(userId, nickname, request).block();

        // then
        assertNotNull(result);
        assertEquals(content, result.getContent());
        verify(commentR2dbcRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void 댓글을_수정하는_메서드() {
        // given
        Long commentId = 1L;
        Long userId = 1L;
        String newNickname = "updatedUser";
        String newContent = "수정된 댓글 내용";
        CommentModifyRequest request = CommentModifyRequest.builder()
                .commentId(commentId)
                .body(newContent)
                .build();

        when(commentR2dbcRepository.findById(commentId)).thenReturn(Mono.just(comment));
        when(commentR2dbcRepository.save(any(Comment.class))).thenReturn(Mono.just(comment));

        // when
        Comment result = commentPersistenceAdapter.modifyComment(userId, newNickname, request).block();

        // then
        assertNotNull(result);
        assertEquals(newContent, result.getContent());
        assertEquals(newNickname, result.getNickname());
        verify(commentR2dbcRepository, times(1)).save(comment);
    }

    @Test
    void 존재하지_않는_댓글을_수정하면_예외를_던진다() {
        // given
        Long commentId = 99L;
        CommentModifyRequest request = CommentModifyRequest.builder()
                .commentId(commentId)
                .body("수정 내용")
                .build();

        when(commentR2dbcRepository.findById(commentId)).thenReturn(Mono.empty());

        // when & then
        assertThrows(FeedException.class, () -> commentPersistenceAdapter.modifyComment(1L, "user", request).block());
    }

    @Test
    void commentId로_댓글을_삭제하는_메서드() {
        // given
        Long commentId = 5L;
        Long boardId = 10L;
        Post post = Post.builder()
                .id(boardId)
                .comments(3L)
                .build();

        when(commentR2dbcRepository.findById(commentId)).thenReturn(Mono.just(comment));
        when(findPostPort.findPostByPostId(boardId)).thenReturn(Mono.just(post));
        when(modifyPostPort.update(any(Post.class))).thenReturn(Mono.just(post));
        when(commentR2dbcRepository.deleteById(commentId)).thenReturn(Mono.empty());

        // when
        commentPersistenceAdapter.deleteByCommentId(commentId).block();

        // then
        assertEquals(2L, post.getComments());
        verify(commentR2dbcRepository, times(1)).deleteById(commentId);
    }

    @Test
    void 댓글을_삭제하는_메서드_존재하지_않는_경우_예외처리() {
        // given
        Long commentId = 99L;
        when(commentR2dbcRepository.findById(commentId)).thenReturn(Mono.empty());

        // when & then
        assertThrows(FeedException.class, () -> commentPersistenceAdapter.deleteByCommentId(commentId).block());
    }


    @Test
    void commentId로_댓글을_조회하는_메서드() {
        // given
        Long commentId = 1L;
        when(commentR2dbcRepository.findById(commentId)).thenReturn(Mono.just(comment));

        // when
        Comment result = commentPersistenceAdapter.findCommentByCommentId(commentId).block();

        // then
        assertNotNull(result);
        assertEquals(commentId, result.getId());
        verify(commentR2dbcRepository, times(1)).findById(commentId);
    }

    @Test
    void postId로_댓글의_목록을_조회하는_메서드() {
        // given
        Long postId = 10L;
        when(commentR2dbcRepository.findByBoardId(postId)).thenReturn(Flux.just(comment));

        // when
        long count = commentPersistenceAdapter.findCommentResponseByPostId(postId).count().block();

        // then
        assertEquals(1, count);
        verify(commentR2dbcRepository, times(1)).findByBoardId(postId);
    }
}

