package com.ns.common;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimelineData {
    private int time; // minute
    private int blueGold;
    private int redGold;
}
