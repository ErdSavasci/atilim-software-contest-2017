package com.atilim.uni.unipath.json;

import com.google.api.client.util.Key;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class OverviewPolyLine {
    @SerializedName("points")
    @Expose
    public String points;
}
