package com.ns.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ns.result.adapter.axon.MembershipEloRequest;
import com.ns.result.application.service.EloService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @InjectMocks private EloService eloService;


    MembershipEloRequest eloRequest1 = new MembershipEloRequest(1001L, "blue", 1000L);
    MembershipEloRequest eloRequest2 = new MembershipEloRequest(1002L, "blue", 1200L);
    MembershipEloRequest eloRequest3 = new MembershipEloRequest(1003L, "red", 1100L);
    MembershipEloRequest eloRequest4 = new MembershipEloRequest(1004L, "red", 1150L);

    List<MembershipEloRequest> team = Arrays.asList(eloRequest1, eloRequest2, eloRequest3, eloRequest4);

    @Test
    void Blue팀이_승리한_경우_실력점수_업데이트() {
        // given
        boolean isWinner = true;
        // when
        List<MembershipEloRequest> updatedTeam = eloService.updateElo(team, isWinner);
        // then
        assertEquals(4, updatedTeam.size());
        assertTrue(updatedTeam.stream().allMatch(elo -> elo.getElo() > 0));
    }

    @Test
    void Red팀이_승리한_경우_실력점수_업데이트() {
        // given
        boolean isWinner = false;
        // when
        List<MembershipEloRequest> updatedTeam = eloService.updateElo(team, isWinner);
        // then
        assertEquals(4, updatedTeam.size());
        assertTrue(updatedTeam.stream().allMatch(elo -> elo.getElo() > 0));
    }

    @Test
    void 각_팀의_실력점수_합을_연산하는_메서드() {
        // given
        List<MembershipEloRequest> team = Arrays.asList(eloRequest1, eloRequest2);

        // when
        Long teamEloSum = eloService.calcTeamEloSum(team);

        // then
        assertEquals(2200L, teamEloSum);
    }

    @Test
    void 승리한_경우_실력점수_업데이트() {
        // given
        long currentElo = 1000L;
        long opposingTeamElo = 1200L;
        boolean isWinner = true;

        // when
        long newElo = eloService.calculateElo(currentElo, opposingTeamElo, isWinner);

        // then
        assertTrue(newElo > currentElo);
    }

    @Test
    void 패배한_경우_실력점수_업데이트() {
        // given
        long currentElo = 1000L;
        long opposingTeamElo = 1200L;
        boolean isWinner = false;

        // when
        long newElo = eloService.calculateElo(currentElo, opposingTeamElo, isWinner);

        // then
        assertTrue(newElo < currentElo);
    }
}
