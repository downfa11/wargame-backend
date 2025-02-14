package com.ns.feed.application.port.out.comment;

import com.ns.feed.adapter.out.persistence.comment.Comment;
import com.ns.feed.dto.CommentModifyRequest;
import reactor.core.publisher.Mono;

public interface ModifyCommentPort {
    Mono<Comment> modifyComment(Long userId, String nickName, CommentModifyRequest request);
}
