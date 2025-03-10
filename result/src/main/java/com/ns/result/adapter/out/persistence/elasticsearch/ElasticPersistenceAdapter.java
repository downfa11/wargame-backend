package com.ns.result.adapter.out.persistence.elasticsearch;

import static com.ns.result.adapter.out.persistence.elasticsearch.ResultMapper.mapToResultDocument;

import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.common.anotation.PersistanceAdapter;
import com.ns.result.application.port.out.search.AutoCompletePlayerPort;
import com.ns.result.application.port.out.search.DeleteResultPort;
import com.ns.result.application.port.out.search.FindResultPort;
import com.ns.result.application.port.out.search.RegisterResultPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@PersistanceAdapter
@RequiredArgsConstructor
public class ElasticPersistenceAdapter implements RegisterResultPort, DeleteResultPort, FindResultPort, AutoCompletePlayerPort {
    public static final int RESULT_SEARCH_SIZE = 30;

    private final ResultRepository resultRepository;
    private final PlayerRepository playerRepository;

    @Override
    public Mono<Result> saveResult(GameFinishedEvent gameFinishedEvent) {
        return Flux.fromIterable(gameFinishedEvent.getBlueTeams())
                .concatWith(Flux.fromIterable(gameFinishedEvent.getRedTeams()))
                .flatMap(clientRequest -> savePlayer(clientRequest))
                .then(resultRepository.save(mapToResultDocument(gameFinishedEvent)));
    }

    private Mono<Void> savePlayer(ClientRequest clientRequest) {
        Player player = Player.builder()
                .nickname(clientRequest.getUser_name())
                .build();
        return playerRepository.save(player).then();
    }

    @Override
    public Flux<Result> findAll() {
        return resultRepository.findAll();
    }

    @Override
    public Flux<Result> searchByUserName(String name, int offset) {
        return resultRepository.searchByUserName(name, RESULT_SEARCH_SIZE, offset);
    }

    @Override
    public Flux<Result> searchByMembershipId(Long membershipId, int offset) {
        return resultRepository.searchByMembershipId(membershipId, RESULT_SEARCH_SIZE, offset);
    }

    @Override
    public Mono<Boolean> deleteResult(String spaceId) {
        return resultRepository.searchBySpaceId(spaceId)
                .flatMap(resultRepository::delete)
                .then(Mono.just(true))
                .defaultIfEmpty(false);
    }

    @Override
    public Flux<String> getAutoCompleteSuggestions(String query) {
        return playerRepository.findByNicknameStartingWith(query)
                .map(player -> player.getNickname());
    }
}
