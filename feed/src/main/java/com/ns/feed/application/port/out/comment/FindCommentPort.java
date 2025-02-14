package com.ns.feed.application.port.out.comment;

import com.ns.feed.adapter.out.persistence.comment.Comment;
import com.ns.feed.dto.CommentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindCommentPort {
    Mono<Comment> findCommentByCommentId(Long commentId);
    Flux<CommentResponse> findCommentResponseByPostId(Long postId);

}
