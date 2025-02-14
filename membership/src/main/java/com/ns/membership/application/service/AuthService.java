package com.ns.membership.application.service;

import com.ns.common.utils.JwtToken;
import com.ns.common.utils.JwtTokenProvider;
import com.ns.membership.adapter.out.persistence.User;
import com.ns.membership.adapter.out.persistence.UserR2dbcRepository;
import com.ns.membership.dto.UserRequest;
import com.ns.membership.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserR2dbcRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public Mono<UserResponse> login(UserRequest request) {
        return userRepository.findByAccountAndPassword(request.getAccount(), request.getPassword())
                .flatMap(this::handleUserLogin)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials :"+request.getAccount()+" pw:"+request.getPassword())));
    }

    private Mono<UserResponse> handleUserLogin(User user){
        String id = user.getId().toString();
        Mono<String> jwtMono = jwtTokenProvider.generateJwtToken(id);
        Mono<String> refreshMono = jwtTokenProvider.generateRefreshToken(id);

        return Mono.zip(jwtMono, refreshMono)
                .flatMap(tuple -> {
                    String jwtToken = tuple.getT1();
                    String refreshToken = tuple.getT2();

                    return updateTokens(user, jwtToken, refreshToken);
                });
    }

    private Mono<UserResponse> updateTokens(User user, String jwtToken, String refreshToken){
        user.setRefreshToken(refreshToken);

        return userRepository.save(user)
                .map(savedUser -> UserResponse.of(savedUser))
                .flatMap(userResponse -> {
                    userResponse.setJwtToken(jwtToken);
                    return Mono.just(userResponse);
                });
    }

    public Mono<User> validateJwtToken(String token) {
        return jwtTokenProvider.validateJwtToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new RuntimeException("Invalid token"));
                    }
                    return jwtTokenProvider.parseMembershipIdFromToken(token)
                            .flatMap(userRepository::findById)
                            .switchIfEmpty(Mono.error(new RuntimeException("User not found")));
                });
    }

    public Mono<JwtToken> refreshJwtToken(String refreshToken) {
        return validateJwtToken(refreshToken)
                .flatMap(membership -> {
                    if (!membership.getRefreshToken().equals(refreshToken))
                        return Mono.empty();


                    String membershipId = String.valueOf(membership.getId());
                    return jwtTokenProvider.generateJwtToken(membershipId)
                            .map(newJwtToken -> new JwtToken(membershipId, newJwtToken, refreshToken));
                });
    }

    public Mono<User> getMembershipByJwtToken(String token) {
        return validateJwtToken(token)
                .flatMap(membership -> {
                    if (!membership.getRefreshToken().equals(token)) {
                        return Mono.empty();
                    }
                    return Mono.just(membership);
                });
    }
}
