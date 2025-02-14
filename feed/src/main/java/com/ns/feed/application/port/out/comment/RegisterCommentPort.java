package com.ns.feed.application.port.out.comment;

import com.ns.feed.adapter.out.persistence.comment.Comment;
import com.ns.feed.dto.CommentRegisterRequest;
import reactor.core.publisher.Mono;

public interface RegisterCommentPort {
    Mono<Comment> registerComment(Long userId, String nickName, CommentRegisterRequest request);
}
