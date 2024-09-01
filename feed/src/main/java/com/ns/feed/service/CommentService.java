package com.ns.feed.service;


import com.ns.feed.entity.Comment;
import com.ns.feed.entity.dto.CommentModifyRequest;
import com.ns.feed.entity.dto.CommentRegisterRequest;
import com.ns.feed.entity.dto.CommentResponse;
import com.ns.feed.repository.CommentR2dbcRepository;
import com.ns.feed.repository.PostR2dbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentR2dbcRepository commentR2dbcRepository;
    private final PostR2dbcRepository postR2dbcRepository;
    private final PostService postService;

    public Mono<CommentResponse> create(Long userId, CommentRegisterRequest request) {
        long boardId = request.getBoardId();
        String content = request.getBody();

        return postService.findPostById(boardId)
                .flatMap(post -> {
                    Long curComments = post.getComments();
                    post.setComments(curComments + 1);
                    return postR2dbcRepository.save(post)
                            .then(commentR2dbcRepository.save(Comment.builder()
                                    .userId(userId)
                                    .boardId(boardId)
                                    .content(content)
                                    .build()));
                })
                .flatMap(savedComment -> userService.findById(savedComment.getUserId())
                        .map(user -> {
                            CommentResponse commentResponse = CommentResponse.of(savedComment);
                            commentResponse.setNickname(user.getName());
                            return commentResponse;
                        }));
    }


    public Mono<CommentResponse> modify(CommentModifyRequest request) {
        String content = request.getBody();

        return commentR2dbcRepository.findById(request.getCommentId())
                .flatMap(post -> {
                    post.setContent(content);
                    return commentR2dbcRepository.save(post);
                }).flatMap(savedComment -> userService.findById(savedComment.getUserId())
                        .map(user -> {
                            CommentResponse commentResponse = CommentResponse.of(savedComment);
                            commentResponse.setNickname(user.getName());
                            return commentResponse;
                        }))
                .switchIfEmpty(Mono.error(new RuntimeException("Comment or category not found")));
    }
    public Mono<CommentResponse> findById(Long id){
        return commentR2dbcRepository.findById(id).flatMap(comment -> userService.findById(comment.getUserId())
                .map(user -> {
                    CommentResponse commentResponse = CommentResponse.of(comment);
                    commentResponse.setNickname(user.getName());
                    return commentResponse;
                }));
    }

    public Flux<Comment> findAllByBoardId(Long boardId) {return commentR2dbcRepository.findByBoardId(boardId);}

    public Mono<Void> deleteById(Long commentId) {
        return commentR2dbcRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new RuntimeException("Comment not found")))
                .flatMap(comment -> {
                    long boardId = comment.getBoardId();
                    return postService.findPostById(boardId)
                            .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                            .flatMap(post -> {
                                Long curComments = post.getComments();
                                post.setComments(curComments - 1);
                                return postR2dbcRepository.save(post);
                            })
                            .then(commentR2dbcRepository.deleteById(commentId));
                });
    }
}
