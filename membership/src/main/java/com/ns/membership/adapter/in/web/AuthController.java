package com.ns.membership.adapter.in.web;

import com.ns.common.utils.MessageEntity;
import com.ns.membership.application.service.AuthService;
import com.ns.membership.dto.UserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final String INCORRECT_REQUEST_ERROR_MESSAGE = "Request is not correct.";
    private final String INCORRECT_REFRESH_TOKEN_ERROR_MESSAGE = "RefreshToken is incorrect.";
    private final String INCORRECT_ACCESS_TOKEN_ERROR_MESSAGE = "AccessToken is incorrect.";

    private final AuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<MessageEntity>> loginUser(@RequestBody UserRequest request){
        return authService.login(request)
                .map(userResponse -> ResponseEntity.ok()
                        .body(new MessageEntity("Success", userResponse)))
                .defaultIfEmpty(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REQUEST_ERROR_MESSAGE)));
    }


    @PostMapping(path="/refresh-token")
    public Mono<ResponseEntity<MessageEntity>> refreshToken(@RequestParam String refreshToken){
        if(refreshToken==null)
            return Mono.just(ResponseEntity.ok().body(new MessageEntity("Fail", INCORRECT_REFRESH_TOKEN_ERROR_MESSAGE)));

        return Mono.just(ResponseEntity.ok()
                .body(new MessageEntity("Success",authService.refreshJwtToken(refreshToken))));
    }

    @PostMapping(path="/token-membership")
    ResponseEntity<MessageEntity> getMembershipByJwtToken(@RequestParam String token){
        if(token==null)
            return ResponseEntity.ok().body(new MessageEntity("Fail",INCORRECT_ACCESS_TOKEN_ERROR_MESSAGE));

        return ResponseEntity.ok().body(new MessageEntity("Success",authService.getMembershipByJwtToken(token)));
    }
}
