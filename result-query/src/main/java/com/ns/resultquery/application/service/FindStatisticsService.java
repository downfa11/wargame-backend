package com.ns.resultquery.application.service;

import com.ns.common.anotation.UseCase;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.in.FindStatisticsUseCase;
import com.ns.resultquery.application.port.out.FindStatisticsPort;
import com.ns.resultquery.application.port.out.cache.FindRedisPort;
import com.ns.resultquery.application.port.out.cache.PushRedisPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class FindStatisticsService implements FindStatisticsUseCase {
    private final PushRedisPort pushRedisPort;
    private final FindRedisPort findRedisPort;
    private final FindStatisticsPort findStatisticsPort;

    @Override
    public CountSumByChamp findStatisticsByChampion(String champName) {
        String key = "statistics:name:" + champName;

        CountSumByChamp cached = findRedisPort.findCountSumByChampInRange(key);
        if (cached != null) {
            return cached;
        }

        CountSumByChamp result = findStatisticsPort.queryToResultSumByChampName(champName);
        pushRedisPort.pushCountSumByChamp(key, result);
        return result;
    }

    @Override
    public CountSumByMembership findStatisticsByUserName(String userName) {
        String key = "statistics:name:" + userName;

        CountSumByMembership cached = findRedisPort.findCountSumByMembershipInRange(key);
        if (cached != null) {
            return cached;
        }

        CountSumByMembership result = findStatisticsPort.queryToResultByUserName(userName);
        pushRedisPort.pushCountSumByMembership(key, result);
        return result;
    }
}
