package com.ns.feed.controller;

import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.in.post.RegisterPostUseCase;
import com.ns.feed.application.port.out.image.UpdateImagePort;
import com.ns.feed.dto.BannerListWrapper;
import com.ns.feed.dto.PostRegisterRequest;
import com.ns.feed.adapter.in.web.AnnounceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AnnounceControllerTest {

    @InjectMocks private AnnounceController announceController;
    @Mock private RegisterPostUseCase registerPostUseCase;
    @Mock private FindPostUseCase findPostUseCase;
    @Mock private UpdateImagePort updateImagePort;

    private WebTestClient webTestClient;

    @BeforeEach
    public void init() {
        webTestClient = WebTestClient.bindToController(announceController).build();
    }

    @Test
    public void 가장_최근_공지사항_목록을_조회하는_메서드() {
        // given
        Integer count = 5;
        List<PostRegisterRequest> mockPosts = List.of(
                PostRegisterRequest.builder()
                        .title("Announce Title 1")
                        .content("Announce Content 1")
                        .build(),
                PostRegisterRequest.builder()
                        .title("Announce Title 2")
                        .content("Announce Content 2")
                        .build()
        );

        when(findPostUseCase.findLatestAnnounces(count)).thenReturn(Mono.just(new BannerListWrapper(List.of())));

        // when
        webTestClient.post().uri("/announce/latest?count=" + count)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("Success")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data.length()").isEqualTo(2);

        // then
        verify(findPostUseCase).findLatestAnnounces(count);
    }

    @Test
    public void 공지사항을_생성하는_메서드() {
        // given
        Long userId = 1L;
        List<String> imageUrls = List.of("imageUrl1", "imageUrl2");

        PostRegisterRequest postRegisterRequest1 = PostRegisterRequest.builder()
                .title("Announce Title 1")
                .content("Announce Content 1")
                .build();

        PostRegisterRequest postRegisterRequest2 = PostRegisterRequest.builder()
                .title("Announce Title 2")
                .content("Announce Content 2")
                .build();

        Post post1 = Post.builder()
                .title("Announce Title 1")
                .content("Announce Content 1")
                .build();

        Post post2 = Post.builder()
                .title("Announce Title 2")
                .content("Announce Content 2")
                .build();

        when(registerPostUseCase.create(userId, postRegisterRequest1)).thenReturn(Mono.just(post1));
        when(registerPostUseCase.create(userId, postRegisterRequest2)).thenReturn(Mono.just(post2));

        // when
        webTestClient.post().uri("/announce/create-announces?userId=" + userId + "&imageUrls=" + imageUrls)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("announces created");

        // then
        verify(registerPostUseCase).create(userId, postRegisterRequest1);
        verify(registerPostUseCase).create(userId, postRegisterRequest2);
        verify(updateImagePort).createImage(1L, "imageUrl1");
        verify(updateImagePort).createImage(1L, "imageUrl2");
    }
}

