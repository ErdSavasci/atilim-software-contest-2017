package com.atilim.uni.unipath.utils;

/**
 * Created by erd_9 on 12.04.2017.
 */

public class AccessPointInfo {
    private String macAddress;
    private String bssid;
    private int strength;

    public AccessPointInfo(String macAddress, String bssid, int strength){
        this.macAddress = macAddress;
        this.bssid = bssid;
        this.strength = strength;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }
}
