package com.ns.wargame.Controller;

import com.ns.wargame.Domain.dto.*;
import com.ns.wargame.Service.PostService;
import com.ns.wargame.Service.UserService;
import com.ns.wargame.Utils.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final PostService postService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public Mono<ResponseEntity<messageEntity>> createUser(@RequestBody UserCreateRequest request){

        return userService.create(request)
                .map(user -> ResponseEntity.ok()
                        .body(new messageEntity("Success", UserResponse.of(user))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<messageEntity>> loginUser(@RequestBody UserRequest request){
        return userService.login(request)
                .map(userResponse -> ResponseEntity.ok()
                        .body(new messageEntity("Success", userResponse)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @GetMapping("/memberList")
    public Mono<ResponseEntity<messageEntity>> findAllUsers(){
        return userService.findAll()
                .collectList()
                .map(users -> users.stream()
                        .map(UserResponse::of)
                        .collect(Collectors.toList()))
                .map(userResponses -> ResponseEntity.ok()
                        .body(new messageEntity("Success", userResponses)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<messageEntity>> findUser(@PathVariable Long id, ServerWebExchange exchange) {
        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }
                    return userService.findById(id)
                            .map(user -> ResponseEntity.ok()
                                    .body(new messageEntity("Success", UserResponse.of(user))))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
                });
    }

    @DeleteMapping("/delete/{id}")
    public Mono<ResponseEntity<messageEntity>> deleteUser(ServerWebExchange exchange) {
        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if(idx==0)
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));

                        return Mono.just(ResponseEntity.ok()
                                        .body(new messageEntity("Success", userService.deleteById(idx))))
                                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "id is not correct.")));

                                        });
    }
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<messageEntity>> deleteUserByName(@RequestParam String name, ServerWebExchange exchange) {

        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0)
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));

                    return Mono.just(ResponseEntity.ok()
                                    .body(new messageEntity("Success", userService.deleteByName(name))))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
                });
    }
    @PutMapping("/update/{id}")
    public Mono<ResponseEntity<messageEntity>> updateUser(@RequestBody UserUpdateRequest request, ServerWebExchange exchange) {
        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {

                    if(idx==0)
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));

                    return userService.update(idx, request.getName(), request.getEmail(), request.getPassword())
                                .map(user -> ResponseEntity.ok()
                                        .body(new messageEntity("Success", UserResponse.of(user))))
                                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));


                    });
    }

    @GetMapping("/{id}/posts")
    public Mono<ResponseEntity<messageEntity>> getUserPosts(@PathVariable Long id, ServerWebExchange exchange) {

        return jwtTokenProvider.getMembershipIdByToken(exchange)
                .flatMap(idx -> {
                    if (idx == 0) {
                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
                    }
                    return postService.findAllByuserId(id)
                            .collectList()
                            .map(boards -> boards.stream()
                                    .map(PostResponse::of)
                                    .collect(Collectors.toList()))
                            .map(boardResponses -> ResponseEntity.ok()
                                    .body(new messageEntity("Success", boardResponses)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
                });
    }

    @PostMapping(path="/refresh-token")
    public Mono<ResponseEntity<messageEntity>> refreshToken(@RequestParam String refreshToken){
        if(refreshToken==null)
            return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail","RefreshToken is incorrect.")));

        return Mono.just(ResponseEntity.ok()
                .body(new messageEntity("Success",userService.refreshJwtToken(refreshToken))));
    }

    @PostMapping(path="/token-membership")
    ResponseEntity<messageEntity> getMembershipByJwtToken(@RequestParam String token){
        if(token==null)
            return ResponseEntity.ok().body(new messageEntity("Fail","Token is incorrect."));

        return ResponseEntity.ok().body(new messageEntity("Success",userService.getMembershipByJwtToken(token)));
    }
}
