package com.ns.result.adapter.axon;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.result.adapter.axon.command.RollbackUpdateEloCommand;
import com.ns.result.adapter.axon.command.UpdateEloCommand;
import com.ns.result.adapter.axon.event.RollbackGameResultEvent;
import com.ns.result.adapter.axon.event.UpdateEloEvent;
import com.ns.result.application.port.out.player.FindPlayerPort;
import com.ns.result.application.port.out.player.UpdatePlayerPort;
import com.ns.result.application.service.EloService;
import com.ns.result.application.service.PlayerService;
import com.ns.result.application.service.ResultService;
import java.util.ArrayList;
import java.util.List;
import com.ns.result.adapter.out.persistence.psql.Player;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Saga
@RequiredArgsConstructor
@Slf4j
public class GameFinishedSaga {

    private final ResultService resultService;
    private final UpdatePlayerPort updatePlayerPort;
    private final FindPlayerPort findPlayerPort;
    private final EloService eloService;

    private final CommandGateway commandGateway;
    private final EventGateway eventGateway;

    // 게임 종료 이벤트를 수신하여 사용자의 실력 점수나 현재 참여중인 방 번호 등을 업데이트합니다.
    // 단, 픽 창에서 완료하지 않아 나가지는 경우(state:dodge)에 따른 후처리도 함께 진행합니다.

    @StartSaga
    @SagaEventHandler(associationProperty = "spaceId")
    public void handle(GameFinishedEvent event) {
        log.info("GameFinishedEvent saga :" + event.toString());

        // 게임 전적을 Elasticsearch에 기록
        resultService.saveResult(event);
        log.info("saveResult");

        // Result-Query 서비스의 업데이트도 함께 진행
        eventGateway.publish(event);
        log.info("Result-query update");

        // 각 사용자들에게 Elo 변동
        updateElo(event)
                .doOnSuccess(aVoid -> log.info("Elo updated successfully"))
                .doOnError(throwable -> log.error("Error updating Elo: ", throwable))
                .subscribe();
    }

    private Mono<Void> updateElo(GameFinishedEvent event){
        boolean isWin = event.getWinTeam().equalsIgnoreCase("blue");
        //todo. blue 문자열로 받았었나? 치맨가..

        List<MembershipEloRequest> newEloRequests = eloService.updateElo(getEloRequests(event), isWin);
        return Flux.fromIterable(newEloRequests)
                .flatMap(request -> {
                    String membershipId = String.valueOf(request.getMembershipId());
                    Long newElo = request.getElo();

                    return findPlayerPort.findByMembershipId(membershipId)
                            .flatMap(player -> {
                                commandGateway.send(new UpdateEloCommand(player.getAggregateIdentifier(), membershipId, newElo));
                                return Mono.empty();
                            })
                            .doOnError(throwable -> log.error("Failed to update Elo for membershipId: {}", membershipId, throwable));
                }).then();
    }

    private List<MembershipEloRequest> getEloRequests(GameFinishedEvent event){
        List<MembershipEloRequest> eloRequests = new ArrayList<>();
        // membershipId, elo, team
        for(var sa : getClientAll((event))){
            String membershipId = String.valueOf(sa.getMembershipId());
            Long elo = getPlayerElo(membershipId); // Mono<Player>에서 getElo 추출
            MembershipEloRequest membershipEloRequest = new MembershipEloRequest(sa.getMembershipId(), sa.getTeam(), elo);
            eloRequests.add(membershipEloRequest);
        }
        return eloRequests;
    }

    private List<ClientRequest> getClientAll(GameFinishedEvent event){
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

    @SagaEventHandler(associationProperty = "aggregateIdentifier")
    public void handle(UpdateEloEvent event) {
        log.info("UpdateEloEvent saga: " + event.toString());

        // 조회용 db에 기록. 오류 발생시 롤백 처리
        updatePlayerPort.updatePlayer(event.getMembershipId(), event.getElo())
                .doOnSuccess(player -> {

                    // todo. 롤백 테스트시, 여기서 player==null 처리해버리자.

                    if (player == null) {
                        rollback(event);
                    } else {
                        SagaLifecycle.end();
                    }
                })
                .doOnError(throwable -> {
                    log.error("Error during Elo update: ", throwable);
                    rollback(event);
                })
                .subscribe();
    }

    private void rollback(UpdateEloEvent event){
        String rollbackId = UUID.randomUUID().toString();
        SagaLifecycle.associateWith("rollbackId", rollbackId);

        commandGateway.send(new RollbackUpdateEloCommand(rollbackId,event.getAggregateIdentifier(), event.getMembershipId(), event.getElo()))
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                        log.info("Rollback failed");
                    } else {
                        log.info("Saga success : "+ result.toString());
                        SagaLifecycle.end();
                    }
                }
        );
    }

    // 롤백 이벤트를 처리 완료
    @EndSaga
    @SagaEventHandler(associationProperty = "rollbackId")
    public void on(RollbackGameResultEvent event) {
        log.info("RollbackGameResultEvent Saga : "+ event.toString());
    }
}
