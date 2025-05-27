package com.ns.membership.application.port.in;

import reactor.core.publisher.Mono;

public interface DeleteUserUseCase {
    Mono<Void> delete(Long membershipId);
}
