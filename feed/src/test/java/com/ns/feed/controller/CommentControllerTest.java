package com.ns.feed.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.feed.adapter.in.web.CommentController;
import com.ns.feed.application.port.in.comment.DeleteCommentUseCase;
import com.ns.feed.application.port.in.comment.FindCommentUseCase;
import com.ns.feed.application.port.in.comment.ModifyCommentUseCase;
import com.ns.feed.application.port.in.comment.RegisterCommentUseCase;
import com.ns.feed.dto.CommentModifyRequest;
import com.ns.feed.dto.CommentRegisterRequest;
import com.ns.feed.dto.CommentResponse;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(CommentController.class)
@ExtendWith(MockitoExtension.class)
public class CommentControllerTest {

    @InjectMocks private CommentController commentController;

    @Mock private RegisterCommentUseCase registerCommentUseCase;

    @Mock private ModifyCommentUseCase modifyCommentUseCase;

    @Mock private DeleteCommentUseCase deleteCommentUseCase;

    @Mock private FindCommentUseCase findCommentUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    public void init() {
        webTestClient = WebTestClient.bindToController(commentController).build();
    }

    @Test
    public void 댓글을_생성하는_메서드() {
        Long membershipId = 1L;
        CommentRegisterRequest request =  CommentRegisterRequest.builder()
                .boardId(1L)
                .body("content")
                .build();

        when(registerCommentUseCase.create(membershipId, request))
                .thenReturn(Mono.just(CommentResponse.builder().build()));

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/comments")
                .queryParam("membershipId",membershipId)
                .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Success")
                .jsonPath("$.data").isEqualTo("Comment created successfully");

        verify(registerCommentUseCase).create(membershipId, request);
    }

    @Test
    public void 댓글을_수정하는_메서드() {
        Long membershipId = 1L;
        CommentModifyRequest request = CommentModifyRequest.builder()
                .commentId(1L)
                .body("content")
                .build();


        when(modifyCommentUseCase.modify(membershipId, request))
                .thenReturn(Mono.just(CommentResponse.builder().build()));

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/comments")
                .queryParam("membershipId",membershipId)
                .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Success")
                .jsonPath("$.data").isEqualTo("Comment updated successfully");

        verify(modifyCommentUseCase).modify(membershipId, request);
    }

    @Test
    public void commentId로_댓글을_조회하는_메서드() {
        Long commentId = 1L;
        when(findCommentUseCase.findByCommentId(commentId))
                .thenReturn(Mono.just(CommentResponse.builder().build()));

        webTestClient.get().uri("/v1/comments/{commentId}", commentId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Success")
                .jsonPath("$.data").isEqualTo("Found comment");

        verify(findCommentUseCase).findByCommentId(commentId);
    }

    @Test
    public void 댓글을_삭제하는_메서드() {
        // Arrange
        Long membershipId = 1L;
        Long commentId = 1L;

        when(deleteCommentUseCase.delete(commentId))
                .thenReturn(Mono.empty());

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/comments/{commentId}")
                .queryParam("membershipId",membershipId)
                .build(Collections.singletonMap("membershipId", membershipId)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Success")
                .jsonPath("$.data").isEqualTo(commentId.toString());

        verify(deleteCommentUseCase).delete(commentId);
    }
}

