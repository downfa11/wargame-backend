package com.ns.feed.controller;


import com.ns.common.MessageEntity;
import com.ns.feed.entity.Post;
import com.ns.feed.entity.dto.PostRegisterRequest;
import com.ns.feed.entity.dto.PostResponse;
import com.ns.feed.service.PostService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {
    private final PostService postService;

    @PostMapping("/in-progress")
    public Mono<ResponseEntity<MessageEntity>> findInProgressEvent(){
        return postService.findInProgressEvents()
                .map(posts -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", posts)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "No events found.")));
    }

    @PostMapping("/create-events")
    public String createEvents(@RequestParam Long userId) {
        for (int i = 1; i <= 3; i++) {
            PostRegisterRequest request = PostRegisterRequest.builder()
                    .sortStatus(Post.SortStatus.EVENT)
                    .categoryId(1L)
                    .title("Event Title " + i)
                    .content("Event Content " + i)
                    .eventStartDate(LocalDateTime.now())
                    .eventEndDate(LocalDateTime.now().plusDays(7))
                    .build();

            postService.create(userId, request).subscribe();
        }
        return "3 events created successfully";
    }

}
