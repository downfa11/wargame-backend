package com.ns.feed.adapter.in.web;


import com.ns.common.utils.MessageEntity;
import com.ns.feed.adapter.out.persistence.post.Post;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.in.post.RegisterPostUseCase;
import com.ns.feed.application.port.out.image.UpdateImagePort;
import com.ns.feed.dto.PostRegisterRequest;
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
    private final RegisterPostUseCase registerPostUseCase;
    private final FindPostUseCase findPostUseCase;
    private final UpdateImagePort updateImagePort;

    @PostMapping("/in-progress")
    public Mono<ResponseEntity<MessageEntity>> findInProgressEvent(){
        return findPostUseCase.findInProgressEvents()
                .map(posts -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", posts)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "No events found.")));
    }

    @PostMapping("/create-events")
    public Mono<String> createEvents(@RequestParam Long userId, @RequestParam String imageUrl) {

            PostRegisterRequest request = PostRegisterRequest.builder()
                    .sortStatus(Post.SortStatus.EVENT)
                    .categoryId(1L)
                    .title("Event Title ")
                    .content("Event Content ")
                    .eventStartDate(LocalDateTime.now())
                    .eventEndDate(LocalDateTime.now().plusDays(7))
                    .build();

        return registerPostUseCase.create(userId, request)
                .flatMap(post -> updateImagePort.createImage(post.getId(), imageUrl)
                        .thenReturn(post))
                .then(Mono.just("event created"));
    }

}
