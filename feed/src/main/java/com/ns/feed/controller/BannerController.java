package com.ns.feed.controller;

import com.ns.feed.entity.dto.Banner;
import com.ns.feed.entity.dto.BannerListWrapper;
import com.ns.feed.service.PostService;
import java.util.stream.Collectors;
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

    private final PostService postService;

    @GetMapping("/announce/latest")
    public Mono<BannerListWrapper> getLatestBanners(@RequestParam(defaultValue = "5") int count) {
        return postService.findLatestAnnounces(count);
    }

    @GetMapping("/event/in-progress")
    public Mono<BannerListWrapper> getEventBanners() {
        return postService.findInProgressEvents();
    }
}
