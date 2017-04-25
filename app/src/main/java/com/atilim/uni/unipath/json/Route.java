package com.atilim.uni.unipath.json;

import com.google.api.client.util.Key;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class Route {
    @SerializedName("overview_polyline")
    @Expose
    public OverviewPolyLine overviewPolyLine;

    @SerializedName("legs")
    @Expose
    public List<Leg> legs;
}
