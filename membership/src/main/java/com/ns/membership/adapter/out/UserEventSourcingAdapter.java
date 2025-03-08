package com.ns.membership.adapter.out;

import com.ns.common.CreatePlayerCommand;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.membership.adapter.axon.command.CreateMemberCommand;
import com.ns.membership.adapter.axon.command.ModifyMemberCommand;
import com.ns.membership.adapter.out.persistence.User;
import com.ns.membership.application.port.out.FindUserPort;
import com.ns.membership.application.port.out.ModifyUserPort;
import com.ns.membership.application.port.out.RegisterUserPort;
import com.ns.membership.application.port.out.UserEventSourcingPort;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@PersistanceAdapter
public class UserEventSourcingAdapter implements UserEventSourcingPort {
    private final CommandGateway commandGateway;
    private final RegisterUserPort registerUserPort;
    private final ModifyUserPort modifyUserPort;
    private final FindUserPort findUserPort;

    @Override
    public Mono<User> createMemberByEvent(UserCreateRequest request) {
        CreateMemberCommand axonCommand = new CreateMemberCommand(request.getAccount(), request.getName(), request.getEmail(), request.getPassword());

        return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                .flatMap(result -> registerUserPort.create(request, (String) result))
                .doOnSuccess(user -> commandGateway.send(new CreatePlayerCommand(String.valueOf(user.getId()))))
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
}
