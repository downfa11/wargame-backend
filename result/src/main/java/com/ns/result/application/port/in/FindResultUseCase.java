package com.ns.result.application.port.in;

import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Flux;

public interface FindResultUseCase {
    Flux<Result> getResultList();
    Flux<Result> getGameResultsByName(String name, int offset);
    Flux<Result> getGameResultsByMembershipId(Long membershipId, int offset);
}
