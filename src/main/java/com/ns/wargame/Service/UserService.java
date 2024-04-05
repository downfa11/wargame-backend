package com.ns.wargame.Service;

import com.ns.wargame.Domain.User;
import com.ns.wargame.Domain.dto.UserRequest;
import com.ns.wargame.Repository.UserR2dbcRepository;
import com.ns.wargame.Domain.dto.UserCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserR2dbcRepository userRepository;
    private final ReactiveRedisTemplate<String, User> reactiveRedisTemplate;
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

    public Mono<User> login(UserRequest request){
        return userRepository.findByEmailAndPassword(request.getEmail(),request.getPassword());
    }

    public Flux<User> findAll(){
        return userRepository.findAll().flatMap(user -> decryptUserData(user));
    }

    public Mono<User> findById(Long id){

        return reactiveRedisTemplate.opsForValue()
                .get("users:%d".formatted(id))
                .switchIfEmpty(userRepository.findById(id)
                        .flatMap(u-> {
                           return reactiveRedisTemplate.opsForValue()
                                .set("users:%d".formatted(id),u, Duration.ofSeconds(30))
                                .then(Mono.just(u));
                            }
                        ).flatMap(this::decryptUserData));
        //return userRepository.findById(id);
    }

    public Mono<Void> deleteById(Long id){

        return userRepository.deleteById(id)
                .then(reactiveRedisTemplate.unlink("users:%d".formatted(id)))
                .then(Mono.empty());
    }
    public Mono<Void> deleteByName(String name) {
        return userRepository.deleteByName(name);}
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
}
