package com.ns.resultquery.application.port.out;

public interface InsertChampStatisticsPort {
    void insertResultCountIncreaseEventByChampName(Long champIndex, String champName, Long resultCount, Long winCount, Long loseCount);

}
