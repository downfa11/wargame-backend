package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistence.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FindUserPort {
     Mono<User> findUserByMembershipId(Long membershipId);
     Flux<User> findAll();
     Mono<User> findByAccount(String account);
}
