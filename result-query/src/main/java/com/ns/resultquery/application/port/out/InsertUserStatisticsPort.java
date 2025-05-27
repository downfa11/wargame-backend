package com.ns.resultquery.application.port.out;

import com.ns.resultquery.domain.dto.InsertResultCountDto;

public interface InsertUserStatisticsPort {
    void insertResultCountIncreaseEventByUserName(Long membershipId, String username, InsertResultCountDto insertResultCountDto);
}
