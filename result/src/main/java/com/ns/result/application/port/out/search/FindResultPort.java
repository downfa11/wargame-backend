package com.ns.result.application.port.out.search;

import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import reactor.core.publisher.Flux;

public interface FindResultPort {
    Flux<Result> findAll();
    Flux<Result> searchByUserName(String name, int offset);
    Flux<Result> searchByMembershipId(Long membershipId, int offset);
}
