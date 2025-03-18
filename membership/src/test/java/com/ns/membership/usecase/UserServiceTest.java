package com.ns.membership.usecase;

import com.ns.membership.adapter.out.persistence.User;
import com.ns.membership.application.port.out.FindUserPort;
import com.ns.membership.application.port.out.TaskProducerPort;
import com.ns.membership.application.port.out.UserEventSourcingPort;
import com.ns.membership.application.service.UserService;
import com.ns.membership.dto.PostSummary;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserResponse;
import com.ns.membership.dto.UserUpdateRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserEventSourcingPort userEventSourcingPort;
    @Mock private FindUserPort findUserPort;
    @Mock private TaskProducerPort taskProducerPort;
    @InjectMocks private UserService userService;

    private UserResponse userResponse;
    private User user;
    private UserCreateRequest userCreateRequest;
    private UserUpdateRequest userUpdateRequest;
    private Long membershipId;

    @BeforeEach
    void init() {
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

        userResponse = UserResponse.of(user);
    }

    @Test
    void 사용자를_생성하는_메서드() {
        // given
        when(userEventSourcingPort.createMemberByEvent(any(UserCreateRequest.class)))
                .thenReturn(Mono.just(user));

        // when
        Mono<UserResponse> result = userService.create(userCreateRequest);

        // then
        StepVerifier.create(result)
                .expectNext(userResponse)
                .verifyComplete();

        verify(userEventSourcingPort).createMemberByEvent(userCreateRequest);
    }

    @Test
    void 사용자를_수정하는_메서드() {
        // given
        when(userEventSourcingPort.modifyMemberByEvent(eq(membershipId), any(UserUpdateRequest.class)))
                .thenReturn(Mono.just(user));

        // when
        Mono<UserResponse> result = userService.modify(membershipId, userUpdateRequest);

        // then
        StepVerifier.create(result)
                .expectNext(userResponse)
                .verifyComplete();

        verify(userEventSourcingPort).modifyMemberByEvent(eq(membershipId), any(UserUpdateRequest.class));
    }

    @Test
    void 사용자가_작성한_게시글_목록을_조회하는_메서드() {
        // given
        List<PostSummary> postSummaries = Collections.singletonList(PostSummary.builder()
                .id(1L)
                .sortStatus(PostSummary.SortStatus.EVENT)
                .nickname("nickname")
                .title("title")
                .likes(100L)
                .comments(10L)
                .views(500L)
                .createdAt(LocalDateTime.now())
                .build());

        when(taskProducerPort.getUserPosts(membershipId)).thenReturn(Mono.just(postSummaries));

        // when
        Mono<List<PostSummary>> result = userService.getUserPosts(membershipId);

        // then
        StepVerifier.create(result)
                .expectNext(postSummaries)
                .verifyComplete();

        verify(taskProducerPort).getUserPosts(membershipId);
    }

    @Test
    void membershipId로_사용자를_조회하는_메서드() {
        // given
        when(findUserPort.findUserByMembershipId(membershipId)).thenReturn(Mono.just(user));

        // when
        Mono<UserResponse> result = userService.findUserByMembershipId(membershipId);

        // then
        StepVerifier.create(result)
                .expectNext(userResponse)
                .verifyComplete();

        verify(findUserPort).findUserByMembershipId(membershipId);
    }

    @Test
    void 모든_사용자의_목록을_조회하는_메서드() {
        // given
        List<UserResponse> userResponses = List.of(userResponse);
        when(findUserPort.findAll()).thenReturn(Flux.just(user));

        // when
        Mono<List<UserResponse>> result = userService.findAll();

        // then
        StepVerifier.create(result)
                .expectNext(userResponses)
                .verifyComplete();

        verify(findUserPort).findAll();
    }
}
