package com.ns.membership.adapter.out.persistence;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.membership.application.port.out.FindUserPort;
import com.ns.membership.application.port.out.ModifyUserPort;
import com.ns.membership.application.port.out.RegisterUserPort;
import com.ns.membership.dto.UserCreateRequest;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class UserPersistenceAdapter implements RegisterUserPort, ModifyUserPort, FindUserPort {

    private final UserR2dbcRepository userR2dbcRepository;


    @Override
    public Mono<User> create(UserCreateRequest request, String aggregateIdentifier){
        Flux<User> existingUsers = Flux.concat(
                userR2dbcRepository.findByName(request.getName()),
                userR2dbcRepository.findByEmail(request.getEmail()));

        return existingUsers.collectList()
                .flatMap(existingUserList -> {
                    if (existingUserList.isEmpty()) {
                        return userR2dbcRepository.save(createUser(aggregateIdentifier, request));
                    } else {
                        return Mono.error(new RuntimeException("Duplicated data."));
                    }
                });
    }

    private User createUser(String aggregateIdentifier, UserCreateRequest request){
        return User.builder()
                .aggregateIdentifier(aggregateIdentifier)
                .account(request.getAccount())
                .password(request.getPassword())
                .name(request.getName())
                .email(request.getEmail())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }


    @Override
    public Mono<User> update(Long id,String account, String name, String email,String password) {
        return userR2dbcRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setEmail(email);
                    u.setAccount(account);
                    u.setPassword(password);
                    u.setUpdatedAt(LocalDateTime.now());
                    return userR2dbcRepository.save(u);
                });
    }

    @Override
    public Mono<User> resetPassword(Long id,String password) {
        return userR2dbcRepository.findById(id)
                .flatMap(u -> {
                    u.setPassword(password);
                    u.setUpdatedAt(LocalDateTime.now());
                    return userR2dbcRepository.save(u);
                });
    }


    @Override
    public Mono<User> findUserByMembershipId(Long membershipId){
        return userR2dbcRepository.findById(membershipId);
    }
    @Override
    public Flux<User> findAll(){return userR2dbcRepository.findAll();}
    @Override
    public Mono<User> findByAccount(String account){
        return userR2dbcRepository.findByAccount(account);
    }

}
