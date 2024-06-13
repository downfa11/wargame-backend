package com.ns.wargame.Repository;

import com.ns.wargame.Domain.GameResultDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

public interface ResultRepository extends ReactiveElasticsearchRepository<GameResultDocument, String> {
    @Query("{\"bool\": {\"should\": [" +
            "{\"nested\": {\"path\": \"blueTeams\", \"query\": {\"match\": {\"blueTeams.user_name\": \"?0\"}}}}," +
            "{\"nested\": {\"path\": \"redTeams\", \"query\": {\"match\": {\"redTeams.user_name\": \"?0\"}}}}" +
            "]}}")
    Flux<GameResultDocument> searchByUserName(String userName);
}

