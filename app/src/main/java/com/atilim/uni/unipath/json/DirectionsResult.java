package com.atilim.uni.unipath.json;

import com.google.api.client.util.Key;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class DirectionsResult {
    @SerializedName("routes")
    @Expose
    public List<Route> routes;
}
