package com.ns.player.application.port.out;

import com.ns.player.adapter.axon.query.QueryPlayer;
import com.ns.player.application.port.in.PlayerInfo;
import reactor.core.publisher.Mono;

public interface SendQueryPort {
    Mono<QueryPlayer> sendPlayerQuery(String membershipId);
    Mono<PlayerInfo> sendPlayerInfo(String nickname);
}
