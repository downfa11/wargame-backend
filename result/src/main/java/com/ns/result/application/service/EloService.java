package com.ns.result.application.service;

import com.ns.common.dto.MembershipEloRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EloService {
    private static final int K = 16;


    // TeamElo를 통해 승패에 따른 Elo 점수 변동값을 연산한다.
    public List<MembershipEloRequest> updateTeamElo(List<MembershipEloRequest> team, Long opposingTeamEloSum, boolean isWinner) {
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

    private long calculateElo(long currentElo, long opposingTeamElo, boolean isWinner) {
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
