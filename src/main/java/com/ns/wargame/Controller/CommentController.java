package com.ns.wargame.Controller;

import com.ns.wargame.Domain.dto.CommentModifyRequest;
import com.ns.wargame.Domain.dto.CommentRegisterRequest;
import com.ns.wargame.Domain.dto.messageEntity;
import com.ns.wargame.Service.CommentService;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("")
    public Mono<ResponseEntity<messageEntity>> createComment(@RequestBody CommentRegisterRequest request) {
        return commentService.create(request)
                .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @PatchMapping("")
    public Mono<ResponseEntity<messageEntity>> modifyComment(@RequestBody CommentModifyRequest request) {
        return commentService.modify(request)
                .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<messageEntity>> findCommentById(@PathVariable Long id) {
        return commentService.findById(id)
                .map(comment -> ResponseEntity.ok().body(new messageEntity("Success", comment)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Comment is empty.")));

    }

    @DeleteMapping("/{commentId}")
    public Mono<ResponseEntity<messageEntity>> deleteComment(@PathVariable Long commentId, @RequestParam Long userId ) {
        return commentService.deleteById(commentId, userId)
                .map(deleted-> ResponseEntity.ok().body(new messageEntity("Success", commentId)));
    }
}