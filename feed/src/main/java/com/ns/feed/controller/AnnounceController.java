package com.ns.feed.controller;


import com.ns.common.MessageEntity;
import com.ns.feed.entity.Post;
import com.ns.feed.entity.Post.SortStatus;
import com.ns.feed.entity.dto.PostRegisterRequest;
import com.ns.feed.entity.dto.PostResponse;
import com.ns.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/announce")
@RequiredArgsConstructor
public class AnnounceController {

    private final PostService postService;

    @PostMapping("/latest")
    public Mono<ResponseEntity<MessageEntity>> findLatestAnnounce(@RequestParam Integer count){
        return postService.findLatestAnnounces(count)
                .map(posts -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", posts)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "No posts found.")));
    }

    @PostMapping("/create-announces")
    public String createAnnounces(@RequestParam Long userId) {
        for (int i = 1; i <= 4; i++) {
            PostRegisterRequest request = PostRegisterRequest.builder()
                    .sortStatus(SortStatus.ANNOUNCE)
                    .categoryId(1L)
                    .title("Announce Title " + i)
                    .content("Announce Content " + i)
                    .eventStartDate(null)
                    .eventEndDate(null)
                    .build();

            postService.create(userId, request).subscribe();
        }
        return "4 announces created successfully";
    }
}
