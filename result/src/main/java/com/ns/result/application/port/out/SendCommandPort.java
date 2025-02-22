package com.ns.result.application.port.out;

import com.ns.common.task.SubTask;
import com.ns.result.adapter.axon.command.CreatePlayerCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import reactor.core.publisher.Mono;

public interface SendCommandPort {
    void sendReceivedGameFinishedEvent(SubTask subtask);
    Mono<String> sendCreatePlayer(CreatePlayerCommand command);
    Mono<String> sendUpdatePlayer(UpdateEloCommand command);
}
