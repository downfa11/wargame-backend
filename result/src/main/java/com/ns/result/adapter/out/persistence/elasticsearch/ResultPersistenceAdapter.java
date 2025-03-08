package com.ns.result.adapter.out.persistence.elasticsearch;

import static com.ns.result.adapter.out.persistence.elasticsearch.ResultMapper.mapToResultDocument;

import com.ns.common.GameFinishedEvent;
import com.ns.common.anotation.PersistanceAdapter;
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
public class ResultPersistenceAdapter implements RegisterResultPort, DeleteResultPort, FindResultPort {
    public static final int RESULT_SEARCH_SIZE = 30;
    private final ResultRepository resultRepository;

    @Override
    public Mono<Result> saveResult(GameFinishedEvent gameFinishedEvent) {
        return resultRepository.save(mapToResultDocument(gameFinishedEvent));
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
}
