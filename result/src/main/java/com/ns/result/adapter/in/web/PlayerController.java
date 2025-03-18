package com.ns.result.adapter.in.web;

import static com.ns.common.task.SubTask.TaskStatus.success;
import static com.ns.common.task.SubTask.TaskType.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.ClientRequest;
import com.ns.common.task.SubTask;
import com.ns.result.adapter.axon.command.GameFinishedCommand;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.application.port.in.FindPlayerUseCase;
import com.ns.result.application.port.in.RegisterPlayerUseCase;
import com.ns.result.application.port.in.UpdatePlayerUseCase;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/v1/player")
@RequiredArgsConstructor
public class PlayerController {
    private final RegisterPlayerUseCase registerPlayerUseCase;
    private final UpdatePlayerUseCase updatePlayerUseCase;
    private final FindPlayerUseCase findPlayerUseCase;

    private final CommandGateway commandGateway;

    @PostMapping(path="/create")
    Mono<QueryPlayer> RegisterPlayerByEvent(@RequestParam String membershipId){
        return registerPlayerUseCase.createPlayer(membershipId);
    }

    @PostMapping(path="/increase-elo/event")
    Mono<QueryPlayer> UpdateEloByEvent(@RequestParam String membershipId, @RequestParam Long elo){
//        Random random = new Random();
//        Long elo = random.nextLong(1000);
        return updatePlayerUseCase.updateEloByEvent(membershipId, elo);
    }

    @GetMapping("/test/room")
    public Mono<Void> testSuccessGameRoom(@RequestParam String state){
        GameFinishedCommand command = GameFinishedCommand.builder()
                .spaceId("12345")
                .state(state)
                .channel(1)
                .room(1)
                .winTeam("blue")
                .loseTeam("red")
                .blueTeams(List.of(new ClientRequest(), new ClientRequest()))
                .redTeams(List.of(new ClientRequest(), new ClientRequest()))
                .dateTime("2025-02-01T12:00:00Z")
                .gameDuration(120)
                .build();

        SubTask subTask = SubTask.builder()
                .taskType(result)
                .status(success)
                .data(command)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        GameFinishedCommand axonCommand = objectMapper.convertValue(subTask.getData(), GameFinishedCommand.class);

        return Mono.fromFuture(() -> commandGateway.send(axonCommand))
                .doOnSuccess(success -> log.info("GameFinishedCommand sent successfully: " + success))
                .doOnError(throwable -> log.error("Failed to send GameFinishedCommand: " + throwable)).then();
    }

    @GetMapping("/playerList")
    public Flux<Player> findAllPlayers(){
        return findPlayerUseCase.findAll();
    }

    @GetMapping("/player/{membershipId}")
    public Mono<QueryPlayer> findByMembershipId(@PathVariable String membershipId){
        return findPlayerUseCase.queryToPlayerByMembershipId(membershipId);
    }
}
