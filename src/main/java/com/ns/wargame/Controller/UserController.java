package com.ns.wargame.Controller;

import com.ns.wargame.Domain.dto.*;
import com.ns.wargame.Service.PostService;
import com.ns.wargame.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final PostService postService;

    @PostMapping("/register")
    public Mono<ResponseEntity<messageEntity>> createUser(@RequestBody UserCreateRequest request){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",userService.create(request)
                                .map(UserResponse::of))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }

    @PostMapping("/login")
    public Mono<ResponseEntity<messageEntity>> loginUser(@RequestBody UserRequest request){
        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",userService.login(request)
                                .map(UserResponse::of))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }

    @GetMapping("/memberList")
    public Mono<ResponseEntity<messageEntity>> findAllUsers(){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",userService.findAll()
                                .map(UserResponse::of))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<messageEntity>> findUser(@PathVariable Long id){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",userService.findById(id)
                                .map(u -> ResponseEntity.ok(UserResponse.of(u))))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

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

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success", userService.update(id, request.getName(), request.getEmail(),request.getPassword())
                                .map(u -> ResponseEntity.ok(UserResponse.of(u))))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","request is not correct.")));

    }

    @GetMapping("/{id}/posts")
    public Mono<ResponseEntity<messageEntity>> getUserPosts(@PathVariable Long id){

        return Mono.just(ResponseEntity.ok()
                        .body(new messageEntity("Success",postService.findAllByuserId(id)
                                .map(UserPostResponse::of))))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail","id is not correct.")));

    }
}
