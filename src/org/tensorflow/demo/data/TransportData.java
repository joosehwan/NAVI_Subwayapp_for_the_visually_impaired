package org.tensorflow.demo.data;

import com.google.gson.annotations.SerializedName;

public class TransportData {
    @SerializedName("startline")
    String startline;

    public String getStartline() {
        return startline;
    }

    public void setStartline(String startline) {
        this.startline = startline;
    }

    public String getStartname() {
        return startname;
    }

    public void setStartname(String startname) {
        this.startname = startname;
    }

    public String getExchaline() {
        return exchaline;
    }

    public void setExchaline(String exchaline) {
        this.exchaline = exchaline;
    }

    public String getExchawname() {
        return exchawname;
    }

    public void setExchawname(String exchawname) {
        this.exchawname = exchawname;
    }

    @SerializedName("startwname")
    String startname;
    @SerializedName("exchaline")
    String exchaline;
    @SerializedName("exchawname")
    String exchawname;

//    startwname행 승차
}
