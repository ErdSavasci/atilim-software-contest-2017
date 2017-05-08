package com.atilim.uni.unipath.cons;

import com.atilim.uni.unipath.R;
import com.atilim.uni.unipath.customs.CustomLogcatTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import es.munix.logcat.LogcatTextView;

/**
 * Created by erd_9 on 9.04.2017.
 */

public class Globals {
    public static boolean isUserInUniversityArea = false;
    public static boolean openNavigationActivityOnce = true;
    public static final HashMap<String, Integer> routerMacFloorNumberMatches = new HashMap<>();
    public static CharSequence customLogcatTextViewLog = "";

    static {
        routerMacFloorNumberMatches.put("94:b4:0f:16:01:f0", -2);
        routerMacFloorNumberMatches.put("94:b4:0f:16:00:40", -2);
        routerMacFloorNumberMatches.put("94:b4:0f:16:02:90", -2);

        routerMacFloorNumberMatches.put("94:b4:0f:15:eb:70", -1);
        routerMacFloorNumberMatches.put("94:b4:0f:15:eb:d0", -1);
        routerMacFloorNumberMatches.put("94:b4:0f:15:b3:d0", -1);
        routerMacFloorNumberMatches.put("94:b4:0f:16:0e:20", -1);

        routerMacFloorNumberMatches.put("94:b4:0f:16:02:20", 0);
        routerMacFloorNumberMatches.put("94:b4:0f:16:02:30", 0);

        routerMacFloorNumberMatches.put("94:b4:0f:16:03:d0", 1);
        routerMacFloorNumberMatches.put("94:b4:0f:16:08:b0", 1);
        routerMacFloorNumberMatches.put("94:b4:0f:15:e8:90", 1);
        routerMacFloorNumberMatches.put("94:b4:0f:15:b6:20", 1);

        routerMacFloorNumberMatches.put("94:b4:0f:15:e8:80", 2);
        routerMacFloorNumberMatches.put("94:b4:0f:16:03:10", 2);
        routerMacFloorNumberMatches.put("94:b4:0f:15:be:f0", 2);

        routerMacFloorNumberMatches.put("00:24:6c:7c:de:d0", 3);
        routerMacFloorNumberMatches.put("94:b4:0f:16:00:70", 3);
        routerMacFloorNumberMatches.put("94:b4:0f:15:d4:70", 3);
        routerMacFloorNumberMatches.put("94:b4:0f:16:00:00", 3);

        routerMacFloorNumberMatches.put("94:b4:0f:16:0e:30", 4);
        routerMacFloorNumberMatches.put("94:b4:0f:16:0c:c0", 4);
        routerMacFloorNumberMatches.put("94:b4:0f:15:0d:f0", 4);

        routerMacFloorNumberMatches.put("00:22:33:e2:07:b2", 1); //(FOR TEST PURPOSES)
    }
}
