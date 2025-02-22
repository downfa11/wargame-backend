package com.ns.feed.adapter.in.web;

import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.dto.BannerListWrapper;
import com.ns.feed.application.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/banner")
public class BannerController {
    private final FindPostUseCase findPostUseCase;

    @GetMapping("/announce/latest")
    public Mono<BannerListWrapper> getLatestBanners(@RequestParam(defaultValue = "5") int count) {
        return findPostUseCase.findLatestAnnounces(count);
    }

    @GetMapping("/event/in-progress")
    public Mono<BannerListWrapper> getEventBanners() {
        return findPostUseCase.findInProgressEvents();
    }
}
