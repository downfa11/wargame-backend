package com.ns.resultquery.adapter.out.persistence;

import com.ns.common.anotation.PersistanceAdapter;
import com.ns.resultquery.adapter.axon.QueryResultSumByChampName;
import com.ns.resultquery.adapter.axon.QueryResultSumByUserName;
import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import com.ns.resultquery.application.port.out.FindStatisticsPort;
import lombok.RequiredArgsConstructor;
import org.axonframework.queryhandling.QueryGateway;

@PersistanceAdapter
@RequiredArgsConstructor
public class FindStatisticsAdapter implements FindStatisticsPort {
    private final QueryGateway queryGateway;


    @Override
    public CountSumByChamp queryToResultSumByChampName(String champName) {
        return queryGateway.query(new QueryResultSumByChampName(champName), CountSumByChamp.class).join();
    }

    @Override
    public CountSumByMembership queryToResultByUserName(String champName) {
        return queryGateway.query(new QueryResultSumByUserName(champName), CountSumByMembership.class).join();
    }
}
