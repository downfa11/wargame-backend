package com.ns.membership.adapter.in.web;

import com.ns.common.MessageEntity;
import com.ns.common.utils.JwtTokenProvider;
import com.ns.membership.application.port.in.FindUserUseCase;
import com.ns.membership.application.port.in.ModifyUserUseCase;
import com.ns.membership.application.port.in.RegisterUserUseCase;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserUpdateRequest;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final String INCORRECT_REQUEST_ERROR_MESSAGE = "Request is not correct.";
    private final String USER_DELETE_SUCCESS_MESSAGE =  "User deleted successfully.";
    private final String FAIL_TO_DELETE_USER_ERROR_MESSAGE = "Failed to delete the user.";
    private final String INCORRECT_MEMBERSHIP_ID_ERROR_MESSAGE = "ID is not correct.";


    private final RegisterUserUseCase registerUserUseCase;
    private final ModifyUserUseCase modifyUserUseCase;
    private final FindUserUseCase findUserUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping(path="/register-eda")
    Mono<ResponseEntity<MessageEntity>> createUser(@RequestBody UserCreateRequest request){
        return registerUserUseCase.create(request)
                .map(user -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", request)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @PostMapping("/register/temp")
    public Mono<ResponseEntity<MessageEntity>> createTempUser(){
        Random random = new Random();

        UserCreateRequest request = UserCreateRequest.builder()
                .name("user" + random.nextInt(10000))
                .account("account:" + random.nextInt(10000))
                .password("password:" + random.nextInt(10000))
                .email("email: " + random.nextInt(10000))
                .verify("random")
                .build();

        return registerUserUseCase.create(request)
                .map(user -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", request)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @PostMapping(path="/modify-eda")
    Mono<ResponseEntity<MessageEntity>> modifyUser(@RequestParam Long membershipId, @RequestBody UserUpdateRequest request){
        return modifyUserUseCase.modify(membershipId, request)
                .map(user -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", request)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @GetMapping("/memberList")
    public Mono<ResponseEntity<MessageEntity>> findAllUsers(){
        return findUserUseCase.findAll()
                .map(userResponses -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", userResponses)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<MessageEntity>> findUser(@PathVariable Long id, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return userService.findById(id)
//                            .map(user -> ResponseEntity.ok()
//                                    .body(new MessageEntity("Success", UserResponse.of(user))))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "request is not correct.")));
//                });
                    return findUserUseCase.findUserByMembershipId(id)
                            .map(user -> ResponseEntity.ok()
                                    .body(new MessageEntity("Success", user)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @GetMapping("/posts/{membershipId}")
    public Mono<ResponseEntity<MessageEntity>> getUserPosts(@PathVariable Long membershipId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorized or boardId is incorrect.")));
//                    }
//                    return userService.getUserPosts(postMemberId)
//                            .map(posts -> ResponseEntity.ok(new MessageEntity("Success", posts)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "Request is not correct.")));
//                });

                    return findUserUseCase.getUserPosts(membershipId)
                            .map(posts -> ResponseEntity.ok(new MessageEntity("Success", posts)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }
}
