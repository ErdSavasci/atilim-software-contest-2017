package com.atilim.uni.unipath.converters;

/**
 * Created by erd_9 on 16.04.2017.
 */

public class DMS {
    private int latDegrees;
    private int lonDegrees;

    private int latMinutes;
    private int lonMinutes;

    private int latSeconds;
    private int lonSeconds;

    private double latitude;
    private double longitude;

    private double elevationHeight;

    public DMS(double lat, double lon){
        this.latitude = lat;
        this.longitude = lon;
    }

    private DMS(int latDegrees, int lonDegrees, int latMinutes, int lonMinutes, int latSeconds, int lonSeconds){
        this.latDegrees = latDegrees;
        this.lonDegrees = lonDegrees;
        this.latMinutes = latMinutes;
        this.lonMinutes = lonMinutes;
        this.latSeconds = latSeconds;
        this.lonSeconds = lonSeconds;
    }

    public DMS convertIntoDMS(){
        latDegrees = (int) latitude;
        lonDegrees = (int) longitude;

        double latM, lonM;

        latM = (latitude - latDegrees) * 60;
        lonM = (longitude - lonDegrees) * 60;

        latMinutes = (int) latM;
        lonMinutes = (int) lonM;

        latSeconds = (int) ((latM - latMinutes) * 60);
        lonSeconds = (int) ((lonM - lonMinutes) * 60);

        return new DMS(latDegrees, lonDegrees, latMinutes, lonMinutes, latSeconds, lonSeconds);
    }

    public int getLatDegrees() {
        return latDegrees;
    }

    public void setLatDegrees(int latDegrees) {
        this.latDegrees = latDegrees;
    }

    public int getLonDegrees() {
        return lonDegrees;
    }

    public void setLonDegrees(int lonDegrees) {
        this.lonDegrees = lonDegrees;
    }

    public int getLatMinutes() {
        return latMinutes;
    }

    public void setLatMinutes(int latMinutes) {
        this.latMinutes = latMinutes;
    }

    public int getLonMinutes() {
        return lonMinutes;
    }

    public void setLonMinutes(int lonMinutes) {
        this.lonMinutes = lonMinutes;
    }

    public int getLatSeconds() {
        return latSeconds;
    }

    public void setLatSeconds(int latSeconds) {
        this.latSeconds = latSeconds;
    }

    public int getLonSeconds() {
        return lonSeconds;
    }

    public void setLonSeconds(int lonSeconds) {
        this.lonSeconds = lonSeconds;
    }

    public double getElevationHeight() {
        return elevationHeight;
    }

    public void setElevationHeight(double elevationHeight) {
        this.elevationHeight = elevationHeight;
    }
}
