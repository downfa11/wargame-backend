package com.ns.membership.application.port.out;

import com.ns.membership.adapter.out.persistence.User;
import reactor.core.publisher.Mono;

public interface ModifyUserPort {
    Mono<User> update(Long id,String account, String name, String email,String password);
    Mono<User> resetPassword(Long id, String password);
}
