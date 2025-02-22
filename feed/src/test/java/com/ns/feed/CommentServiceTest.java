package com.ns.feed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void 댓글을_작성하는_메서드() {
        // given
        CommentRegisterRequest request = new CommentRegisterRequest();
        request.setBoardId(1L);
        request.setBody("Test comment");

        // when
        Mono<CommentResponse> result = commentService.create(1L, request);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody().equals("Test comment"))
                .verifyComplete();

        verify(findPostPort, times(1)).findPostByPostId(1L);
        verify(registerCommentPort, times(1)).registerComment(any(), any(), eq(request));
    }

    @Test
    void 댓글을_삭제하는_메서드() {
        // given
        Mono<Void> result = commentService.delete(1L);

        // when
        StepVerifier.create(result).verifyComplete();

        // then
        verify(deleteCommentPort, times(1)).deleteByCommentId(1L);
    }

    @Test
    void CommentId를_통해_댓글을_조회하는_커맨드() {
        // given, when
        Mono<CommentResponse> result = commentService.findByCommentId(1L);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody().equals("Test comment"))
                .verifyComplete();

        verify(findCommentPort, times(1)).findCommentByCommentId(1L);
    }

    @Test
    void 댓글을_수정하는_메서드() {
        // given
        CommentModifyRequest request = new CommentModifyRequest();
        request.setCommentId(1L);
        request.setBody("Test content");

        // when
        Mono<CommentResponse> result = commentService.modify(1L, request);

        // then
        StepVerifier.create(result)
                .expectNextMatches(response -> response.getBody().equals("Test content"))
                .verifyComplete();

        verify(findCommentPort, times(1)).findCommentByCommentId(1L);
        verify(modifyCommentPort, times(1)).modifyComment(any(), any(), eq(request));
    }

    @Test
    void 댓글을_작성하려는데_없는_경우() {
        // given
        when(findPostPort.findPostByPostId(any())).thenReturn(Mono.empty());

        CommentRegisterRequest request = new CommentRegisterRequest();
        request.setBoardId(999L);
        request.setBody("Test comment");

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
        CommentModifyRequest request = new CommentModifyRequest();
        request.setCommentId(999L);
        request.setBody("Test content");

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
