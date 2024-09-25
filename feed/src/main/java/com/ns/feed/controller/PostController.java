package com.ns.feed.controller;


import com.ns.common.messageEntity;
import com.ns.feed.Utils.JwtTokenProvider;
import com.ns.feed.entity.dto.PostModifyRequest;
import com.ns.feed.entity.dto.PostRegisterRequest;
import com.ns.feed.entity.dto.PostResponse;
import com.ns.feed.service.CommentService;
import com.ns.feed.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("")
    public Mono<ResponseEntity<messageEntity>> createPost(@RequestBody PostRegisterRequest request, @RequestParam Long membershipId, ServerWebExchange exchange){
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                return postService.create(membershipId,request)
//                    .map(board -> ResponseEntity.ok()
//                        .body(new messageEntity("Success", PostResponse.of(board))))
//                    .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
//                });
                    return postService.create(membershipId,request)
                            .map(board -> ResponseEntity.ok()
                                    .body(new messageEntity("Success", PostResponse.of(board))))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @PatchMapping("")
    public Mono<ResponseEntity<messageEntity>> modifyPost(@RequestParam Long membershipId, @RequestBody PostModifyRequest request, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return postService.findPostById(request.getBoardId())
//                            .flatMap(post -> {
//                                if(!post.getUserId().equals(membershipId))
//                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to modify this post.")));
//
//                                return postService.modify(request)
//                                        .map(board -> ResponseEntity.ok()
//                                                .body(new messageEntity("Success", PostResponse.of(board))))
//                                        .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
//                            });
//                });
                    return postService.findPostById(request.getBoardId())
                            .flatMap(post -> {
                                if(!post.getUserId().equals(membershipId))
                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to modify this post.")));

                                return postService.modify(request)
                                        .map(board -> ResponseEntity.ok()
                                                .body(new messageEntity("Success", PostResponse.of(board))))
                                        .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
                            });
    }

    @GetMapping("")
    public Mono<ResponseEntity<messageEntity>> findAllPost(){
        return postService.findAll()
                .collectList()
                .map(boards -> boards.stream()
                        .map(PostResponse::of)
                        .collect(Collectors.toList()))
                .map(boardResponses -> ResponseEntity.ok()
                        .body(new messageEntity("Success", boardResponses)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @GetMapping("/all/{categoryId}")
    public Mono<ResponseEntity<messageEntity>> findPostAllPagination(@PathVariable Long categoryId,@RequestParam int page, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    int size = 10;
//                    PageRequest sortedPageRequest = PageRequest.of(page, size).withSort(Sort.by("createdAt").descending());
//                    return postService.findPostAllPagination(categoryId, sortedPageRequest)
//                            .map(posts -> ResponseEntity.ok()
//                                    .body(new messageEntity("Success", posts)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
//                });
                    int size = 10;
                    PageRequest sortedPageRequest = PageRequest.of(page, size).withSort(Sort.by("createdAt").descending());
                    return postService.findPostAllPagination(categoryId, sortedPageRequest)
                            .map(posts -> ResponseEntity.ok()
                                    .body(new messageEntity("Success", posts)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @GetMapping("/{boardId}/comments")
    public Mono<ResponseEntity<messageEntity>> findCommentByBoardId(Long boardId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return commentService.findAllByBoardId(boardId)
//                            .collectList()
//                            .flatMap(comments -> {
//                                if (!comments.isEmpty())
//                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Success", comments)));
//                                else
//                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Comment is empty.")));
//                            });
//                });

                    return commentService.findAllByBoardId(boardId)
                            .collectList()
                            .flatMap(comments -> {
                                if (!comments.isEmpty())
                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Success", comments)));
                                else
                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Comment is empty.")));
                            });
    }


    @GetMapping("/{boardId}")
    public Mono<ResponseEntity<messageEntity>> findPostById(@PathVariable Long boardId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return postService.findPostResponseById(boardId)
//                            .map(postResponse -> ResponseEntity.ok().body(new messageEntity("Success", postResponse)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
//                });

                    return postService.findPostResponseById(boardId)
                            .map(postResponse -> ResponseEntity.ok().body(new messageEntity("Success", postResponse)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @GetMapping("/updates/{boardId}")
    public Mono<ResponseEntity<messageEntity>> updatePostById(@PathVariable Long boardId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return postService.updatePostResponseById(boardId)
//                            .map(postResponse -> ResponseEntity.ok().body(new messageEntity("Success", postResponse)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
//                });

                    return postService.updatePostResponseById(boardId)
                            .map(postResponse -> ResponseEntity.ok().body(new messageEntity("Success", postResponse)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @DeleteMapping("/{boardId}")
    public Mono<ResponseEntity<messageEntity>> deletePost(@RequestParam Long membershipId, @PathVariable Long boardId, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorized or boardId is incorrect.")));
//                    }
//
//                    return postService.findPostById(boardId)
//                            .flatMap(post -> {
//                                if (!post.getUserId().equals(membershipId)) {
//                                    return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to delete this post.")));
//                                }
//
//                                return postService.deleteById(boardId)
//                                        .map(deleted -> ResponseEntity.ok().body(new messageEntity("Success", "Post deleted successfully.")))
//                                        .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post not found.")));
//                            });
//                });

        return postService.findPostById(boardId)
                .flatMap(post -> {
                    if (!post.getUserId().equals(membershipId)) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Unauthorized to delete this post.")));
                    }

                    return postService.deleteById(boardId)
                            .then(Mono.just(ResponseEntity.ok().body(new messageEntity("Success", "Post deleted successfully."))))
                            .onErrorReturn(ResponseEntity.ok().body(new messageEntity("Fail", "Post not found.")));
                })
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post not found.")));
    }


    @PostMapping("/{boardId}/likes")
    public Mono<ResponseEntity<messageEntity>> updateLikes(@RequestParam Long membershipId, @PathVariable Long boardId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return postService.updateLikes(membershipId, boardId)
//                            .map(likes -> ResponseEntity.ok().body(new messageEntity("Success", "post likes : "+likes)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
//                });
                    return postService.updateLikes(membershipId, boardId)
                            .map(likes -> ResponseEntity.ok().body(new messageEntity("Success", "post likes : "+likes)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }
}
