package com.ns.feed.adapter.in.web;

import com.ns.common.utils.MessageEntity;
import com.ns.common.utils.JwtTokenProvider;
import com.ns.feed.dto.CommentModifyRequest;
import com.ns.feed.dto.CommentRegisterRequest;
import com.ns.feed.application.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
public class CommentController {
    private final String POST_RESULT_EMPTY_ERROR_MESSAGE = "Post is empty.";
    private final String UNAUTHORIZED_MODIFY_COMMENT_ERROR_MESSAGE = "Unauthorized to modify this comment.";
    private final String UNAUTHORIZED_DELETE_COMMENT_ERROR_MESSAGE = "Unauthorized to delete this comment.";
    private final String NOT_FOUND_COMMENT_ERROR_MESSAGE = "Comment not found.";
    private final String UNABLE_TO_MODIFY_COMMENT_ERROR_MESSAGE = "Unable to modify the comment.";


    private final CommentService commentService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("")
    public Mono<ResponseEntity<MessageEntity>> createComment(@RequestParam Long membershipId, @RequestBody CommentRegisterRequest request, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return commentService.create(membershipId, request)
//                            .map(comment -> ResponseEntity.ok().body(new MessageEntity("Success", comment)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post is empty.")));
//                })
//                .onErrorResume(e -> Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "JwtToken is Invalid."))));

        return commentService.create(membershipId, request)
                            .map(comment -> ResponseEntity.ok().body(new MessageEntity("Success", comment)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @PatchMapping("")
    public Mono<ResponseEntity<MessageEntity>> modifyComment(@RequestParam Long membershipId, @RequestBody CommentModifyRequest request, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return commentService.findById(request.getCommentId())
//                            .flatMap(comment -> {
//                                if (!comment.getUserId().equals(membershipId))
//                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Unauthorized to modify this comment.")));
//
//                                return commentService.modify(request)
//                                        .map(updatedComment -> ResponseEntity.ok().body(new MessageEntity("Success", updatedComment)))
//                                        .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Unable to modify the comment.")));
//                            });
//                })
//                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Comment not found.")));
                    return commentService.findById(request.getCommentId())
                            .flatMap(comment -> {
                                if (!comment.getUserId().equals(membershipId))
                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", UNAUTHORIZED_MODIFY_COMMENT_ERROR_MESSAGE)));

                                return commentService.modify(request)
                                        .map(updatedComment -> ResponseEntity.ok().body(new MessageEntity("Success", updatedComment)))
                                        .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", UNABLE_TO_MODIFY_COMMENT_ERROR_MESSAGE)));
                            }).defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", NOT_FOUND_COMMENT_ERROR_MESSAGE)));

    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<MessageEntity>> findCommentById(@PathVariable Long id, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//
//                    return commentService.findById(id)
//                            .map(comment -> ResponseEntity.ok().body(new MessageEntity("Success", comment)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Comment is empty.")));
//                });
                    return commentService.findById(id)
                            .map(comment -> ResponseEntity.ok().body(new MessageEntity("Success", comment)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @DeleteMapping("/{commentId}")
    public Mono<ResponseEntity<MessageEntity>> deleteComment(@RequestParam Long membershipId, @PathVariable Long commentId, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return commentService.findById(commentId)
//                            .flatMap(comment ->
//                            {
//                                if (!comment.getUserId().equals(membershipId))
//                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Unauthorized to delete this comment.")));
//
//                                return commentService.deleteById(commentId)
//                                        .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", commentId))));
//                            });
//                            })
//                .map(deleted-> ResponseEntity.ok().body(new MessageEntity("Success", commentId)));
//
//    }
                    return commentService.findById(commentId)
                            .flatMap(comment ->
                            {
                                if (!comment.getUserId().equals(membershipId))
                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", UNAUTHORIZED_DELETE_COMMENT_ERROR_MESSAGE)));

                                return commentService.deleteByCommentId(commentId)
                                        .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", commentId))));
                            }).map(deleted-> ResponseEntity.ok().body(new MessageEntity("Success", commentId)));


    }
}