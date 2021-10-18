package org.tensorflow.navi.data;

import com.google.gson.annotations.SerializedName;

public class SubwayResponse {
    public String getStartdet() {
        return startdet;
    }

    public void setStartdet(String startdet) {
        this.startdet = startdet;
    }

    @SerializedName("startdet")
    private String startdet;

    public SubwayResponse(String startdet) {
        this.startdet = startdet;
    }
}
