package com.ns.resultquery.application.port.out;

import com.ns.resultquery.adapter.axon.query.CountSumByChamp;
import com.ns.resultquery.adapter.axon.query.CountSumByMembership;
import reactor.core.publisher.Mono;

public interface FindStatisticsPort {

    CountSumByChamp queryToResultSumByChampName(String champName);
    CountSumByMembership queryToResultByUserName(String champName);
}
