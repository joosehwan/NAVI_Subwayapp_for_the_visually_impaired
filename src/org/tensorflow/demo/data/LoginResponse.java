package org.tensorflow.demo.data;

import com.google.gson.annotations.SerializedName;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSource;

public class LoginResponse extends ResponseBody {
    @SerializedName("status")
    private int status;

    @SerializedName("message")
    private String message;

    public String getCode() {
        return code;
    }

    @SerializedName("code")
    private String code;
//    @SerializedName("userid")
//    private String userId;

    public int getstatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public MediaType contentType() {
        return null;
    }

    @Override
    public long contentLength() {
        return 0;
    }

    @Override
    public BufferedSource source() {
        return null;
    }


//    public String getUserId() {
//        return userId;
//    }
}
