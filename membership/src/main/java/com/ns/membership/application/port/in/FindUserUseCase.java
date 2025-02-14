package com.ns.membership.application.port.in;

import com.ns.membership.dto.PostSummary;
import com.ns.membership.dto.UserResponse;
import java.util.List;
import reactor.core.publisher.Mono;

public interface FindUserUseCase {
    Mono<UserResponse> findUserByMembershipId(Long membershipId);
    Mono<List<UserResponse>> findAll();
    Mono<List<PostSummary>> getUserPosts(Long membershipId);
}
