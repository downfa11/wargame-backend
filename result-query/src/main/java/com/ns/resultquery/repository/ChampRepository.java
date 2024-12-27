package com.ns.resultquery.repository;

import com.ns.resultquery.domain.Champ;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChampRepository extends ReactiveCrudRepository<Champ, Long> {
    @Query("SELECT champion_id, name FROM champion_stats")
    Flux<Champ> findAllChampNames();
}
