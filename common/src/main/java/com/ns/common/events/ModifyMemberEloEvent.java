package com.ns.common.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModifyMemberEloEvent {
    private String aggregateIdentifier;
    private String membershipId;
    private Long elo;

}
