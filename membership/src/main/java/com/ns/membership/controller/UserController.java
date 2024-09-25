package com.ns.membership.controller;

import com.ns.common.messageEntity;
import com.ns.membership.Utils.JwtTokenProvider;
import com.ns.membership.entity.dto.*;
import com.ns.membership.service.MailService;
import com.ns.membership.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final MailService mailService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public Mono<ResponseEntity<messageEntity>> createUser(@RequestBody UserCreateRequest request, ServerWebExchange exchange) {

        return exchange.getSession()
                .flatMap(webSession -> {
                    String sessionCode = (String) webSession.getAttributes().get("registerCode");

                    if (sessionCode == null || !sessionCode.equals(request.getVerify())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new messageEntity("Fail", "인증 코드가 유효하지 않거나 일치하지 않습니다.")));
                    }

                    return userService.create(request)
                            .map(user -> ResponseEntity.ok()
                                    .body(new messageEntity("Success", UserResponse.of(user))))
                            .defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(new messageEntity("Fail", "Request is not correct.")));
                });
    }

    @PostMapping("/register/temp")
    public Mono<ResponseEntity<messageEntity>> createTempUser(){

        Random random = new Random();

        UserCreateRequest request = UserCreateRequest.builder()
                .name("user" + random.nextInt(10000))
                .account("account:" + random.nextInt(10000))
                .password("password:" + random.nextInt(10000))
                .email("email: " + random.nextInt(10000))
                .verify("random")
                .build();


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
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//                    }
//                    return userService.findById(id)
//                            .map(user -> ResponseEntity.ok()
//                                    .body(new messageEntity("Success", UserResponse.of(user))))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
//                });
                    return userService.findById(id)
                            .map(user -> ResponseEntity.ok()
                                    .body(new messageEntity("Success", UserResponse.of(user))))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @DeleteMapping("/delete/{membershipId}")
    public Mono<ResponseEntity<messageEntity>> deleteUser(@PathVariable Long membershipId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if(membershipId==0)
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                        return Mono.just(ResponseEntity.ok()
//                                        .body(new messageEntity("Success", userService.deleteById(membershipId))))
//                                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "id is not correct.")));
//
//                                        });
        return userService.deleteById(membershipId)
                .then(Mono.just(ResponseEntity.ok().body(new messageEntity("Success", "User deleted successfully."))))
                .onErrorReturn(ResponseEntity.ok().body(new messageEntity("Fail", "Failed to delete the user.")))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "ID is not correct.")));
    }
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<messageEntity>> deleteUserByName(@RequestParam String name, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return Mono.just(ResponseEntity.ok()
//                                    .body(new messageEntity("Success", userService.deleteByName(name))))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
//                });
        return userService.deleteByName(name)
                .then(Mono.just(ResponseEntity.ok().body(new messageEntity("Success", "User deleted successfully."))))
                .onErrorReturn(ResponseEntity.ok().body(new messageEntity("Fail", "Failed to delete the user.")))
                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "ID is not correct.")));

    }
    @PutMapping("/update/{membershipId}")
    public Mono<ResponseEntity<messageEntity>> updateUser(@PathVariable Long membershipId, @RequestBody UserUpdateRequest request, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//
//                    if(membershipId==0)
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return userService.update(membershipId,request.getAccount(), request.getName(), request.getEmail(), request.getPassword())
//                                .map(user -> ResponseEntity.ok()
//                                        .body(new messageEntity("Success", UserResponse.of(user))))
//                                .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
//
//
//                    });
                    return userService.update(membershipId,request.getAccount(), request.getName(), request.getEmail(), request.getPassword())
                            .map(user -> ResponseEntity.ok()
                                    .body(new messageEntity("Success", UserResponse.of(user))))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "request is not correct.")));
    }

    @GetMapping("/posts/{membershipId}")
    public Mono<ResponseEntity<messageEntity>> getUserPosts(@PathVariable Long membershipId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0) {
//                        return Mono.just(ResponseEntity.ok().body(new messageEntity("Fail", "Not Authorized or boardId is incorrect.")));
//                    }
//                    return userService.getUserPosts(postMemberId)
//                            .map(posts -> ResponseEntity.ok(new messageEntity("Success", posts)))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Request is not correct.")));
//                });

                    return userService.getUserPosts(membershipId)
                            .map(posts -> ResponseEntity.ok(new messageEntity("Success", posts)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new messageEntity("Fail", "Request is not correct.")));
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

    @PostMapping("/membership/register/email-send")
    public Mono<ResponseEntity<String>> sendRegistrationVerificationEmail(@RequestParam String email, ServerWebExchange exchange) {

        return Mono.fromCallable(() -> {
            try {
                String code = mailService.sendRegisterMessage(email);
                exchange.getSession().map(webSession -> {
                    webSession.getAttributes().put("registerCode", code);
                    return ResponseEntity.ok("회원가입을 위한 이메일 인증 메일이 전송되었습니다.");
                }).subscribe();
                return ResponseEntity.ok("회원가입을 위한 이메일 인증 메일이 전송되었습니다.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("이메일 전송 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }

    @PostMapping("/membership/password-reset/email-send")
    public Mono<ResponseEntity<String>> sendPasswordResetVerificationEmail(ServerWebExchange exchange) {
        Long membershipId = 1L; // todo. jwtTokenProvider.getMembershipIdByToken(exchange);

        return userService.findById(membershipId)
                .flatMap(membership -> {
                    if (membership == null) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("해당하는 membershipId를 찾을 수 없습니다."));
                    }

                    String email = membership.getEmail();
                    if (email == null) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("사용자 인증 실패."));
                    }

                    return Mono.fromCallable(() -> {
                        try {
                            String code = mailService.sendPasswordResetMessage(email);
                            exchange.getSession().map(webSession -> {
                                webSession.getAttributes().put("passwordResetCode", code);
                                return ResponseEntity.ok("비밀번호 변경을 위한 이메일 인증 메일이 전송되었습니다.");
                            }).subscribe();
                            return ResponseEntity.ok("비밀번호 변경을 위한 이메일 인증 메일이 전송되었습니다.");
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("이메일 전송 중 오류가 발생했습니다: " + e.getMessage());
                        }
                    });
                });
    }

    @PostMapping("/membership/password-reset")
    public Mono<ResponseEntity<String>> resetPassword(@RequestBody PasswordResetRequest request, ServerWebExchange exchange) {

        return exchange.getSession()
                .flatMap(webSession -> {
                    String sessionCode = (String) webSession.getAttributes().get("passwordResetCode");
                    Long membershipId = 1L; // todo. jwtTokenProvider.getMembershipIdbyToken();

                    if (sessionCode == null || !sessionCode.equals(request.getVerify())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("인증 코드가 유효하지 않거나 일치하지 않습니다."));
                    }

                    if (!membershipId.equals(request.getMembershipId())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body("membershipId가 유효하지 않습니다."));
                    }

                    return userService.findById(membershipId)
                            .flatMap(membership -> {
                                if (membership == null) {
                                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                            .body("해당하는 membershipId를 찾을 수 없습니다."));
                                }

                                return Mono.fromCallable(() -> {
                                    userService.update(membership.getId(),membership.getAccount(),membership.getName(),membership.getEmail(),membership.getPassword());
                                    return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
                                });
                            });
                });
    }
}
