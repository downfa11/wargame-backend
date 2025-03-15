package com.ns.result.adapter;

import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.adapter.out.persistence.psql.PlayerPersistenceAdapter;
import com.ns.result.adapter.out.persistence.psql.PlayerR2dbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlayerPersistenceAdapterTest {

    @Mock private PlayerR2dbcRepository playerRepository;
    @InjectMocks private PlayerPersistenceAdapter playerPersistenceAdapter;

    private Player player;
    private String membershipId = "12345";
    private String aggregateIdentifier = "aggId";

    @BeforeEach
    void init() {
        player = Player.builder()
                .membershipId(membershipId)
                .aggregateIdentifier(aggregateIdentifier)
                .elo(2000L)
                .code("")
                .build();
    }

    @Test
    void 플레이어를_등록하는_메서드() {
        // given
        when(playerRepository.save(any())).thenReturn(Mono.just(player));

        // when
        Mono<Player> registeredPlayer = playerPersistenceAdapter.registerPlayer(membershipId, aggregateIdentifier);

        // then
        StepVerifier.create(registeredPlayer)
                .expectNext(player)
                .verifyComplete();

        verify(playerRepository, times(1)).save(any());
    }

    @Test
    void 플레이어의_정보를_업데이트하는_메서드() {
        // given
        Long increase = 100L;
        Player updatedPlayer = Player.builder()
                .membershipId(membershipId)
                .aggregateIdentifier(aggregateIdentifier)
                .elo(2100L)  // Increased elo by 100
                .code("")
                .build();
        when(playerRepository.findByMembershipId(membershipId)).thenReturn(Mono.just(player));
        when(playerRepository.save(any())).thenReturn(Mono.just(updatedPlayer));

        // when
        Mono<Player> result = playerPersistenceAdapter.updatePlayerElo(membershipId, increase);

        // then
        StepVerifier.create(result)
                .expectNext(updatedPlayer)
                .verifyComplete();

        verify(playerRepository, times(1)).findByMembershipId(membershipId);
        verify(playerRepository, times(1)).save(any());
    }

    @Test
    void membershipId로_플레이어를_조회하는_메서드() {
        // given
        when(playerRepository.findByMembershipId(membershipId)).thenReturn(Mono.just(player));

        // when
        Mono<Player> foundPlayer = playerPersistenceAdapter.findByMembershipId(membershipId);

        // then
        StepVerifier.create(foundPlayer)
                .expectNext(player)
                .verifyComplete();

        verify(playerRepository, times(1)).findByMembershipId(membershipId);
    }

    @Test
    void 모든_플레이어들을_조회하는_메서드() {
        // given
        when(playerRepository.findAll()).thenReturn(Flux.just(player));

        // when
        Flux<Player> players = playerPersistenceAdapter.findAll();

        // then
        StepVerifier.create(players)
                .expectNext(player)
                .verifyComplete();

        verify(playerRepository, times(1)).findAll();
    }
}

