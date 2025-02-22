package com.ns.membership.application.port.in;

import com.ns.membership.dto.UserResponse;
import com.ns.membership.dto.UserUpdateRequest;
import reactor.core.publisher.Mono;

public interface ModifyUserUseCase {
    Mono<UserResponse> modify(Long membershipId, UserUpdateRequest request);
}
