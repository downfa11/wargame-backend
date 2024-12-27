package com.ns.resultquery;

import com.ns.common.ClientRequest;
import com.ns.common.ResultRequestEvent;
import com.ns.resultquery.dto.MembershipResultEventDto;
import com.ns.resultquery.dto.ResultEventDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ResultQueryMapper {

    private static final Map<Long, String> champList = new HashMap<>();

    ResultQueryMapper(){
        for (long i = 1; i <= 10; i++) {
            champList.put(i, "test-champ-" + i);
        }
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

    public static List<ClientRequest> getAllTeamClientRequests(ResultRequestEvent event){
        List<ClientRequest> allClients = new ArrayList<>();
        allClients.addAll(event.getBlueTeams());
        allClients.addAll(event.getRedTeams());

        return allClients;
    }

}
