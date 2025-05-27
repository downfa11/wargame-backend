package com.ns.player.adapter.axon;

import com.ns.common.CreatePlayerCommand;
import com.ns.player.adapter.axon.command.ModifyCodeCommand;
import com.ns.player.adapter.axon.command.UpdateEloCommand;
import com.ns.player.adapter.axon.event.CreatePlayerEvent;
import com.ns.player.adapter.axon.event.ModifyCodeEvent;
import com.ns.player.adapter.axon.event.UpdateEloEvent;
import com.ns.player.adapter.out.persistence.PlayerPersistenceAdapter;
import com.ns.player.application.port.out.RegisterPlayerPort;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.UUID;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate(snapshotTriggerDefinition = "snapshotTrigger", cache = "snapshotCache")
@Getter
@Slf4j
@NoArgsConstructor
public class PlayerAggregate {
    @AggregateIdentifier
    private String id;

    private String membershipId;
    private String nickname;
    private Long elo;
    private String code;

    @CommandHandler
    public PlayerAggregate(CreatePlayerCommand command, RegisterPlayerPort registerPlayerPort){
        id= UUID.randomUUID().toString();
        membershipId = command.getMembershipId();
        nickname = command.getNickname();
        log.info("CreatePlayerCommand command handler: "+ command.getMembershipId());

        registerPlayerPort.registerPlayer(command.getMembershipId(), id, command.getNickname())
                .subscribe();

        apply(new CreatePlayerEvent(command.getMembershipId(), id, command.getNickname()));
    }

    @EventSourcingHandler // 이벤트 리플레이시, 이벤트 소싱 핸들러만 호출한다
    public void onCreateMemberEvent(CreatePlayerEvent event){
        id = event.getId();
        membershipId = event.getMembershipId();
        nickname = event.getNickname();
        elo = 2000L;
        code = "";
        log.info("CreatePlayerEvent handler: "+ membershipId +":"+ nickname+" ="+elo + "   : " + id);
    }

    @CommandHandler
    public void handleUpdateElo(@NotNull UpdateEloCommand command){
        id = command.getAggregateIdentifier();
        apply(new UpdateEloEvent(id, command.getMembershipId(), command.getElo()));
    }

    @EventSourcingHandler
    public void onUpdateEloEvent(UpdateEloEvent event){
        log.info("UpdateEloEvent "+ event.getMembershipId()+"'s elo: " + elo + " -> " + event.getElo());
        id = event.getAggregateIdentifier();
        membershipId = event.getMembershipId();
        elo = event.getElo();
        code = "";
    }

    @CommandHandler
    public void handleModifyCode(@NotNull ModifyCodeCommand command){
        id = command.getAggregateIdentifier();
        apply(new ModifyCodeEvent(id, command.getMembershipId(), command.getCode()));
    }

    @EventSourcingHandler
    public void onModifyCodeEvent(ModifyCodeEvent event){
        log.info("onModifyCodeEvent "+ event.getMembershipId()+"'s code: " + code + " -> " + event.getCode());
        id = event.getAggregateIdentifier();
        code = event.getCode();
    }
}
