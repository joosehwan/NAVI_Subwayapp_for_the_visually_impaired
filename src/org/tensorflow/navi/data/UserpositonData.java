package org.tensorflow.navi.data;

import com.google.gson.annotations.SerializedName;

public class UserpositonData {
    public Integer getusertrain() {
        return usertrain;
    }

    public void setusertrain(Integer usertrain) {
        this.usertrain = usertrain;
    }

    public String getusersta() {
        return usersta;
    }

    public void setusersta(String usersta) {
        this.usersta = usersta;
    }

    @SerializedName("usertrain")
    Integer usertrain;
    @SerializedName("usersta")
    String usersta;
}
