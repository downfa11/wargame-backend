package com.ns.membership.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ns.membership.adapter.out.persistence.User;
import com.ns.membership.adapter.out.persistence.UserPersistenceAdapter;
import com.ns.membership.adapter.out.persistence.UserR2dbcRepository;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserUpdateRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock private UserR2dbcRepository userR2dbcRepository;

    private UserPersistenceAdapter userPersistenceAdapter;
    private User user;
    private UserCreateRequest userCreateRequest;
    private UserUpdateRequest userUpdateRequest;
    private Long membershipId;

    @BeforeEach
    void init() {
        userPersistenceAdapter = new UserPersistenceAdapter(userR2dbcRepository);

        membershipId = 1L;

        userCreateRequest = UserCreateRequest.builder()
                .account("username")
                .password("password")
                .name("name")
                .email("email@example.com")
                .verify("verifyCode")
                .build();

        userUpdateRequest = UserUpdateRequest.builder()
                .account("newUsername")
                .name("newName")
                .email("email@example.com")
                .password("newPassword")
                .build();

        user = User.builder()
                .id(1L)
                .account("username")
                .password("password")
                .name("name")
                .email("email@example.com")
                .refreshToken("refreshToken")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 사용자를_생성하는_메서드() {
        // given
        String aggregateIdentifier = "aggId";

        when(userR2dbcRepository.findByName(userCreateRequest.getName())).thenReturn(Flux.empty());
        when(userR2dbcRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(Flux.empty());
        when(userR2dbcRepository.save(any())).thenReturn(Mono.just(user));

        // when
        Mono<User> result = userPersistenceAdapter.create(userCreateRequest, aggregateIdentifier);

        // then
        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(userR2dbcRepository, times(1)).findByName(userCreateRequest.getName());
        verify(userR2dbcRepository, times(1)).findByEmail(userCreateRequest.getEmail());
        verify(userR2dbcRepository, times(1)).save(any());
    }

    @Test
    void 사용자를_생성하는_메서드_중복된_이름이나_계정인_경우() {
        // given
        when(userR2dbcRepository.findByName(userCreateRequest.getName())).thenReturn(Flux.just(user));
        when(userR2dbcRepository.findByEmail(userCreateRequest.getEmail())).thenReturn(Flux.empty());

        // when
        Mono<User> result = userPersistenceAdapter.create(userCreateRequest, user.getAggregateIdentifier());

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(userR2dbcRepository, times(1)).findByName(userCreateRequest.getName());
        verify(userR2dbcRepository, times(1)).findByEmail(userCreateRequest.getEmail());
        verify(userR2dbcRepository, never()).save(any());
    }

    @Test
    void 사용자를_수정하는_메서드() {
        // given
        Long id = 1L;
        String account = "newAccount";
        String name = "newName";
        String email = "newEmail";
        String password = "newPassword";

        when(userR2dbcRepository.findById(id)).thenReturn(Mono.just(user));
        when(userR2dbcRepository.save(any())).thenReturn(Mono.just(user));

        // when
        Mono<User> result = userPersistenceAdapter.update(id, account, name, email, password);

        // then
        StepVerifier.create(result)
                .expectNextMatches(user -> user.getAccount().equals(account) && user.getName().equals(name))
                .verifyComplete();

        verify(userR2dbcRepository, times(1)).findById(id);
        verify(userR2dbcRepository, times(1)).save(any());
    }

    @Test
    void 사용자를_수정하는_메서드_사용자가_없는_경우() {
        // given
        Long id = 1L;
        String account = "newAccount";
        String name = "newName";
        String email = "newEmail";
        String password = "newPassword";

        when(userR2dbcRepository.findById(id)).thenReturn(Mono.empty());

        // when
        Mono<User> result = userPersistenceAdapter.update(id, account, name, email, password);

        // then
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(userR2dbcRepository, times(1)).findById(id);
        verify(userR2dbcRepository, never()).save(any());
    }

    @Test
    void 비밀번호를_변경하는_메서드() {
        // given
        Long id = 1L;
        String newPassword = "newPassword";

        when(userR2dbcRepository.findById(id)).thenReturn(Mono.just(user));
        when(userR2dbcRepository.save(any())).thenReturn(Mono.just(user));

        // when
        Mono<User> result = userPersistenceAdapter.resetPassword(id, newPassword);

        // then
        StepVerifier.create(result)
                .expectNextMatches(user -> user.getPassword().equals(newPassword))
                .verifyComplete();

        verify(userR2dbcRepository, times(1)).findById(id);
        verify(userR2dbcRepository, times(1)).save(any());
    }

    @Test
    void 비밀번호를_변경하는_메서드_사용자가_없는_경우() {
        // given
        Long id = 1L;
        String newPassword = "newPassword";

        when(userR2dbcRepository.findById(id)).thenReturn(Mono.empty());

        // when
        Mono<User> result = userPersistenceAdapter.resetPassword(id, newPassword);

        // then
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(userR2dbcRepository, times(1)).findById(id);
        verify(userR2dbcRepository, never()).save(any());
    }

    @Test
    void membershipId로_사용자를_조회하는_메서드() {
        // given
        Long membershipId = 1L;
        when(userR2dbcRepository.findById(membershipId)).thenReturn(Mono.just(user));

        // when
        Mono<User> result = userPersistenceAdapter.findUserByMembershipId(membershipId);

        // then
        StepVerifier.create(result)
                .expectNextMatches(user -> user.getId().equals(membershipId))
                .verifyComplete();

        verify(userR2dbcRepository, times(1)).findById(membershipId);
    }

    @Test
    void findUserByMembershipId_회원_없음() {
        // given
        Long membershipId = 1L;
        when(userR2dbcRepository.findById(membershipId)).thenReturn(Mono.empty());

        // when
        Mono<User> result = userPersistenceAdapter.findUserByMembershipId(membershipId);

        // then
        StepVerifier.create(result)
                .expectComplete()
                .verify();

        verify(userR2dbcRepository, times(1)).findById(membershipId);
    }
}
