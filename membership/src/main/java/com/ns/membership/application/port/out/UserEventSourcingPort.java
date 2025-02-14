package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistence.User;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserUpdateRequest;
import reactor.core.publisher.Mono;

public interface UserEventSourcingPort {
    Mono<User> createMemberByEvent(UserCreateRequest request);
    Mono<User> modifyMemberByEvent(Long membershipId, UserUpdateRequest request);
}
