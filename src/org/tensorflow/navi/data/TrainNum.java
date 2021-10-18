package org.tensorflow.navi.data;

import com.google.gson.annotations.SerializedName;

public class TrainNum {

    public String getTrainline() {
        return trainline;
    }

    public void setTrainline(String trainline) {
        this.trainline = trainline;
    }

    public int getArrivetime() {
        return arrivetime;
    }

    public void setArrivetime(int arrivetime) {
        this.arrivetime = arrivetime;
    }

    public int getTraino() {
        return traino;
    }

    public void setTraino(int traino) {
        this.traino = traino;
    }

    public String getStation() {
        return station;
    }

    public void setStation(String station) {
        this.station = station;
    }

    @SerializedName("trainline")
    private String trainline;
    @SerializedName("arrivetime")
    private int arrivetime;
    @SerializedName("traino")
    private int traino;
    @SerializedName("station")
    private String station;



}
