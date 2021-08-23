package org.tensorflow.demo.data;

import com.google.gson.annotations.SerializedName;

public class SubwayData {

    @SerializedName("startdet")
    String startdet;


    @SerializedName("enddet")
    String enddet;

    public SubwayData(String startdet, String enddet) {
        this.startdet = startdet;
        this.enddet =enddet;
    }

    public String getStartdet() {
        return startdet;
    }

    public void setStartdet(String startdet) {
        this.startdet = startdet;

    }

    public String getEnddet() {
        return enddet;
    }

    public void setEnddet(String enddet) {
        this.enddet = enddet;
    }

}
