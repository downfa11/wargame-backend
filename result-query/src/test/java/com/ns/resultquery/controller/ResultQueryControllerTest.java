package com.ns.resultquery.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ns.resultquery.adapter.axon.query.ChampStat;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.adapter.in.web.ResultQueryController;
import com.ns.resultquery.application.port.in.FindStatisticsUseCase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(ResultQueryController.class)
class ResultQueryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private FindStatisticsUseCase findStatisticsUseCase;

    private CountSumByChamp champData;
    private CountSumByMembership membershipData;

    @BeforeEach
    void init() {
        champData = CountSumByChamp.builder()
                .champName("champ")
                .champCount(100L)
                .winCount(50L)
                .loseCount(50L)
                .build();

        membershipData = CountSumByMembership.builder()
                .username("player")
                .entireCount(100L)
                .winCount(50L)
                .loseCount(50L)
                .champStatList(List.of(new ChampStat(1L, "champ", 10L, 7L, 3L, "70.0%")))
                .build();
    }

    @Test
    void 챔프_이름으로_통계를_조회하는_메서드() throws Exception {
        when(findStatisticsUseCase.findStatisticsByChampion(anyString()))
                .thenReturn(champData);

        mockMvc.perform(MockMvcRequestBuilders.get("/statistics/query/champ/" + champData.getChampName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.champName").value(champData.getChampName()))
                .andExpect(jsonPath("$.champCount").value(champData.getChampCount()))
                .andExpect(jsonPath("$.winCount").value(champData.getWinCount()))
                .andExpect(jsonPath("$.loseCount").value(champData.getLoseCount()))
                .andExpect(jsonPath("$.percent").value(String.format("%.1f", (double) champData.getWinCount() / champData.getChampCount() * 100) + "%"));
    }

    @Test
    void 사용자_이름으로_통계를_조회하는_메서드() throws Exception {
        when(findStatisticsUseCase.findStatisticsByUserName(anyString()))
                .thenReturn(membershipData);

        mockMvc.perform(MockMvcRequestBuilders.get("/statistics/query/user/" + membershipData.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value(membershipData.getUsername()))
                .andExpect(jsonPath("$.entireCount").value(membershipData.getEntireCount()))
                .andExpect(jsonPath("$.winCount").value(membershipData.getWinCount()))
                .andExpect(jsonPath("$.loseCount").value(membershipData.getLoseCount()))
                .andExpect(jsonPath("$.percent").value(String.format("%.1f", (double) membershipData.getWinCount() / membershipData.getEntireCount() * 100) + "%"))
                .andExpect(jsonPath("$.champStatList[0].champName").value("champ"))
                .andExpect(jsonPath("$.champStatList[0].winCount").value(7));
    }
}