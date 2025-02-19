package com.ns.result.adapter.in.web;

import static com.ns.common.task.SubTask.TaskStatus.success;
import static com.ns.common.task.SubTask.TaskType.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.common.ClientRequest;
import com.ns.common.task.SubTask;
import com.ns.result.adapter.axon.command.GameFinishedCommand;
import com.ns.result.adapter.axon.query.QueryPlayer;
import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.application.service.PlayerService;
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
    private final PlayerService playerService;

    private final CommandGateway commandGateway;

    @PostMapping(path="/create")
    Mono<Void> RegisterPlayerByEvent(@RequestParam String membershipId){
        return playerService.createPlayerByEvent(membershipId);
    }

    @PostMapping(path="/increase-elo/event")
    Mono<QueryPlayer> UpdateEloByEvent(@RequestParam String membershipId){
        Random random = new Random();
        Long elo = random.nextLong(2000);
        return playerService.updateEloByEvent(membershipId, elo);
    }

    @PostMapping(path="/increase-elo/saga")
    Mono<QueryPlayer> UpdateEloBySaga(@RequestParam String membershipId){
        Random random = new Random();
        Long elo = random.nextLong(2000);
        return playerService.updateEloBySaga(membershipId, elo);
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
        return playerService.findAll();
    }

    @GetMapping("/player/{membershipId}")
    public Mono<QueryPlayer> findByMembershipId(@PathVariable String membershipId){
        return playerService.queryToPlayerByMembershipId(membershipId);
    }

    @GetMapping("/player/db/{membershipId}")
    public Mono<Player> findByMembershipIddb(@PathVariable String membershipId){
        return playerService.findByMembershipId(membershipId);
    }
}
