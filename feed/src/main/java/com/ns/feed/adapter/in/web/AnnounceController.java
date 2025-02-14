package com.ns.feed.adapter.in.web;


import com.ns.common.utils.MessageEntity;
import com.ns.feed.adapter.out.persistence.post.Post.SortStatus;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.in.post.RegisterPostUseCase;
import com.ns.feed.application.port.out.image.UpdateImagePort;
import com.ns.feed.dto.PostRegisterRequest;
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
    private final RegisterPostUseCase registerPostUseCase;
    private final FindPostUseCase findPostUseCase;
    private final UpdateImagePort updateImagePort;

    @PostMapping("/latest")
    public Mono<ResponseEntity<MessageEntity>> findLatestAnnounce(@RequestParam Integer count){
        return findPostUseCase.findLatestAnnounces(count)
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

                    return registerPostUseCase.create(userId, request)
                            .flatMap(post -> {
                                Flux.fromIterable(imageUrls)
                                        .flatMap(imageUrl -> updateImagePort.createImage(post.getId(), imageUrl))
                                        .subscribe();
                                return Mono.just(post);
                            });
                })
                .then(Mono.just("announces created"));
    }
}
