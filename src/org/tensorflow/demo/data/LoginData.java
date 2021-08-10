package org.tensorflow.demo.data;

import com.google.gson.annotations.SerializedName;

public class LoginData {
    @SerializedName("userid")
    String login_userid;

    @SerializedName("userpw")
    String login_userpw;

    public LoginData(String login_userid, String login_userpw) {
        this.login_userid = login_userid;
        this.login_userpw = login_userpw;
    }
}
