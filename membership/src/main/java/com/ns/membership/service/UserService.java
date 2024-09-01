package com.ns.membership.service;


import com.ns.membership.Utils.JwtTokenProvider;
import com.ns.membership.Utils.Vault.VaultAdapter;
import com.ns.membership.Utils.jwtToken;
import com.ns.membership.entity.User;
import com.ns.membership.entity.dto.PostResponse;
import com.ns.membership.entity.dto.UserCreateRequest;
import com.ns.membership.entity.dto.UserRequest;
import com.ns.membership.entity.dto.UserResponse;
import com.ns.membership.repository.UserR2dbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserR2dbcRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final VaultAdapter vaultAdapter;

    public Mono<User> create(UserCreateRequest request) {

        String encryptedPassword = vaultAdapter.encrypt(request.getPassword());

        // name, email의 중복 여부를 확인
        Flux<User> existingUsers = Flux.concat(
                userRepository.findByName(request.getName()),
                userRepository.findByEmail(request.getEmail())
        );

        return existingUsers.collectList()
                .flatMap(existingUserList -> {
                    if (existingUserList.isEmpty()) {
                        return userRepository.save(User.builder()
                                .password(encryptedPassword)
                                .name(request.getName())
                                .email(request.getEmail())
                                        .elo(2000L)
                                        .curGameSpaceCode("")
                                .build());
                    } else {
                        return Mono.error(new RuntimeException("Duplicated data."));
                    }
                });
    }
    public Mono<UserResponse> login(UserRequest request) {
        String encryptedPassword = vaultAdapter.encrypt(request.getPassword());
        log.info("encrypt password : " + encryptedPassword);

        return userRepository.findByEmailAndPassword(request.getEmail(), encryptedPassword)
                .flatMap(user -> {
                    String id = user.getId().toString();
                    Mono<String> jwtMono = jwtTokenProvider.generateJwtToken(id);
                    Mono<String> refreshMono = jwtTokenProvider.generateRefreshToken(id);

                    return Mono.zip(jwtMono, refreshMono)
                            .flatMap(tuple -> {
                                String jwt = tuple.getT1();
                                String refreshToken = tuple.getT2();

                                user.setRefreshToken(refreshToken);

                                return userRepository.save(user)
                                        .map(savedUser -> UserResponse.of(savedUser))
                                        .flatMap(userResponse -> {
                                            userResponse.setJwtToken(jwt);
                                            return Mono.just(userResponse);
                                        });
                            });
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials id:"+request.getEmail()+" pw:"+request.getPassword())));
    }


    public Flux<User> findAll(){
        return userRepository.findAll().flatMap(user -> decryptUserData(user));
    }

    public Mono<User> findById(Long id){
        return userRepository.findById(id).flatMap(this::decryptUserData);
    }

    public Mono<Void> deleteById(Long id){

        return userRepository.deleteById(id)
                .then(Mono.empty());
    }
    public Mono<Void> deleteByName(String name) {
        return userRepository.findByName(name)
                .flatMap(user -> userRepository.deleteByName(name)
                            .thenReturn(user.getId()))
                .then(Mono.empty());
    }

    public Mono<User> update(Long id, String name, String email,String password){
        String encryptedPassword = vaultAdapter.encrypt(password);

        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setEmail(email);
                    u.setPassword(encryptedPassword);
                    return userRepository.save(u);
                });
                // map으로 하면 Mono<Mono<User>>를 반환
    }

    public Flux<PostResponse> getUserPosts(Long membershipId){

        /*
        * 일단 먼저 kafka에 PostResponse 데이터를 요청한다.
        * 기다리면서 listen한다.
        * */

        return Flux.empty();
    }

    private Mono<User> decryptUserData(User user) {
        String decryptedPassword = vaultAdapter.decrypt(user.getPassword());

        User decryptedUser = new User();
        decryptedUser.setId(user.getId());
        decryptedUser.setEmail(user.getEmail());
        decryptedUser.setPassword(decryptedPassword);
        decryptedUser.setName(user.getName());
        decryptedUser.setElo(user.getElo());
        decryptedUser.setCurGameSpaceCode(user.getCurGameSpaceCode());
        return Mono.just(decryptedUser);
    }

    public Mono<jwtToken> refreshJwtToken(String refreshToken) {

        return jwtTokenProvider.validateJwtToken(refreshToken)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.empty();
                    }
                    return jwtTokenProvider.parseMembershipIdFromToken(refreshToken)
                            .flatMap(membershipId -> userRepository.findById(membershipId)
                                    .flatMap(membership -> {
                                        if (!membership.getRefreshToken().equals(refreshToken)) {
                                            return Mono.empty();
                                        }
                                        return jwtTokenProvider.generateJwtToken(String.valueOf(membershipId))
                                                .map(newJwtToken -> new jwtToken(
                                                        membershipId.toString(),
                                                        newJwtToken,
                                                        refreshToken
                                                ));
                                    })
                            );
                });
    }

    public Mono<Boolean> validateJwtToken(String token) {
        return jwtTokenProvider.validateJwtToken(token);
    }

    public Mono<User> getMembershipByJwtToken(String token) {

        return validateJwtToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.empty();
                    }
                    Long membershipId = jwtTokenProvider.parseMembershipIdFromToken(token).block();

                    return userRepository.findById(membershipId)
                            .flatMap(membership -> {
                                if (!membership.getRefreshToken().equals(token)) {
                                    return Mono.empty();
                                }
                                return Mono.just(membership);
                            });
                });
    }
}
