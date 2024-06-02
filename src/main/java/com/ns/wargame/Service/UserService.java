package com.ns.wargame.Service;

import com.ns.wargame.Domain.User;
import com.ns.wargame.Domain.dto.UserRequest;
import com.ns.wargame.Domain.dto.UserResponse;
import com.ns.wargame.Repository.UserR2dbcRepository;
import com.ns.wargame.Domain.dto.UserCreateRequest;
import com.ns.wargame.Utils.JwtTokenProvider;
import com.ns.wargame.Utils.jwtToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserR2dbcRepository userRepository;
    private final ReactiveRedisTemplate<String, User> reactiveRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    //private final VaultAdapter vaultAdapter;

    public Mono<User> create(UserCreateRequest request) {

        //String encryptedPassword = vaultAdapter.encrypt(request.getPassword());
        //String encryptedName = vaultAdapter.encrypt(request.getName());
        //String encryptedEmail = vaultAdapter.encrypt(request.getEmail());

        // name, email의 중복 여부를 확인
        Flux<User> existingUsers = Flux.concat(
                userRepository.findByName(request.getName()),
                userRepository.findByEmail(request.getEmail())
        );

        return existingUsers.collectList()
                .flatMap(existingUserList -> {
                    if (existingUserList.isEmpty()) {
                        return userRepository.save(User.builder()
                                .password(request.getPassword())
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

    public Mono<UserResponse> login(UserRequest request){
        return userRepository.findByEmailAndPassword(request.getEmail(), request.getPassword())
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
                } );
    }

    public Flux<User> findAll(){
        return userRepository.findAll().flatMap(user -> decryptUserData(user));
    }

    public Mono<User> findById(Long id){

        return reactiveRedisTemplate.opsForValue()
                .get("users:%d".formatted(id))
                .switchIfEmpty(userRepository.findById(id)
                        .flatMap(u->
                            reactiveRedisTemplate.opsForValue()
                                .set("users:%d".formatted(id),u, Duration.ofSeconds(30))
                                .then(Mono.just(u))
                        ).flatMap(this::decryptUserData));
        //return userRepository.findById(id);
    }

    public Mono<Void> deleteById(Long id){

        return userRepository.deleteById(id)
                .then(reactiveRedisTemplate.unlink("users:%d".formatted(id)))
                .then(Mono.empty());
    }
    public Mono<Void> deleteByName(String name) {
        return userRepository.findByName(name)
                .flatMap(user -> userRepository.deleteByName(name)
                            .thenReturn(user.getId()))
                .flatMap(userId -> reactiveRedisTemplate.unlink("users:%d".formatted(userId)))
                .then(Mono.empty());
    }

    public Mono<User> update(Long id, String name, String email,String password){
        // String encryptedName = vaultAdapter.encrypt(name);
        // String encryptedEmail = vaultAdapter.encrypt(email);
        // String encryptedPassword = vaultAdapter.encrypt(password);

        return userRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setEmail(email);
                    u.setPassword(password);
                    return userRepository.save(u);
                })
                .flatMap(u->reactiveRedisTemplate.unlink("users:%d".formatted(id))
                        .then(Mono.just(u)));
                // map으로 하면 Mono<Mono<User>>를 반환
    }

    private Mono<User> decryptUserData(User user) {
        //String decryptedName = vaultAdapter.decrypt(user.getName());
        //String decryptedEmail = vaultAdapter.decrypt(user.getEmail());
        //String decryptedPassword = vaultAdapter.decrypt(user.getPassword());

        User decryptedUser = new User();
        decryptedUser.setId(user.getId());
        decryptedUser.setEmail(user.getEmail());
        decryptedUser.setPassword(user.getPassword());
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
