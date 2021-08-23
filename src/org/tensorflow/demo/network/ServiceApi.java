package org.tensorflow.demo.network;

import org.tensorflow.demo.data.JoinData;
import org.tensorflow.demo.data.JoinResponse;
import org.tensorflow.demo.data.LoginData;
import org.tensorflow.demo.data.LoginResponse;
import org.tensorflow.demo.data.SubwayData;
import org.tensorflow.demo.data.SubwayResponse;
import org.tensorflow.demo.data.TrainNum;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ServiceApi {
    @POST("/users/")
    Call<LoginResponse> userLogin(@Body LoginData data);

    @POST("/users/")
    Call<JoinResponse> userJoin(@Body JoinData data);

    @POST("/destination/")
    Call<SubwayResponse> subwayPost(@Body SubwayData data);

    @GET ("/arrival/")
    Call<List<TrainNum>> get_trainnum();

}
