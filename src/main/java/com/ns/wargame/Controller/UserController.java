package com.ns.wargame.Controller;

import com.ns.wargame.Domain.dto.*;
import com.ns.wargame.Service.PostService;
import com.ns.wargame.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final PostService postService;

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
                .map(user -> ResponseEntity.ok()
                        .body(new messageEntity("Success", UserResponse.of(user))))
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
    public Mono<ResponseEntity<messageEntity>> findUser(@PathVariable Long id){

        return userService.findById(id)
                .map(user -> ResponseEntity.ok()
                        .body(new messageEntity("Success", UserResponse.of(user))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @DeleteMapping("/delete/{id}")
    public Mono<ResponseEntity<messageEntity>> deleteUser(@PathVariable Long id){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",userService.deleteById(id))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","id is not correct.")));

    }
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<messageEntity>> deleteUserByName(@RequestParam String name){
        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",userService.deleteByName(name))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }
    @PutMapping("/update/{id}")
    public Mono<ResponseEntity<messageEntity>> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request){
        return userService.update(id, request.getName(), request.getEmail(),request.getPassword())
                .map(user -> ResponseEntity.ok()
                        .body(new messageEntity("Success", UserResponse.of(user))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @GetMapping("/{id}/posts")
    public Mono<ResponseEntity<messageEntity>> getUserPosts(@PathVariable Long id){
        return postService.findAllByuserId(id)
                .collectList()
                .map(boards -> boards.stream()
                        .map(PostResponse::of)
                        .collect(Collectors.toList()))
                .map(boardResponses -> ResponseEntity.ok()
                        .body(new messageEntity("Success", boardResponses)))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }
}
