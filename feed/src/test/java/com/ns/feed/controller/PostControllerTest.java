package com.ns.feed.controller;


import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.common.MessageEntity;
import com.ns.feed.adapter.in.web.PostController;
import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.adapter.out.persistence.post.PostR2dbcRepository;
import com.ns.feed.application.port.in.UpdateLikeUseCase;
import com.ns.feed.application.port.in.comment.FindCommentUseCase;
import com.ns.feed.application.port.in.post.DeletePostUseCase;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.in.post.ModifyPostUseCase;
import com.ns.feed.application.port.in.post.RegisterPostUseCase;
import com.ns.feed.dto.PostModifyRequest;
import com.ns.feed.dto.PostRegisterRequest;
import com.ns.feed.dto.PostResponse;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(PostController.class)
class PostControllerTest {

    @Autowired private WebTestClient webTestClient;

    @MockBean private RegisterPostUseCase registerPostUseCase;

    @MockBean private ModifyPostUseCase modifyPostUseCase;

    @MockBean private DeletePostUseCase deletePostUseCase;

    @MockBean private FindPostUseCase findPostUseCase;

    @MockBean private FindCommentUseCase findCommentUseCase;

    @MockBean private UpdateLikeUseCase updateLikeUseCase;


    private PostResponse postResponse;

    @BeforeEach
    void init(){
        postResponse = PostResponse.builder()
                .id(1L)
                .userId(1L)
                .title("title")
                .content("contents")
                .nickname("player")
                .build();
    }

    @Test
    void 게시글을_생성하는_메서드() {
        Long membershipId = 1L;
        PostRegisterRequest request = PostRegisterRequest.builder()
                .title("title")
                .content("content")
                .categoryId(1L)
                .build();


        when(registerPostUseCase.create(anyLong(), any(PostRegisterRequest.class)))
                .thenReturn(Mono.just(Post.builder().build()));

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/posts")
                        .queryParam("membershipId", membershipId)
                        .build())
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assert entity.getMessage().equals("Success");
                    assert entity.getResult() != null;
                });

        verify(registerPostUseCase, times(1)).create(anyLong(), any(PostRegisterRequest.class));
    }

    @Test
    void 게시글을_수정하는_메서드() {
        PostModifyRequest request = PostModifyRequest.builder()
                .boardId(1L)
                .title("title")
                .content("content")
                .build();

        when(modifyPostUseCase.modify(anyLong(), any(PostModifyRequest.class)))
                .thenReturn(Mono.just(Post.builder().build()));

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/player/increase-elo/event")
                        .queryParam("membershipId","1")
                        .build())
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assert entity.getMessage().equals("Success");
                    assert entity.getResult() != null;
                });

        verify(modifyPostUseCase, times(1)).modify(anyLong(), any(PostModifyRequest.class));
    }

    @Test
    void PostId로_조회하는_메서드() {
        Long boardId = 1L;
        when(findPostUseCase.findPostResponseById(boardId))
                .thenReturn(Mono.just(postResponse));

        webTestClient.get()
                .uri("/v1/posts/{boardId}", boardId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assert entity.getMessage().equals("Success");
                    assert entity.getResult() != null;
                });

        verify(findPostUseCase, times(1)).findPostResponseById(boardId);
    }

    @Test
    void PostId로_조회하는_메서드_NotFound() {
        Long boardId = 1L;
        when(findPostUseCase.findPostResponseById(boardId))
                .thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/v1/posts/{boardId}", boardId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assert entity.getMessage().equals("Fail");
                    assert entity.getResult().equals("Post is empty.");
                });

        verify(findPostUseCase, times(1)).findPostResponseById(boardId);
    }

    @Test
    void 게시글을_삭제하는_메서드() {
        Long boardId = 1L;
        Long membershipId = 1L;

        when(deletePostUseCase.deleteById(boardId)).thenReturn(Mono.empty());

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/v1/posts/{boardId}")
                        .queryParam("membershipId",membershipId)
                        .build(Collections.singletonMap("boardId", boardId)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assert entity.getMessage().equals("Success");
                    assert entity.getResult().equals("Post deleted successfully.");
                });

        verify(deletePostUseCase, times(1)).deleteById(boardId);
    }

    @Test
    void 게시글을_삭제하는_메서드_NotFound() {
        Long boardId = 1L;
        Long membershipId = 1L;

        when(deletePostUseCase.deleteById(boardId)).thenReturn(Mono.error(new RuntimeException("Post not found.")));

        webTestClient.delete().uri(uriBuilder -> uriBuilder.path("/v1/posts/{boardId}")
                        .queryParam("membershipId",membershipId)
                        .build(Collections.singletonMap("boardId", boardId)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assert entity.getMessage().equals("Fail");
                    assert entity.getResult().equals("Post not found.");
                });

        verify(deletePostUseCase, times(1)).deleteById(boardId);
    }

    @Test
    void 게시글에_좋아요를_업데이트하는_경우() {
        Long boardId = 1L;
        Long membershipId = 1L;

        when(updateLikeUseCase.updateLikes(membershipId, boardId)).thenReturn(Mono.just(1L));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/v1/posts/{boardId}/likes")
                        .queryParam("membershipId",membershipId)
                        .build(Collections.singletonMap("boardId", boardId)))
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .value(entity -> {
                    assert entity.getMessage().equals("Success");
                    assert entity.getResult().equals("post likes : 1");
                });

        verify(updateLikeUseCase, times(1)).updateLikes(membershipId, boardId);
    }

}
