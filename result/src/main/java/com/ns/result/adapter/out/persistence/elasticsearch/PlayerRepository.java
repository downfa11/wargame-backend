package com.ns.result.adapter.out.persistence.elasticsearch;

import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

public interface PlayerRepository extends ReactiveElasticsearchRepository<Player, String> {
    Flux<Player> findByNicknameStartingWith(String query);
}
