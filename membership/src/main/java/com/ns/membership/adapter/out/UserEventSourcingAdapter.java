package com.ns.membership.adapter.out;

import com.ns.common.CreatePlayerCommand;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.membership.adapter.axon.command.CreateMemberCommand;
import com.ns.membership.adapter.axon.command.DeleteMemberCommand;
import com.ns.membership.adapter.axon.command.ModifyMemberCommand;
import com.ns.membership.adapter.out.persistence.User;
import com.ns.membership.application.port.out.*;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@PersistanceAdapter
public class UserEventSourcingAdapter implements UserEventSourcingPort {
    private final CommandGateway commandGateway;
    private final RegisterUserPort registerUserPort;
    private final ModifyUserPort modifyUserPort;
    private final DeleteUserPort deleteUserPort;
    private final FindUserPort findUserPort;

    @Override
    public Mono<User> createMemberByEvent(UserCreateRequest request) {
        String aggregateId = UUID.randomUUID().toString();
        CreateMemberCommand axonCommand = new CreateMemberCommand(aggregateId, request.getAccount(), request.getName(), request.getEmail(), request.getPassword());

        return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                .flatMap(result -> registerUserPort.create(request, (String) result))
                .doOnSuccess(user -> commandGateway.send(new CreatePlayerCommand(String.valueOf(user.getId()), user.getName())))
                .doOnError(throwable -> log.error("createMemberByEvent throwable : ", throwable));
    }

    @Override
    public Mono<User> modifyMemberByEvent(Long membershipId, UserUpdateRequest request) {
        String account = request.getAccount();
        String name = request.getName();
        String email = request.getEmail();
        String password = request.getPassword();

        return findUserPort.findUserByMembershipId(membershipId)
                .flatMap(user -> {
                    String memberAggregateIdentifier = user.getAggregateIdentifier();
                    ModifyMemberCommand axonCommand = new ModifyMemberCommand(memberAggregateIdentifier, membershipId, account, name, email, password);

                    return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                            .flatMap(result -> modifyUserPort.update(membershipId, account,name , email, password))
                            .doOnError(throwable -> log.error("modifyMemberByEvent throwable : ", throwable));
                });
    }

    @Override
    public Mono<Void> deleteMemberByEvent(Long membershipId) {
        return findUserPort.findUserByMembershipId(membershipId)
                .flatMap(user -> {
                    String memberAggregateIdentifier = user.getAggregateIdentifier();
                    DeleteMemberCommand axonCommand = new DeleteMemberCommand(memberAggregateIdentifier, membershipId);
                    return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                            .flatMap(result -> deleteUserPort.delete(membershipId))
                            .doOnError(throwable -> log.error("deleteUserPort throwable : ", throwable));
                });
    }
}
