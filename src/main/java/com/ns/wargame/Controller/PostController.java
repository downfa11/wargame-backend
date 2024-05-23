package com.ns.wargame.Controller;

import com.ns.wargame.Domain.dto.*;
import com.ns.wargame.Service.CommentService;
import com.ns.wargame.Service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final CommentService commentService;


    @PostMapping("")
    public Mono<ResponseEntity<messageEntity>> createPost(@RequestBody PostRegisterRequest request){
        return postService.create(request)
                .map(board -> ResponseEntity.ok()
                        .body(new messageEntity("Success", PostResponse.of(board))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @PatchMapping("")
    public Mono<ResponseEntity<messageEntity>> modifyPost(@RequestBody PostModifyRequest request){
        return postService.modify(request)
                .map(board -> ResponseEntity.ok()
                        .body(new messageEntity("Success", PostResponse.of(board))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
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
    public Mono<ResponseEntity<messageEntity>> findPostAllPagination(@PathVariable Long categoryId,@RequestParam int page){
        int size = 10;
        PageRequest sortedPageRequest = PageRequest.of(page, size).withSort(Sort.by("createdAt").descending());
        return postService.findPostAllPagination(categoryId,sortedPageRequest )
                .map(posts -> ResponseEntity.ok()
                        .body(new messageEntity("Success", posts)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Post is empty.")));
    }

    @GetMapping("/{boardId}/comments")
    public Mono<ResponseEntity<messageEntity>> findCommentByBoardId(Long boardId){
        return commentService.findAllByBoardId(boardId)
                .collectList()
                .flatMap(comments -> {
                    if (!comments.isEmpty())
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Success", comments)));
                    else
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail","Comment is empty.")));
                });
    }


    @GetMapping("/{boardId}")
    public Mono<ResponseEntity<messageEntity>> findPostById(@PathVariable Long boardId){
        return postService.findPostResponseById(boardId)
                .map(postResponse -> ResponseEntity.ok().body(new messageEntity("Success",postResponse)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","Post is empty.")));
    }

    @DeleteMapping("/{boardId}")
    public Mono<ResponseEntity<messageEntity>> deletePost(@PathVariable Long boardId, @RequestParam Long userId){
        return postService.deleteById(boardId, userId)
                .map(deleted -> ResponseEntity.ok().body(new messageEntity("Success", boardId)));
    }

    @PostMapping("/{boardId}/likes")
    public Mono<ResponseEntity<messageEntity>> updateLikes(@PathVariable Long boardId, @RequestBody LikesRequest request){
        return postService.updateLikes(request.getUserId(),boardId,request.getAddLike())
                .map(post -> ResponseEntity.ok().body(new messageEntity("Success", post)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","Post is empty.")));
    }
}
