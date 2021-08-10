package org.tensorflow.demo.network;

import android.service.autofill.UserData;

import org.tensorflow.demo.data.JoinData;
import org.tensorflow.demo.data.JoinResponse;
import org.tensorflow.demo.data.LoginData;
import org.tensorflow.demo.data.LoginResponse;
import org.tensorflow.demo.data.TrainNum;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ServiceApi {
    @POST("/users/")
    Call<LoginResponse> userLogin(@Body LoginData data);

    @POST("/users/")
    Call<JoinResponse> userJoin(@Body JoinData data);

    @POST("/train/")
    Call<TrainNum> post_trainnum(@Body TrainNum post);

    @GET("/train/")
    Call<List<TrainNum>> get_trainnum();


}