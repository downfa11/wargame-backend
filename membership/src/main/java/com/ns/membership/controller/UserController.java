package com.ns.membership.controller;

import com.ns.common.MessageEntity;
import com.ns.common.Utils.JwtTokenProvider;
import com.ns.membership.entity.User;
import com.ns.membership.entity.dto.*;
import com.ns.membership.service.KafkaService;
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
    private final String INCORRECT_REQUEST_ERROR_MESSAGE = "Request is not correct.";
    private final String UNAUTHORIZATED_CODE_ERROR_MESSAGE = "인증 코드가 유효하지 않거나 일치하지 않습니다.";
    private final String INCORRECT_REFRESH_TOKEN_ERROR_MESSAGE = "RefreshToken is incorrect.";
    private final String INCORRECT_ACCESS_TOKEN_ERROR_MESSAGE = "AccessToken is incorrect.";
    private final String USER_DELETE_SUCCESS_MESSAGE =  "User deleted successfully.";
    private final String FAIL_TO_DELETE_USER_ERROR_MESSAGE = "Failed to delete the user.";
    private final String INCORRECT_MEMBERSHIP_ID_ERROR_MESSAGE = "ID is not correct.";
    private final String UNAUTHORIZATED_MEMBERSHIP_ID_ERROR_MESSAGE = "membershipId가 유효하지 않습니다.";
    private final String PASSWORD_CHANGED_SUCCESS_MESSAGE = "비밀번호가 성공적으로 변경되었습니다.";
    private final String EMAIL_SEND_PROCESS_ERROR_MESSAGE = "이메일 전송 중 오류가 발생했습니다: ";
    private final String NOT_FOUND_MEMBERSHIP_ID_ERROR_MESSAGE = "해당하는 membershipId를 찾을 수 없습니다.";
    private final String EMAIL_SUCCESS_PASSWORD_CHANGE_MESSAGE = "비밀번호 변경을 위한 이메일 인증 메일이 전송되었습니다.";
    private final String EMAIL_SUCCESS_REGISTER_MESSAGE = "회원가입을 위한 이메일 인증 메일이 전송되었습니다.";



    private final UserService userService;
    private final MailService mailService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public Mono<ResponseEntity<MessageEntity>> createUser(@RequestBody UserCreateRequest request, ServerWebExchange exchange) {
        return exchange.getSession()
                .flatMap(webSession -> {
                    String sessionCode = (String) webSession.getAttributes().get("registerCode");

                    if (sessionCode == null || !sessionCode.equals(request.getVerify())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new MessageEntity("Fail", UNAUTHORIZATED_CODE_ERROR_MESSAGE)));
                    }

                    return userService.create(request)
                            .map(user -> ResponseEntity.ok()
                                    .body(new MessageEntity("Success", UserResponse.of(user))))
                            .defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
                });
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


        return userService.create(request)
                .map(user -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", UserResponse.of(user))))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<MessageEntity>> loginUser(@RequestBody UserRequest request){
        return userService.login(request)
                .map(userResponse -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", userResponse)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @GetMapping("/memberList")
    public Mono<ResponseEntity<MessageEntity>> findAllUsers(){
        return userService.findAll()
                .collectList()
                .map(users -> users.stream()
                        .map(UserResponse::of)
                        .collect(Collectors.toList()))
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
                    return userService.findById(id)
                            .map(user -> ResponseEntity.ok()
                                    .body(new MessageEntity("Success", UserResponse.of(user))))
                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @DeleteMapping("/delete/{membershipId}")
    public Mono<ResponseEntity<MessageEntity>> deleteUser(@PathVariable Long membershipId, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if(membershipId==0)
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                        return Mono.just(ResponseEntity.ok()
//                                        .body(new MessageEntity("Success", userService.deleteById(membershipId))))
//                                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "id is not correct.")));
//
//                                        });
        return userService.deleteById(membershipId)
                .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", USER_DELETE_SUCCESS_MESSAGE))))
                .onErrorReturn(ResponseEntity.ok().body(new MessageEntity("Fail", FAIL_TO_DELETE_USER_ERROR_MESSAGE)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_MEMBERSHIP_ID_ERROR_MESSAGE)));
    }

    @DeleteMapping("/delete")
    public Mono<ResponseEntity<MessageEntity>> deleteUserByName(@RequestParam String name, ServerWebExchange exchange) {

//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//                    if (membershipId == 0)
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return Mono.just(ResponseEntity.ok()
//                                    .body(new MessageEntity("Success", userService.deleteByName(name))))
//                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "request is not correct.")));
//                });
        return userService.deleteByName(name)
                .then(Mono.just(ResponseEntity.ok().body(new MessageEntity("Success", USER_DELETE_SUCCESS_MESSAGE))))
                .onErrorReturn(ResponseEntity.ok().body(new MessageEntity("Fail", FAIL_TO_DELETE_USER_ERROR_MESSAGE)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_MEMBERSHIP_ID_ERROR_MESSAGE)));

    }

    @PutMapping("/update/{membershipId}")
    public Mono<ResponseEntity<MessageEntity>> updateUser(@PathVariable Long membershipId, @RequestBody UserUpdateRequest request, ServerWebExchange exchange) {
//        return jwtTokenProvider.getMembershipIdByToken(exchange)
//                .flatMap(membershipId -> {
//
//                    if(membershipId==0)
//                        return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", "Not Authorization or boardId is incorrect.")));
//
//                    return userService.update(membershipId,request.getAccount(), request.getName(), request.getEmail(), request.getPassword())
//                                .map(user -> ResponseEntity.ok()
//                                        .body(new MessageEntity("Success", UserResponse.of(user))))
//                                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", "request is not correct.")));
//
//
//                    });
                    return userService.update(membershipId,request.getAccount(), request.getName(), request.getEmail(), request.getPassword())
                            .map(user -> ResponseEntity.ok()
                                    .body(new MessageEntity("Success", UserResponse.of(user))))
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

                    return userService.getUserPosts(membershipId)
                            .map(posts -> ResponseEntity.ok(new MessageEntity("Success", posts)))
                            .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }

    @PostMapping(path="/refresh-token")
    public Mono<ResponseEntity<MessageEntity>> refreshToken(@RequestParam String refreshToken){
        if(refreshToken==null)
            return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REFRESH_TOKEN_ERROR_MESSAGE)));

        return Mono.just(ResponseEntity.ok()
                .body(new MessageEntity("Success",userService.refreshJwtToken(refreshToken))));
    }

    @PostMapping(path="/token-membership")
    ResponseEntity<MessageEntity> getMembershipByJwtToken(@RequestParam String token){
        if(token==null)
            return ResponseEntity.ok().body(new MessageEntity("Fail",INCORRECT_ACCESS_TOKEN_ERROR_MESSAGE));

        return ResponseEntity.ok().body(new MessageEntity("Success",userService.getMembershipByJwtToken(token)));
    }

    @PostMapping("/membership/register/email-send")
    public Mono<ResponseEntity<String>> sendRegistrationVerificationEmail(@RequestParam String email, ServerWebExchange exchange) {

        return Mono.fromCallable(() -> {
            try {
                String code = mailService.sendRegisterMessage(email);
                exchange.getSession().map(webSession -> {
                    webSession.getAttributes().put("registerCode", code);
                    return ResponseEntity.ok(EMAIL_SUCCESS_REGISTER_MESSAGE);
                }).subscribe();
                return ResponseEntity.ok(EMAIL_SUCCESS_REGISTER_MESSAGE);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(EMAIL_SEND_PROCESS_ERROR_MESSAGE + e.getMessage());
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
                                .body(NOT_FOUND_MEMBERSHIP_ID_ERROR_MESSAGE));
                    }

                    String email = membership.getEmail();
                    if (email == null) {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(UNAUTHORIZATED_MEMBERSHIP_ID_ERROR_MESSAGE));
                    }

                    return Mono.fromCallable(() -> {
                        try {
                            String code = mailService.sendPasswordResetMessage(email);
                            exchange.getSession().map(webSession -> {
                                webSession.getAttributes().put("passwordResetCode", code);
                                return ResponseEntity.ok(EMAIL_SUCCESS_PASSWORD_CHANGE_MESSAGE);
                            }).subscribe();
                            return ResponseEntity.ok(EMAIL_SUCCESS_PASSWORD_CHANGE_MESSAGE);
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(EMAIL_SEND_PROCESS_ERROR_MESSAGE + e.getMessage());
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
                                .body(UNAUTHORIZATED_CODE_ERROR_MESSAGE));
                    }

                    return userService.findByAccount(request.getAccount())
                            .flatMap(user -> {
                                if(user == null){
                                    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                            .body(NOT_FOUND_MEMBERSHIP_ID_ERROR_MESSAGE));
                                }

                                if (!membershipId.equals(user.getId())) {
                                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                            .body(UNAUTHORIZATED_MEMBERSHIP_ID_ERROR_MESSAGE));
                                }

                                return Mono.fromCallable(() -> {
                                    userService.update(user.getId(),user.getAccount(),user.getName(),user.getEmail(),user.getPassword());
                                    return ResponseEntity.ok(PASSWORD_CHANGED_SUCCESS_MESSAGE);
                                });
                            });
                });
    }

    @PostMapping(path="/create-member")
    Mono<Void> createMemberMoney(@RequestBody UserCreateRequest request){
        return userService.createMemberByEvent(request);
    }

    @PostMapping(path="/increase-elo-test-eda")
    Mono<Void> ModifyMemberEloByEvent(@RequestParam String membershipId){
        Random random = new Random();
        Long elo = random.nextLong(2000);
        return userService.modifyMemberEloByEvent(membershipId, elo);
        // 임시로 createMemberMoneyUserCase에서 balance의 증감도 같이 하는중
    }

    @PostMapping(path="/increase-elo-test")
    Mono<User> ModifyMemberElo(@RequestParam String membershipId){
        Random random = new Random();
        Long elo = random.nextLong(2000);
        return userService.modifyMemberElo(membershipId, elo);
    }

    @PostMapping(path="/modify-eda")
    Mono<Void> ModifyMemberByEvent(@RequestParam String membershipId, @RequestBody UserUpdateRequest request){
        return userService.modifyMemberByEvent(membershipId, request);
    }

}
