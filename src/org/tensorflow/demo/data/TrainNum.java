package org.tensorflow.demo.data;

import com.google.gson.annotations.SerializedName;

public class TrainNum {
    private String nowtime;
    private String srcstation;
    private String trainlocation;
    private String trainnum;

    public String getTrainlocation() {
        return trainlocation;
    }

    public void setTrainlocation(String trainlocation) {
        this.trainlocation = trainlocation;
    }

    public String getTrainnum() {
        return trainnum;
    }

    public void setTrainnum(String trainnum) {
        this.trainnum = trainnum;
    }



    public String getNowtime() {
        return nowtime;
    }

    public void setNowtime(String nowtime) {
        this.nowtime = nowtime;
    }

    public String getSrcstation() {
        return srcstation;
    }

    public void setSrcstation(String srcstation) {
        this.srcstation = srcstation;
    }



}
