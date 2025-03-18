package com.ns.result.adapter.axon;

import com.ns.common.*;
import com.ns.result.adapter.axon.command.MembershipElo;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.event.GameResultSavedEvent;
import com.ns.result.adapter.axon.event.RollbackGameResultEvent;
import com.ns.result.adapter.axon.event.RollbackUpdateEloEvent;
import com.ns.result.adapter.out.persistence.psql.Player;
import com.ns.result.application.port.out.player.FindPlayerPort;
import com.ns.result.application.service.EloService;
import com.ns.result.application.service.PlayerService;
import com.ns.result.application.service.ResultService;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Saga
@NoArgsConstructor
public class GameFinishedSaga {

    @NonNull
    private transient ResultService resultService;
    @NonNull
    private transient PlayerService playerService;
    @NonNull
    private transient FindPlayerPort findPlayerPort;
    @NonNull
    private transient CommandGateway commandGateway;
    @NonNull
    private transient EventGateway eventGateway;

    // 게임 종료 이벤트를 수신하여 사용자의 실력 점수나 현재 참여중인 방 번호 등을 업데이트합니다.
    // 단, 픽 창에서 완료하지 않아 나가지는 경우(state:dodge)에 따른 후처리도 함께 진행합니다.

    @StartSaga
    @SagaEventHandler(associationProperty = "spaceId")
    public void handle(GameFinishedEvent event) {
        // TODO. 닷지인지 여부 확인하기
        if (event.getState().equals("dodge"))
            return;

        // 게임 전적을 Elasticsearch에 기록
        resultService.saveResult(event)
                .doOnSuccess(result -> eventGateway.publish(new GameResultSavedEvent(event.getSpaceId(), event.getWinTeam(), event.getLoseTeam(), event.getBlueTeams(), event.getRedTeams())))
                .subscribe();
    }

    @SagaEventHandler(associationProperty = "spaceId")
    public void handle(GameResultSavedEvent event) {
        log.info("GameResultSavedEvent received. Updating Result-Query...");

        CreateResultQueryEvent createResultQueryEvent = CreateResultQueryEvent.builder()
                .spaceId(event.getSpaceId())
                .blueTeams(event.getBlueTeams())
                .redTeams(event.getRedTeams())
                .loseTeam(event.getLoseTeam())
                .winTeam(event.getWinTeam())
                .build();

        // Mono.fromRunnable(() -> eventGateway.publish(createResultQueryEvent));
        log.info("createResultQueryEvent success: " + createResultQueryEvent);

        // 실제로 위에서 resutl-query로 전달하고 나면, 아래의 로직을 거기서 실행해야함
        ResultQueryUpdatedEvent resultQueryUpdatedEvent = new ResultQueryUpdatedEvent(
                event.getSpaceId(),
                event.getWinTeam(),
                event.getLoseTeam(),
                event.getBlueTeams(),
                event.getRedTeams()
        );
        Mono.fromRunnable(() -> eventGateway.publish(resultQueryUpdatedEvent))
                .doOnSuccess(aVoid -> log.info("ResultQueryUpdatedEvent published"))
                .doOnError(throwable -> log.error("error to publish ResultQueryUpdatedEvent", throwable))
                .subscribe();
    }

    @SagaEventHandler(associationProperty = "spaceId")
    public void handle(ResultQueryUpdatedEvent event, EloService eloService) {
        log.info("ResultQueryUpdatedEvent received. Updating Elo... ");

        boolean isWin = event.getWinTeam().equalsIgnoreCase("blue");
        List<MembershipEloRequest> newEloRequests = eloService.updateElo(getEloRequests(event), isWin);
        List<MembershipElo> successfullyUpdatedPlayers = new ArrayList<>();

        // 14는 업데이트하고, 15에서 오류가 발생 -> 14만 롤백해서 이전 상태로 복구
        // 롤백 테스트시 순서보장 처리 되돌리기 (concat → flatMap 전환)
        Flux.fromIterable(newEloRequests)
                .concatMap(request -> handleEloUpdateRequest(request, successfullyUpdatedPlayers))
                .then()
                .onErrorResume(throwable -> {
                    log.error("Elo 업데이트 중 오류 발생, 롤백을 진행합니다.", throwable);
                    return handleRollback(event, successfullyUpdatedPlayers, newEloRequests);
                })
                .subscribe();
    }

    private Mono<Void> handleEloUpdateRequest(MembershipEloRequest request, List<MembershipElo> successfullyUpdatedPlayers) {
        String membershipId = String.valueOf(request.getMembershipId());

        return findPlayerPort.findByMembershipId(membershipId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Not found a Player: membershipId={}", membershipId);
                    return Mono.error(new RuntimeException("Player not found for membershipId=" + membershipId));
                }))
                .flatMap(player -> {
                    MembershipElo membershipElo = new MembershipElo(membershipId, player.getAggregateIdentifier(), player.getElo(), request.getElo());
                    log.info("흠. "+membershipId+"의 elo는 "+player.getElo()+"에서 새롭게 "+ request.getElo()+"로 변할 예정이다.");
                    return updatePlayerElo(membershipElo, player.getAggregateIdentifier(), successfullyUpdatedPlayers);
                });
    }

    private Mono<Void> updatePlayerElo(MembershipElo request, String aggregateIdentifier, List<MembershipElo> successfullyUpdatedPlayers) {
        String membershipId = request.membershipId();
        Long newElo = request.curElo();
        UpdateEloCommand command = new UpdateEloCommand(aggregateIdentifier, membershipId, newElo);

        if (request.membershipId().equals("19")) {
            log.error("에러 주입 - membershipId={}", membershipId);
            return Mono.error(new RuntimeException("에러 주입 - membershipId=" + membershipId));
        }

        return Mono.fromFuture(() -> commandGateway.send(command))
                .doOnSuccess(avoid -> log.info("Elo 이벤트 소싱 성공: membershipId={}, newElo={}", membershipId, newElo))
                .then(Mono.defer(() ->  playerService.updateElo(membershipId, newElo)))
                .doOnSuccess(aVoid -> {
                    successfullyUpdatedPlayers.add(request);
                    log.info("Elo 업데이트 성공: membershipId={}, newElo={}", membershipId, newElo);
                }).then();
    }

    private Mono<Void> handleRollback(ResultQueryUpdatedEvent event, List<MembershipElo> successfullyUpdatedPlayers, List<MembershipEloRequest> newEloRequests) {
        log.info("rollback start: " + successfullyUpdatedPlayers.size() + "/" + newEloRequests.size());

        String spaceId = event.getSpaceId();
        if (successfullyUpdatedPlayers.size() < newEloRequests.size()) {
            return Flux.fromIterable(successfullyUpdatedPlayers)
                    .concatMap(membershipElo -> rollbackElo(spaceId, membershipElo))
                    .then()
                    .doOnTerminate(() -> {
                        eventGateway.publish(new RollbackUpdateQueryEvent(event.getSpaceId(), event.getWinTeam(), event.getLoseTeam(), event.getBlueTeams(), event.getRedTeams()));
                        eventGateway.publish(new RollbackGameResultEvent(spaceId));
                        log.info("모든 롤백이 완료되어서 분산 트랜잭션을 종료합니다.");
                    });
        }

        log.info("모든 Elo 업데이트가 성공하여 롤백하지 않습니다.");
        return Mono.empty();
    }

    private Mono<Void> rollbackElo(String spaceId, MembershipElo membershipElo) {
        log.info("성공적으로 업데이트된 사용자 " + membershipElo.membershipId() + "의 Elo를 롤백합니다. 이전:"+membershipElo.oldElo()+ ", 현재:"+membershipElo.curElo());

        return Mono.fromRunnable(() -> {
            eventGateway.publish(new RollbackUpdateEloEvent(spaceId, membershipElo.aggregateIdentifier(), membershipElo.membershipId(), membershipElo.oldElo(), membershipElo.curElo()));
            playerService.updateElo(membershipElo.membershipId(), membershipElo.oldElo())
                    .subscribe();
        });
    }


    @EndSaga
    @SagaEventHandler(associationProperty = "spaceId")
    public void handle(RollbackGameResultEvent event) {
        log.warn("RollbackGameResultEvent received.");
        resultService.deleteResult(event.getSpaceId())
                .doOnSuccess(bool -> log.info(event.getSpaceId() + "에 해당하는 전적을 삭제합니다. " + bool))
                .subscribe();
    }

    @SagaEventHandler(associationProperty = "spaceId")
    public void handle(RollbackUpdateEloEvent event) {
        log.warn("RollbackUpdateEloEvent received");
        commandGateway.send(new UpdateEloCommand(event.getAggregateIdentifier(), event.getMembershipId(), event.getOldElo()));
    }

    private List<MembershipEloRequest> getEloRequests(ResultQueryUpdatedEvent event) {
        List<MembershipEloRequest> eloRequests = new ArrayList<>();
        // membershipId, elo, team
        for (var sa : getClientAll((event))) {
            String membershipId = String.valueOf(sa.getMembershipId());
            Long elo = getPlayerElo(membershipId);
            MembershipEloRequest membershipEloRequest = new MembershipEloRequest(sa.getMembershipId(), sa.getTeam(), elo);
            eloRequests.add(membershipEloRequest);
        }
        return eloRequests;
    }

    private List<ClientRequest> getClientAll(ResultQueryUpdatedEvent event) {
        List<ClientRequest> allClients = new ArrayList<>();
        allClients.addAll(event.getBlueTeams());
        allClients.addAll(event.getRedTeams());

        return allClients.stream()
                .collect(Collectors.toList());
    }

    private Long getPlayerElo(String membershipId) {
        return findPlayerPort.findByMembershipId(membershipId)
                .map(Player::getElo)
                .block();
    }

    @Autowired
    public void setResultService(ResultService resultService) {
        this.resultService = resultService;
    }

    @Autowired
    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Autowired
    public void setFindPlayerPort(FindPlayerPort findPlayerPort) {
        this.findPlayerPort = findPlayerPort;
    }

    @Autowired
    public void setCommandGateway(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Autowired
    public void setEventGateway(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }
}
