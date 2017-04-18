package com.atilim.uni.unipath.json;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class Route {
    @Key("overview_polyline")
    public OverviewPolyLine overviewPolyLine;

    @Key("legs")
    public List<Leg> legs;
}
