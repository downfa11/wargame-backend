package com.ns.feed.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.feed.adapter.out.persistence.comment.Comment;
import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.application.port.out.TaskProducerPort;
import com.ns.feed.application.port.out.comment.DeleteCommentPort;
import com.ns.feed.application.port.out.comment.FindCommentPort;
import com.ns.feed.application.port.out.comment.ModifyCommentPort;
import com.ns.feed.application.port.out.comment.RegisterCommentPort;
import com.ns.feed.application.port.out.post.FindPostPort;
import com.ns.feed.application.port.out.post.ModifyPostPort;
import com.ns.feed.application.service.CommentService;
import com.ns.feed.dto.CommentModifyRequest;
import com.ns.feed.dto.CommentRegisterRequest;
import com.ns.feed.dto.CommentResponse;
import com.ns.feed.exception.FeedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @InjectMocks private CommentService commentService;

    @Mock private RegisterCommentPort registerCommentPort;
    @Mock private ModifyCommentPort modifyCommentPort;
    @Mock private DeleteCommentPort deleteCommentPort;
    @Mock private FindCommentPort findCommentPort;

    @Mock private ModifyPostPort modifyPostPort;
    @Mock private FindPostPort findPostPort;

    @Mock private TaskProducerPort taskProducerPort;

    private Comment comment;

    @BeforeEach
    void init(){
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
    void 댓글을_작성하는_메서드() {
        // given
        CommentRegisterRequest request = CommentRegisterRequest.builder()
                .boardId(1L)
                .body("댓글 내용")
                .build();

        Post post = Post.builder().id(1L).comments(1L).build();
        when(registerCommentPort.registerComment(any(),any(),eq(request))).thenReturn(Mono.just(comment));
        when(modifyPostPort.update(any())).thenReturn(Mono.just(post));
        when(findPostPort.findPostByPostId(anyLong())).thenReturn(Mono.just(post));
        when(taskProducerPort.getUserNameByComment(anyLong())).thenReturn(Mono.just("testUser"));
        // when
        Mono<CommentResponse> result = commentService.create(1L, request);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody().equals("댓글 내용"))
                .verifyComplete();

        verify(findPostPort, times(1)).findPostByPostId(1L);
        verify(registerCommentPort, times(1)).registerComment(any(), any(), eq(request));
    }

    @Test
    void 댓글을_삭제하는_메서드() {
        // given
        Long commentId = 1L;
        when(deleteCommentPort.deleteByCommentId(commentId)).thenReturn(Mono.empty());
        Mono<Void> result = commentService.delete(commentId);
        // when
        StepVerifier.create(result)
                .verifyComplete();

        // then
        verify(deleteCommentPort, times(1)).deleteByCommentId(commentId);
    }

    @Test
    void commentId를_통해_댓글을_조회하는_메서드() {
        // given, when
        when(findCommentPort.findCommentByCommentId(anyLong())).thenReturn(Mono.just(comment));
        Mono<CommentResponse> result = commentService.findByCommentId(1L);
        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody().equals("댓글 내용"))
                .verifyComplete();

        verify(findCommentPort, times(1)).findCommentByCommentId(1L);
    }

    @Test
    void 댓글을_수정하는_메서드() {
        // given
        CommentModifyRequest request = CommentModifyRequest.builder()
                .commentId(comment.getId())
                .body("updated contents")
                .build();

        when(findCommentPort.findCommentByCommentId(anyLong())).thenReturn(Mono.just(comment));
        when(taskProducerPort.getUserNameByComment(comment.getId())).thenReturn(Mono.just(comment.getNickname()));
        when(modifyCommentPort.modifyComment(any(), any(),eq(request)))
                .thenReturn(Mono.justOrEmpty(comment));
        // when
        Mono<CommentResponse> result = commentService.modify(1L, request);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody().equals("updated contents"))
                .verifyComplete();

        verify(findCommentPort, times(1)).findCommentByCommentId(1L);
        verify(modifyCommentPort, times(1)).modifyComment(any(), any(), eq(request));
        verify(taskProducerPort, times(1)).getUserNameByComment(comment.getId());
    }

    @Test
    void 댓글을_작성하려는데_없는_경우() {
        // given
        when(findPostPort.findPostByPostId(any())).thenReturn(Mono.empty());

        CommentRegisterRequest request = CommentRegisterRequest.builder()
                .boardId(999L)
                .body("Test comment")
                .build();

        // when
        Mono<CommentResponse> result = commentService.create(1L, request);

        // then
        StepVerifier.create(result)
                .expectError(FeedException.class)
                .verify();

        verify(findPostPort, times(1)).findPostByPostId(999L);
    }

    @Test
    void 댓글을_수정하려는데_없는_경우() {
        // given
        CommentModifyRequest request = CommentModifyRequest.builder()
                .commentId(999L)
                .body("Test content").build();

        when(findCommentPort.findCommentByCommentId(any())).thenReturn(Mono.empty());

        // when
        Mono<CommentResponse> result = commentService.modify(1L, request);

        // then
        StepVerifier.create(result)
                .expectError(FeedException.class)
                .verify();

        verify(findCommentPort, times(1)).findCommentByCommentId(999L);
    }
}
