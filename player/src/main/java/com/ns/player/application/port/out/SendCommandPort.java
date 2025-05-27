package com.ns.player.application.port.out;

import com.ns.common.CreatePlayerCommand;
import com.ns.common.task.SubTask;
import com.ns.player.adapter.axon.command.UpdateEloCommand;
import reactor.core.publisher.Mono;

public interface SendCommandPort {
    void sendReceivedGameFinishedEvent(SubTask subtask);
    Mono<String> sendCreatePlayer(CreatePlayerCommand command);
    Mono<String> sendUpdatePlayerElo(UpdateEloCommand command);
}
