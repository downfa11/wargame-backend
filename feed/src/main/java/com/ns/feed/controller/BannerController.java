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
        return postService.findLatestAnnounces(count)
                .map(posts -> new BannerListWrapper(
                        posts.stream()
                                .map(post -> createBanner(post.getTitle(), "url", "https://i.namu.wiki/i/JO0DhNL-Q4qcRWtOpELVgXB3yq5OmpYyrXGjf9HOqp_U4L50uiJKHGX8vEqqHgU6RAYND8CDXFWQBodHTk4JUg.webp"))
                                .collect(Collectors.toList())
                ));
    }

    @GetMapping("/event/in-progress")
    public Mono<BannerListWrapper> getEventBanners() {
        return postService.findInProgressEvents()
                .map(posts -> new BannerListWrapper(
                        posts.stream()
                                .map(post -> createBanner(post.getTitle(), "url", "https://act-webstatic.hoyoverse.com/upload/contentweb/2022/10/27/e83aa42d20b1cb2249b4e84e1db019e3_7369846104876935515.png"))
                                .collect(Collectors.toList())
                ));
    }

    private Banner createBanner(String name, String url, String imageUrl){
        return Banner.builder()
                .name(name)
                .url(url)
                .imageUrl(imageUrl)
                .build();
    }
}
