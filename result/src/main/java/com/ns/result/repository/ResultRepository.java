package com.ns.result.repository;

import com.ns.result.domain.entity.Result;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

public interface ResultRepository extends ReactiveElasticsearchRepository<Result, String> {
    @Query("{\"bool\": {\"should\": [" +
            "{\"nested\": {\"path\": \"blueTeams\", \"query\": {\"match\": {\"blueTeams.user_name\": \"?0\"}}}}," +
            "{\"nested\": {\"path\": \"redTeams\", \"query\": {\"match\": {\"redTeams.user_name\": \"?0\"}}}}" +
            "]}}")
    Flux<Result> searchByUserName(String userName);
}

