package com.ns.membership.axon.event;

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
