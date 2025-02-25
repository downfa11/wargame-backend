package com.ns.result.application.service;

import com.ns.result.adapter.axon.MembershipEloRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EloService {
    private static final int K = 16;


    public List<MembershipEloRequest> updateElo(List<MembershipEloRequest> team, boolean isWinner){
        log.info("업데이트 이전 Elo : {}", team);

        Long blueEloSum = calcTeamEloSum(team.stream()
                .filter(client -> client.getTeam().equals("blue"))
                .collect(Collectors.toList()));

        Long redEloSum = calcTeamEloSum(team.stream()
                    .filter(client -> !client.getTeam().equals("blue"))
                    .collect(Collectors.toList()));

        List<MembershipEloRequest> updatedBlueTeam = updateTeamElo(
                team.stream().filter(client -> client.getTeam().equals("blue")).collect(Collectors.toList()),
                redEloSum, isWinner);

        List<MembershipEloRequest> updatedRedTeam = updateTeamElo(
                team.stream().filter(client -> !client.getTeam().equals("blue")).collect(Collectors.toList()),
                blueEloSum, !isWinner);

        List<MembershipEloRequest> updatedTeams = new ArrayList<>();
        updatedTeams.addAll(updatedBlueTeam);
        updatedTeams.addAll(updatedRedTeam);

        log.info("업데이트 이후 Elo : {}", updatedTeams);
        return updatedTeams;
    }

    // TeamElo를 통해 승패에 따른 Elo 점수 변동값을 연산한다.
    private List<MembershipEloRequest> updateTeamElo(List<MembershipEloRequest> team, Long opposingTeamEloSum, boolean isWinner) {
        return team.stream()
                .map(membershipEloRequest -> calcMembershipEloRequest(membershipEloRequest, opposingTeamEloSum, isWinner))
                .toList();
    }

    private MembershipEloRequest calcMembershipEloRequest(MembershipEloRequest membershipEloRequest, Long opposingTeamEloSum, boolean isWinner){
        Long currentElo = membershipEloRequest.getElo();
        Long newElo = calculateElo(currentElo, opposingTeamEloSum, isWinner);
        membershipEloRequest.setElo(newElo);
        return membershipEloRequest;
    }

    public long calculateElo(long currentElo, long opposingTeamElo, boolean isWinner) {
        final double EA = 1.0 / (1.0 + Math.pow(10, (opposingTeamElo - currentElo) / 400.0));
        int SA = isWinner ? 1 : 0;
        return (long) (currentElo + K * (SA - EA));
    }

    public Long calcTeamEloSum(List<MembershipEloRequest> eloRequests){
        return eloRequests.stream()
                .mapToLong(MembershipEloRequest::getElo)
                .sum();
    }

}
