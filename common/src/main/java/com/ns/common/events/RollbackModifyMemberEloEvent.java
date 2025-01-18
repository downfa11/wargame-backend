package com.ns.common.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RollbackModifyMemberEloEvent {
    private String aggregateIdentifier;
    private Long previousElo;
}
