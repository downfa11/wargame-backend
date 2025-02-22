package com.ns.membership.application.port.in;

import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserResponse;
import reactor.core.publisher.Mono;

public interface RegisterUserUseCase {
    Mono<UserResponse> create(UserCreateRequest request);
}
