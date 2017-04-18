package com.atilim.uni.unipath.json;

import com.google.api.client.util.Key;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class Leg {
    @Key("distance")
    public LegDistance distance;

    @Key("duration")
    public LegDuration duration;
}
