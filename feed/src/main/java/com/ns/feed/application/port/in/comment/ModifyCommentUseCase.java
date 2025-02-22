package com.ns.feed.application.port.in.comment;

import com.ns.feed.dto.CommentModifyRequest;
import com.ns.feed.dto.CommentResponse;
import reactor.core.publisher.Mono;

public interface ModifyCommentUseCase {
    Mono<CommentResponse> modify(Long membershipId, CommentModifyRequest request);
}
