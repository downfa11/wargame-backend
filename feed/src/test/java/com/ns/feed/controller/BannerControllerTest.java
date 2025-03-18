package com.ns.feed.controller;

import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.dto.BannerListWrapper;
import com.ns.feed.adapter.in.web.BannerController;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BannerControllerTest {

    @InjectMocks
    private BannerController bannerController;

    @Mock
    private FindPostUseCase findPostUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    public void init() {
        webTestClient = WebTestClient.bindToController(bannerController).build();
    }

    @Test
    public void testGetLatestBanners() {
        int count = 5;
        BannerListWrapper mockBanners = new BannerListWrapper(List.of());
        when(findPostUseCase.findLatestAnnounces(count)).thenReturn(Mono.just(mockBanners));

        webTestClient.get().uri("/banner/announce/latest?count=" + count)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BannerListWrapper.class)
                .isEqualTo(mockBanners);

        verify(findPostUseCase).findLatestAnnounces(count);
    }

    @Test
    public void testGetEventBanners() {
        BannerListWrapper mockEventBanners = new BannerListWrapper(List.of());
        when(findPostUseCase.findInProgressEvents()).thenReturn(Mono.just(mockEventBanners));

        webTestClient.get().uri("/banner/event/in-progress")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BannerListWrapper.class)
                .isEqualTo(mockEventBanners);

        verify(findPostUseCase).findInProgressEvents();
    }
}
