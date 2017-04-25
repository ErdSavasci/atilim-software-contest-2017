package com.atilim.uni.unipath.cons;

import com.atilim.uni.unipath.customs.CustomLogcatTextView;

import java.util.HashMap;

import es.munix.logcat.LogcatTextView;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class Globals {
    public static boolean isUserInUniversityArea = false;
    public static HashMap<String, Integer> routerMacFloorNumberMatches = new HashMap<>();
    public static CharSequence customLogcatTextViewLog = "";

    static {
        routerMacFloorNumberMatches.put("94-b4-0f-15-eb-70", -1);
        routerMacFloorNumberMatches.put("94-b4-0f-15-eb-d0", -1);
        routerMacFloorNumberMatches.put("94-b4-0f-15-b3-d0", -1);
    }
}
