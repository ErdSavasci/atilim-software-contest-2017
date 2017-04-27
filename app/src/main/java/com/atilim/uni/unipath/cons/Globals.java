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
        routerMacFloorNumberMatches.put("94:b4:0f:15:eb:70", -1);
        routerMacFloorNumberMatches.put("94:b4:0f:15:eb:d0", -1);
        routerMacFloorNumberMatches.put("94:b4:0f:15:b3:d0", -1);
        routerMacFloorNumberMatches.put("94:b4:0f:16:02:20", -1);//0
        routerMacFloorNumberMatches.put("94:b4:0f:16:02:30", -1);//0
        routerMacFloorNumberMatches.put("94:b4:0f:16:01:f0", -2);
    }
}
