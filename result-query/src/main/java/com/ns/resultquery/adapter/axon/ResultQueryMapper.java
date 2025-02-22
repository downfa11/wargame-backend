package com.ns.resultquery.adapter.axon;

import com.ns.common.ClientRequest;
import com.ns.common.GameFinishedEvent;
import com.ns.resultquery.adapter.out.persistence.ChampRepository;
import com.ns.resultquery.domain.dto.MembershipResultEventDto;
import com.ns.resultquery.domain.dto.ResultEventDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResultQueryMapper {

    private static final Map<Long, String> champList = new HashMap<>();

    private final ChampRepository champRepository;

    public ResultQueryMapper(ChampRepository champRepository){
        this.champRepository = champRepository;
        initializeChampList();
    }

    private void initializeChampList() {
        champRepository.findAllChampNames()
                .doOnNext(champ -> {
                    // log.info(champ.getChampionId() + "번째 챔프의 이름 : " + champ.getName());
                    champList.put(Long.valueOf(champ.getChampionId()), champ.getName());
                })
                .then()
                .subscribe();
    }

    public static MembershipResultEventDto getMembershipResultEventDto(ClientRequest clientRequest, Long winCount){
        Long champIndex = clientRequest.getChampindex();

        return MembershipResultEventDto.builder()
                .membershipId(clientRequest.getMembershipId())
                .userName(clientRequest.getUser_name())
                .champIndex(champIndex)
                .champName(champList.get(champIndex))
                .resultCount(1L)
                .winCount(winCount)
                .loseCount(1-winCount)
                .build();
    }

    public static ResultEventDto getResultEventDto(ClientRequest clientRequest, Long winCount){
        Long champIndex = clientRequest.getChampindex();
        return ResultEventDto.builder()
                .champIndex(champIndex)
                .champName(champList.get(champIndex))
                .resultCount(1L)
                .winCount(winCount)
                .loseCount(1-winCount)
                .build();
    }

    public static List<ClientRequest> getAllTeamClientRequests(GameFinishedEvent event){
        List<ClientRequest> allClients = new ArrayList<>();
        allClients.addAll(event.getBlueTeams());
        allClients.addAll(event.getRedTeams());

        return allClients;
    }

}
