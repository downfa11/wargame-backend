package com.ns.membership.application.port.out;

import reactor.core.publisher.Mono;

public interface DeleteUserPort {
    Mono<Void> delete(Long membershipId);
}
