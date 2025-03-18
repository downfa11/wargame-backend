package com.ns.membership.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ns.common.MessageEntity;
import com.ns.membership.adapter.in.web.UserController;
import com.ns.membership.application.port.in.FindUserUseCase;
import com.ns.membership.application.port.in.ModifyUserUseCase;
import com.ns.membership.application.port.in.RegisterUserUseCase;
import com.ns.membership.dto.UserCreateRequest;
import com.ns.membership.dto.UserResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(UserController.class)
public class UserControllerTest {

    @Autowired private WebTestClient webTestClient;

    @MockBean private RegisterUserUseCase registerUserUseCase;

    @MockBean private ModifyUserUseCase modifyUserUseCase;

    @MockBean private FindUserUseCase findUserUseCase;

    private UserResponse userResponse;
    private UserCreateRequest userCreateRequest;


    @BeforeEach
    void init(){
        userResponse = UserResponse.builder().name("user").build();
        userCreateRequest = UserCreateRequest.builder()
                .account("account")
                .email("email")
                .password("password")
                .name("name")
                .verify("")
                .build();
    }


    @Test
    public void 사용자를_생성하는_메서드() {
        // given

        when(registerUserUseCase.create(any())).thenReturn(Mono.just(userResponse));

        // when
        webTestClient.post()
                .uri("/users/register-eda")
                .bodyValue(userCreateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "Success".equals(body.getMessage());
                });
    }

    @Test
    public void 테스트를_위한_임시_사용자를_생성하는_메서드() {
        // given
        when(registerUserUseCase.create(any())).thenReturn(Mono.just(userResponse));

        // when
        webTestClient.post()
                .uri("/users/register/temp")
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "Success".equals(body.getMessage());
                });
    }

    @Test
    public void 모든_사용자_목록을_조회하는_메서드() {
        // given
        when(findUserUseCase.findAll()).thenReturn(Mono.just(List.of(userResponse)));

        // when
        webTestClient.get()
                .uri("/users/memberList")
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "Success".equals(body.getMessage());
                });
    }

    @Test
    public void membershipId를_통해_사용자를_조회하는_메서드() {
        // given
        Long userId = 1L;
        when(findUserUseCase.findUserByMembershipId(userId)).thenReturn(Mono.just(userResponse));

        // when
        webTestClient.get()
                .uri("/users/{id}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MessageEntity.class)
                .consumeWith(response -> {
                    MessageEntity body = response.getResponseBody();
                    assert body != null;
                    assert "Success".equals(body.getMessage());
                });
    }
}

