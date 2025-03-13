package com.ns.result.adapter.out.persistence.elasticsearch;

import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlayerRepository extends ReactiveElasticsearchRepository<Player, String> {
    Mono<Player> findByNickname(String nickname);
    Flux<Player> findByNicknameStartingWith(String query);
}
