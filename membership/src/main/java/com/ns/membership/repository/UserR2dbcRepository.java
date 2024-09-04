package com.ns.membership.repository;

import com.ns.membership.entity.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserR2dbcRepository extends ReactiveCrudRepository<User, Long> {
    Flux<User> findByName(String name);
    Flux<User> findByEmail(String email);
    Flux<User> findByAccount(String account);
    Mono<User> findByAccountAndPassword(String account,String password);
    Flux<User> findByNameOrderByIdDesc(String name);

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    Flux<User> findUsersByIdList(List<Long> ids);

    @Modifying
    @Query("DELETE FROM users WHERE name = :name")
    Mono<Void> deleteByName(String name);
}
