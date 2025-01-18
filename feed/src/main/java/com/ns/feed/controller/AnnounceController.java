package com.ns.feed.controller;


import com.ns.common.utils.MessageEntity;
import com.ns.feed.entity.Post.SortStatus;
import com.ns.feed.entity.dto.PostRegisterRequest;
import com.ns.feed.service.ImageService;
import com.ns.feed.service.PostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/announce")
@RequiredArgsConstructor
public class AnnounceController {

    private final PostService postService;
    private final ImageService imageService;

    @PostMapping("/latest")
    public Mono<ResponseEntity<MessageEntity>> findLatestAnnounce(@RequestParam Integer count){
        return postService.findLatestAnnounces(count)
                .map(posts -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", posts)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "No posts found.")));
    }

    @PostMapping("/create-announces")
    public Mono<String> createAnnounces(@RequestParam Long userId, @RequestParam List<String> imageUrls) {
        return Flux.range(1, imageUrls.size())
                .flatMap(i -> {
                    PostRegisterRequest request = PostRegisterRequest.builder()
                            .sortStatus(SortStatus.ANNOUNCE)
                            .categoryId(1L)
                            .title("Announce Title " + i)
                            .content("Announce Content " + i)
                            .eventStartDate(null)
                            .eventEndDate(null)
                            .build();

                    return postService.create(userId, request)
                            .flatMap(post -> {
                                Flux.fromIterable(imageUrls)
                                        .flatMap(imageUrl -> imageService.createImage(post.getId(), imageUrl))  // 이미지 저장
                                        .subscribe();
                                return Mono.just(post);
                            });
                })
                .then(Mono.just("announces created"));
    }
}
