package com.ns.feed.controller;

import com.ns.common.MessageEntity;
import com.ns.feed.adapter.in.web.PostController;
import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.dto.BannerListWrapper;
import com.ns.feed.dto.PostRegisterRequest;
import com.ns.feed.adapter.in.web.EventController;
import com.ns.feed.application.port.in.post.RegisterPostUseCase;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.out.image.UpdateImagePort;
import java.time.LocalDateTime;
import java.util.List;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@WebFluxTest(PostController.class)
@ExtendWith(MockitoExtension.class)
public class EventControllerTest {

    @InjectMocks private EventController eventController;
    @Mock private RegisterPostUseCase registerPostUseCase;
    @Mock private FindPostUseCase findPostUseCase;
    @Mock private UpdateImagePort updateImagePort;

    private WebTestClient webTestClient;

    @BeforeEach
    public void init() {
        webTestClient = WebTestClient.bindToController(eventController).build();
    }

    @Test
    public void 현재_진행중인_이벤트만_조회하는_메서드() {
        when(findPostUseCase.findInProgressEvents()).thenReturn(Mono.just(new BannerListWrapper(List.of())));

        webTestClient.post().uri("/event/in-progress")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Success")
                .jsonPath("$.data").isEqualTo("Events Found");
    }

    @Test
    public void 이벤트를_생성하는_메서드() {
        Long userId = 1L;
        String imageUrl = "http://example.com/image.jpg";

        PostRegisterRequest request = PostRegisterRequest.builder()
                .sortStatus(Post.SortStatus.EVENT)
                .categoryId(1L)
                .title("title")
                .content("content")
                .eventStartDate(LocalDateTime.now())
                .eventEndDate(LocalDateTime.now().plusDays(7))
                .build();

        when(registerPostUseCase.create(userId, request)).thenReturn(Mono.just(Post.builder().build()));
        when(updateImagePort.createImage(any(), any())).thenReturn(Mono.empty());

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/event/create-events")
                .queryParam("userId",userId.toString())
                .queryParam("imageUrl", imageUrl)
                .build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("event created");

        verify(registerPostUseCase).create(userId, request);
        verify(updateImagePort).createImage(any(), any());
    }
}
