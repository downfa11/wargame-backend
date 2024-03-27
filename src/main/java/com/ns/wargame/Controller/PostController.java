package com.ns.wargame.Controller;

import com.ns.wargame.Domain.dto.messageEntity;
import com.ns.wargame.Service.PostService;
import com.ns.wargame.Domain.dto.PostRequest;
import com.ns.wargame.Domain.dto.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;


    @PostMapping("")
    public Mono<ResponseEntity<messageEntity>> createPost(@RequestBody PostRequest request){
        return Mono.just(ResponseEntity.ok()
                .body(new messageEntity("Success",postService.create(request.getUserId(), request.getTitle(), request.getContent())
                        .map(PostResponse::of))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","Post is empty.")));
    }

    @GetMapping("")
    public Mono<ResponseEntity<messageEntity>> findAllPost(){
        return Mono.just(ResponseEntity.ok()
                .body(new messageEntity("Success",postService.findAll()
                        .map(PostResponse::of))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","Post is empty.")));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<messageEntity>> findPostById(@PathVariable Long id){
        return postService.findById(id)
                .map(post -> ResponseEntity.ok().body(new messageEntity("Success",PostResponse.of(post))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","Post is empty.")));

    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<messageEntity>> deletePost(@PathVariable Long id){
        return postService.deleteById(id)
                .map(deleted -> ResponseEntity.ok().body(new messageEntity("Success", deleted)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","Post is empty.")));
    }
}
