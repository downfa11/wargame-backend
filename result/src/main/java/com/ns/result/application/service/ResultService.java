package com.ns.result.application.service;


import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.common.anotation.UseCase;
import com.ns.common.task.SubTask;
import com.ns.common.task.Task;
import com.ns.common.task.TaskUseCase;
import com.ns.result.adapter.out.persistence.elasticsearch.Result;
import com.ns.result.application.port.in.FindResultUseCase;
import com.ns.result.application.port.in.RegisterResultUseCase;
import com.ns.result.application.port.out.cache.FindRedisPort;
import com.ns.result.application.port.out.search.FindResultPort;
import com.ns.result.application.port.out.cache.PushRedisPort;
import com.ns.result.application.port.out.search.RegisterResultPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ResultService implements RegisterResultUseCase, FindResultUseCase {
    private final PushRedisPort pushRedisPort;
    private final FindRedisPort findRedisPort;

    private final RegisterResultPort registerResultPort;
    private final FindResultPort findResultPort;

    private final TaskUseCase taskUseCase;

    @Override
    public Flux<Result> getResultList() {
        return findResultPort.findAll();
    }

    @Override
    public Flux<Result> getGameResultsByName(String name, int offset) {
        String key = "results:name:" + name + ":offset:" + offset;
        return getGameResultsByKey(key, offset, findResultPort.searchByUserName(name, offset));
    }

    @Override
    public Flux<Result> getGameResultsByMembershipId(Long membershipId, int offset) {
        String key = "results:membershipId:" + membershipId + ":offset:" + offset;
        return getGameResultsByKey(key, offset, findResultPort.searchByMembershipId(membershipId, offset));
    }

    private Flux<Result> getGameResultsByKey(String key, int offset, Flux<Result> dbResults) {
        return findRedisPort.findResultInRange(key, offset)
                .switchIfEmpty(Flux.defer(() -> dbResults.flatMap(result -> pushRedisPort.pushResult(key, result))));
    }


    @Override
    public Mono<Result> saveResult(GameFinishedEvent gameFinishedEvent) {
        return registerResultPort.saveResult(gameFinishedEvent);
    }

    //=============== test ================//
    @Override
    public Mono<Result> createResultTemp() {
        Random random = new Random();

        ClientRequest bluePlayer1 = createRandomClientRequest(1L, 1L, "Blue", "BluePlayer1");
        ClientRequest bluePlayer2 = createRandomClientRequest(2L, 2L, "Blue", "BluePlayer2");

        ClientRequest redPlayer1 = createRandomClientRequest(3L, 3L, "Red", "RedPlayer1");
        ClientRequest redPlayer2 = createRandomClientRequest(4L, 4L, "Red", "RedPlayer2");

        String winningTeam = random.nextBoolean() ? "Blue" : "Red";
        String losingTeam = winningTeam.equals("Blue") ? "Red" : "Blue";

        GameFinishedEvent dummyResult = GameFinishedEvent.builder()
                .spaceId("dummy-space-id")
                .state("success")
                .channel(random.nextInt(10))
                .room(random.nextInt(10))
                .winTeam(winningTeam)
                .loseTeam(losingTeam)
                .blueTeams(List.of(bluePlayer1, bluePlayer2))
                .redTeams(List.of(redPlayer1, redPlayer2))
                .dateTime(String.valueOf(LocalDateTime.now()))
                .gameDuration(random.nextInt(7200))
                .build();

        return saveResult(dummyResult);
    }

    private ClientRequest createRandomClientRequest(Long membershipId, Long champIndex, String team, String userName) {
        Random random = new Random();

        return ClientRequest.builder()
                .membershipId(membershipId)
                .socket(random.nextInt(100))
                .champindex(champIndex)
                .user_name(userName)
                .team(team)
                .channel(random.nextInt(5))
                .room(random.nextInt(10))
                .kill(random.nextInt(10))
                .death(random.nextInt(10))
                .assist(random.nextInt(10))
                .gold(random.nextInt(10000))
                .level(random.nextInt(18) + 1)
                .maxhp(random.nextInt(100))
                .maxmana(random.nextInt(100))
                .attack(random.nextInt(30))
                .critical(random.nextInt(100))
                .criProbability(random.nextInt(100))
                .attrange(random.nextInt(1000))
                .attspeed(random.nextFloat() * 2)
                .movespeed(random.nextInt(100))
                .itemList(List.of(random.nextInt(100), random.nextInt(100), random.nextInt(100))) // 3개의 랜덤 아이템
                .build();
    }




    //    public Mono<Void> dodge(GameFinishedEvent result) {
//        List<ClientRequest> allTeams = getAllTeams(result);
//
//        if (allTeams.isEmpty()) {
//            log.warn("각 팀이 비어 있습니다!");
//            return Mono.empty();
//        }
//
//        return Flux.fromIterable(allTeams)
//                .flatMap(client -> Mono.just(createDodgeSubTask(client.getMembershipId())))
//                .collectList()
//                .flatMap(subTasks -> resultService.sendTask("task.membership.response", createDodgeTask(subTasks)));
//    }

    private List<ClientRequest> getAllTeams(GameFinishedEvent result){
        List<ClientRequest> allTeams = new ArrayList<>();
        allTeams.addAll(result.getBlueTeams());
        allTeams.addAll(result.getRedTeams());
        return allTeams;
    }

    private SubTask createDodgeSubTask(Long membershipId){
        return taskUseCase.createSubTask("Dodge",
                String.valueOf(membershipId),
                SubTask.TaskType.result,
                SubTask.TaskStatus.ready,
                membershipId);
    }

    private Task createDodgeTask(List<SubTask> subTasks){
        return taskUseCase.createTask(
                "Dodge Request",
                null,
                subTasks);
    }
}


