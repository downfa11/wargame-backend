package com.ns.wargame.Controller;

import com.ns.wargame.Domain.dto.CommentModifyRequest;
import com.ns.wargame.Domain.dto.CommentRegisterRequest;
import com.ns.wargame.Domain.dto.messageEntity;
import com.ns.wargame.Service.CommentService;
import com.ns.wargame.Utils.JwtTokenProvider;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("")
    public Mono<ResponseEntity<messageEntity>> createComment(@RequestBody CommentRegisterRequest request, ServerWebExchange exchange) {
        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }
                    return commentService.create(request)
                            .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
                })
                .onErrorResume(e -> Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "JwtToken is Invalid."))));
    }

    @PatchMapping("")
    public Mono<ResponseEntity<messageEntity>> modifyComment(@RequestBody CommentModifyRequest request, ServerWebExchange exchange) {
        //todo. 본인 여부
        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }
                    return commentService.modify(request)
                .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
            });
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<messageEntity>> findCommentById(@PathVariable Long id, ServerWebExchange exchange) {

        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }

                    return commentService.findById(id)
                            .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Comment is empty.")));
                });
    }

    @DeleteMapping("/{commentId}")
    public Mono<ResponseEntity<messageEntity>> deleteComment(@PathVariable Long commentId, @RequestParam Long userId , ServerWebExchange exchange) {
        // todo. jwt 권한과 본인 여부

        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }
                    return commentService.deleteById(commentId, userId)
                .map(deleted-> ResponseEntity.ok().body(new messageEntity("Success", commentId)));
        });
    }
}