package com.ns.result.application.port.out;

import com.ns.result.adapter.axon.query.QueryPlayer;
import reactor.core.publisher.Mono;

public interface SendQueryPort {
    Mono<QueryPlayer> sendPlayerQuery(String membershipId);
}
