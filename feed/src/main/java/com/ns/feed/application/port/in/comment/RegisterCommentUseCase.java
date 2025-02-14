package com.ns.feed.application.port.in.comment;

import com.ns.feed.dto.CommentRegisterRequest;
import com.ns.feed.dto.CommentResponse;
import reactor.core.publisher.Mono;

public interface RegisterCommentUseCase {
    Mono<CommentResponse> create(Long membershipId, CommentRegisterRequest request);
}
