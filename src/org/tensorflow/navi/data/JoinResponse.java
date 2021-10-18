package org.tensorflow.navi.data;

import com.google.gson.annotations.SerializedName;

public class JoinResponse {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
