package com.ns.player.adapter.axon.command;

public record MembershipElo(String membershipId, String aggregateIdentifier, Long oldElo, Long curElo) {
}
