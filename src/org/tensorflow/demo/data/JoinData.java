package org.tensorflow.demo.data;


import com.google.gson.annotations.SerializedName;

public class JoinData {
    @SerializedName("userid")
     String userid;
    @SerializedName("userpw")
     String userpw;

    public JoinData(String userid, String userpw) {
        this.userid = userid;
        this.userpw = userpw;
    }


    public void setuserid(String s) {
        userid = s;
    }

    public void setuserpw(String s) {
        userpw = s;
    }

}
