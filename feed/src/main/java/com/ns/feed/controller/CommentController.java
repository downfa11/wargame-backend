package com.ns.feed.controller;

import com.ns.feed.Utils.JwtTokenProvider;
import com.ns.feed.entity.dto.CommentModifyRequest;
import com.ns.feed.entity.dto.CommentRegisterRequest;
import com.ns.feed.entity.dto.messageEntity;
import com.ns.feed.service.CommentService;
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
    public Mono<ResponseEntity<messageEntity>> createComment(@RequestParam Long membershipId, @RequestBody CommentRegisterRequest request, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return commentService.create(membershipId, request)
//                            .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
//                })
//                .onErrorResume(e -> Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "JwtToken is Invalid."))));

        return commentService.create(membershipId, request)
                            .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @PatchMapping("")
    public Mono<ResponseEntity<messageEntity>> modifyComment(@RequestParam Long membershipId, @RequestBody CommentModifyRequest request, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return commentService.findById(request.getCommentId())
//                            .flatMap(comment -> {
//                                if (!comment.getUserId().equals(membershipId))
//                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to modify this comment.")));
//
//                                return commentService.modify(request)
//                                        .map(updatedComment -> ResponseEntity.ok().body(new messageEntity("Success", updatedComment)))
//                                        .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Unable to modify the comment.")));
//                            });
//                })
//                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Comment not found.")));
                    return commentService.findById(request.getCommentId())
                            .flatMap(comment -> {
                                if (!comment.getUserId().equals(membershipId))
                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to modify this comment.")));

                                return commentService.modify(request)
                                        .map(updatedComment -> ResponseEntity.ok().body(new messageEntity("Success", updatedComment)))
                                        .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Unable to modify the comment.")));
                            }).defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Comment not found.")));

    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<messageEntity>> findCommentById(@PathVariable Long id, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//
//                    return commentService.findById(id)
//                            .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Comment is empty.")));
//                });
                    return commentService.findById(id)
                            .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Comment is empty.")));
    }

    @DeleteMapping("/{commentId}")
    public Mono<ResponseEntity<messageEntity>> deleteComment(@RequestParam Long membershipId, @PathVariable Long commentId, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return commentService.findById(commentId)
//                            .flatMap(comment ->
//                            {
//                                if (!comment.getUserId().equals(membershipId))
//                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to delete this comment.")));
//
//                                return commentService.deleteById(commentId)
//                                        .then(Mono.just(ResponseEntity.ok().body(new messageEntity("Success", commentId))));
//                            });
//                            })
//                .map(deleted-> ResponseEntity.ok().body(new messageEntity("Success", commentId)));
//
//    }
                    return commentService.findById(commentId)
                            .flatMap(comment ->
                            {
                                if (!comment.getUserId().equals(membershipId))
                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to delete this comment.")));

                                return commentService.deleteById(commentId)
                                        .then(Mono.just(ResponseEntity.ok().body(new messageEntity("Success", commentId))));
                            }).map(deleted-> ResponseEntity.ok().body(new messageEntity("Success", commentId)));


    }
}