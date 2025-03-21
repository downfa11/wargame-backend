package com.ns.feed.adapter.in.web;


import com.ns.common.MessageEntity;
import com.ns.common.utils.JwtTokenProvider;
import com.ns.feed.application.port.in.UpdateLikeUseCase;
import com.ns.feed.application.port.in.comment.FindCommentUseCase;
import com.ns.feed.application.port.in.post.DeletePostUseCase;
import com.ns.feed.application.port.in.post.FindPostUseCase;
import com.ns.feed.application.port.in.post.ModifyPostUseCase;
import com.ns.feed.application.port.in.post.RegisterPostUseCase;
import com.ns.feed.dto.PostModifyRequest;
import com.ns.feed.dto.PostRegisterRequest;
import com.ns.feed.dto.PostResponse;
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
    private final String POST_RESULT_EMPTY_ERROR_MESSAGE = "Post is empty.";
    private final String UNAUTHORIZED_MODIFY_POST_ERROR_MESSAGE = "Unauthorized to modify this post.";
    private final String UNAUTHORIZED_DELETE_POST_ERROR_MESSAGE = "Unauthorized to delete this post.";
    private final String NOT_FOUND_POST_ERROR_MESSAGE = "Post not found.";
    private final String NOT_FOUND_COMMENT_ERROR_MESSAGE = "Comment not found.";
    private final String POST_DELETE_SUCCESS_MESSAGE = "Post deleted successfully.";


    private final JwtTokenProvider jwtTokenProvider;

    private final RegisterPostUseCase registerBoardUseCase;
    private final ModifyPostUseCase modifyPostUseCase;
    private final DeletePostUseCase deletePostUseCase;
    private final FindPostUseCase findPostUseCase;
    private final FindCommentUseCase findCommentUseCase;
    private final UpdateLikeUseCase updateLikeUseCase;

    @PostMapping("")
    public Mono<ResponseEntity<MessageEntity>> createPost(@RequestBody PostRegisterRequest request,
                                                          @RequestParam Long membershipId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                return postService.create(membershipId,request)
//                    .map(board -> ResponseEntity.ok()
//                        .body(new MessageEntity("Success", PostResponse.of(board))))
//                    .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post is empty.")));
//                });
        return registerBoardUseCase.create(membershipId, request)
                .map(board -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", PostResponse.of(board))))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @PatchMapping("")
    public Mono<ResponseEntity<MessageEntity>> modifyPost(@RequestParam Long membershipId,
                                                          @RequestBody PostModifyRequest request,
                                                          ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return postService.findPostById(request.getBoardId())
//                            .flatMap(post -> {
//                                if(!post.getUserId().equals(membershipId))
//                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Unauthorized to modify this post.")));
//
//                                return postService.modify(request)
//                                        .map(board -> ResponseEntity.ok()
//                                                .body(new MessageEntity("Success", PostResponse.of(board))))
//                                        .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post is empty.")));
//                            });
//                });
        return modifyPostUseCase.modify(membershipId, request)
                .map(board -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", PostResponse.of(board))))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @GetMapping("")
    public Mono<ResponseEntity<MessageEntity>> findAllPost() {
        return findPostUseCase.findPostsAll()
                .collectList()
                .map(boards -> boards.stream()
                        .map(PostResponse::of)
                        .collect(Collectors.toList()))
                .map(boardResponses -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", boardResponses)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @GetMapping("/all/{categoryId}")
    public Mono<ResponseEntity<MessageEntity>> findPostAllPagination(@PathVariable Long categoryId,
                                                                     @RequestParam int page,
                                                                     ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    int size = 10;
//                    PageRequest sortedPageRequest = PageRequest.of(page, size).withSort(Sort.by("createdAt").descending());
//                    return postService.findPostAllPagination(categoryId, sortedPageRequest)
//                            .map(posts -> ResponseEntity.ok()
//                                    .body(new MessageEntity("Success", posts)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post is empty.")));
//                });
        int size = 10;
        PageRequest sortedPageRequest = PageRequest.of(page, size).withSort(Sort.by("createdAt").descending());
        return findPostUseCase.findPostAllPagination(categoryId, sortedPageRequest)
                .map(posts -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", posts)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @GetMapping("/{boardId}/comments")
    public Mono<ResponseEntity<MessageEntity>> findCommentByBoardId(Long boardId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return commentService.findAllByBoardId(boardId)
//                            .collectList()
//                            .flatMap(comments -> {
//                                if (!comments.isEmpty())
//                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", comments)));
//                                else
//                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Comment is empty.")));
//                            });
//                });

        return findCommentUseCase.findAllByBoardId(boardId)
                .collectList()
                .flatMap(comments -> {
                    if (!comments.isEmpty()) {
                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", comments)));
                    } else {
                        return Mono.just(
                                ResponseEntity.ok().body(new MessageEntity("Fail", NOT_FOUND_COMMENT_ERROR_MESSAGE)));
                    }
                });
    }


    @GetMapping("/{boardId}")
    public Mono<ResponseEntity<MessageEntity>> findPostById(@PathVariable Long boardId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return postService.findPostResponseById(boardId)
//                            .map(postResponse -> ResponseEntity.ok().body(new MessageEntity("Success", postResponse)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post is empty.")));
//                });

        return findPostUseCase.findPostResponseById(boardId)
                .map(postResponse -> ResponseEntity.ok().body(new MessageEntity("Success", postResponse)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @GetMapping("/updates/{boardId}")
    public Mono<ResponseEntity<MessageEntity>> updatePostById(@PathVariable Long boardId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return postService.updatePostResponseById(boardId)
//                            .map(postResponse -> ResponseEntity.ok().body(new MessageEntity("Success", postResponse)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post is empty.")));
//                });

        return findPostUseCase.updatePostResponseById(boardId)
                .map(postResponse -> ResponseEntity.ok().body(new MessageEntity("Success", postResponse)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }

    @DeleteMapping("/{boardId}")
    public Mono<ResponseEntity<MessageEntity>> deletePost(@RequestParam Long membershipId, @PathVariable Long boardId,
                                                          ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorized or boardId is incorrect.")));
//                    }
//
//                    return postService.findPostById(boardId)
//                            .flatMap(post -> {
//                                if (!post.getUserId().equals(membershipId)) {
//                                    return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Unauthorized to delete this post.")));
//                                }
//
//                                return postService.deleteById(boardId)
//                                        .map(deleted -> ResponseEntity.ok().body(new MessageEntity("Success", "Post deleted successfully.")))
//                                        .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post not found.")));
//                            });
//                });

        return deletePostUseCase.deleteById(boardId)
                .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", POST_DELETE_SUCCESS_MESSAGE))))
                .onErrorReturn(ResponseEntity.ok().body(new MessageEntity("Fail", NOT_FOUND_POST_ERROR_MESSAGE)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", NOT_FOUND_POST_ERROR_MESSAGE)));
    }


    @PostMapping("/{boardId}/likes")
    public Mono<ResponseEntity<MessageEntity>> updateLikes(@RequestParam Long membershipId, @PathVariable Long boardId,
                                                           ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return postService.updateLikes(membershipId, boardId)
//                            .map(likes -> ResponseEntity.ok().body(new MessageEntity("Success", "post likes : "+likes)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Post is empty.")));
//                });
        return updateLikeUseCase.updateLikes(membershipId, boardId)
                .map(likes -> ResponseEntity.ok().body(new MessageEntity("Success", "post likes : " + likes)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", POST_RESULT_EMPTY_ERROR_MESSAGE)));
    }
}
