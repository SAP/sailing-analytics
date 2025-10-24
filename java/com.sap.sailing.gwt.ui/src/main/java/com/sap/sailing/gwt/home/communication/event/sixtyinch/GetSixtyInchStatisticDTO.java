package com.sap.sailing.gwt.home.communication.event.sixtyinch;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.gwt.dispatch.shared.commands.Result;
import com.sap.sse.gwt.shared.DTO;

public class GetSixtyInchStatisticDTO implements DTO, Result {
    private int competitors;
    private int legs;
    private Duration duration;
    private Distance distance;

    public GetSixtyInchStatisticDTO() {
    }

    public GetSixtyInchStatisticDTO(int competitors, int legs, Duration duration, Distance distance) {
        this.competitors = competitors;
        this.legs = legs;
        this.duration = duration;
        this.distance = distance;
    }

    public int getCompetitors() {
        return competitors;
    }

    public int getLegs() {
        return legs;
    }

    public Duration getDuration() {
        return duration;
    }

    public Distance getDistance() {
        return distance;
    }
}
