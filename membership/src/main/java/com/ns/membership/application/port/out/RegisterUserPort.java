package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistence.User;
import com.ns.membership.dto.UserCreateRequest;
import reactor.core.publisher.Mono;

public interface RegisterUserPort {
    Mono<User> create(UserCreateRequest request, String aggregateIdentifier);
}
