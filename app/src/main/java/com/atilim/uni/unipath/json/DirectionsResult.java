package com.atilim.uni.unipath.json;

import com.google.api.client.util.Key;

import java.util.List;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class DirectionsResult {
    @Key("routes")
    public List<Route> routes;
}
