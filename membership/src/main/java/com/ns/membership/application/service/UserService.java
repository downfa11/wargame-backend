package com.ns.membership.application.service;


import com.ns.common.anotation.UseCase;
import com.ns.membership.application.port.in.FindUserUseCase;
import com.ns.membership.application.port.in.ModifyUserUseCase;
import com.ns.membership.application.port.in.RegisterUserUseCase;
import com.ns.membership.application.port.out.FindUserPort;
import com.ns.membership.application.port.out.TaskProducerPort;
import com.ns.membership.application.port.out.UserEventSourcingPort;
import com.ns.membership.dto.PostSummary;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserResponse;
import com.ns.membership.dto.UserUpdateRequest;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class UserService implements RegisterUserUseCase, ModifyUserUseCase, FindUserUseCase {
    private final UserEventSourcingPort userEventSourcingPort;
    private final FindUserPort findUserPort;
    private final TaskProducerPort taskProducerPort;

    @Override
    public Mono<UserResponse> create(UserCreateRequest request) {
        return userEventSourcingPort.createMemberByEvent(request)
                .map(UserResponse::of);
    }

    @Override
    public Mono<UserResponse> modify(Long membershipId, UserUpdateRequest request) {
        return userEventSourcingPort.modifyMemberByEvent(membershipId, request)
                .map(UserResponse::of);
    }

    @Override
    public Mono<List<PostSummary>> getUserPosts(Long membershipId) {
        return taskProducerPort.getUserPosts(membershipId);
    }

    @Override
    public Mono<UserResponse> findUserByMembershipId(Long membershipId) {
        return findUserPort.findUserByMembershipId(membershipId)
                .map(UserResponse::of);
    }

    @Override
    public Mono<List<UserResponse>> findAll() {
        return findUserPort.findAll()
                .collectList()
                .map(users -> users.stream()
                        .map(UserResponse::of)
                        .collect(Collectors.toList()));
    }
}
