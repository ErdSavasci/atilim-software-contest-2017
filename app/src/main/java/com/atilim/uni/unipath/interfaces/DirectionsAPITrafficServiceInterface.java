package com.atilim.uni.unipath.interfaces;

import com.atilim.uni.unipath.json.DirectionsResult;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by erd_9 on 21.04.2017.
 */

public interface DirectionsAPITrafficServiceInterface {
    @GET("maps/api/directions/json?traffic_model=best_guess&departure_time=now")
    Call<DirectionsResult> getDirections(@Query("origin") String latLong, @Query("destination") String dest, @Query("key") String key, @Query("alternatives") String alts);
}
