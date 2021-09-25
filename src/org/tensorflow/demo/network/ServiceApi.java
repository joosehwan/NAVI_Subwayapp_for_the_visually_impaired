package org.tensorflow.demo.network;

import org.tensorflow.demo.data.JoinData;
import org.tensorflow.demo.data.JoinResponse;
import org.tensorflow.demo.data.LoginData;
import org.tensorflow.demo.data.LoginResponse;
import org.tensorflow.demo.data.OcrResponse;
import org.tensorflow.demo.data.Ocrdata;
import org.tensorflow.demo.data.SubwayData;
import org.tensorflow.demo.data.SubwayResponse;
import org.tensorflow.demo.data.TrainNum;
import org.tensorflow.demo.data.TransportData;
import org.tensorflow.demo.data.UserpositonData;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ServiceApi {
    @POST("/users/")
    Call<LoginResponse> userLogin(@Body LoginData data);

    @POST("/users/")
    Call<JoinResponse> userJoin(@Body JoinData data);

    @POST("/destination/")
    Call<SubwayResponse> subwayPost(@Body SubwayData data);

    @Multipart
    @POST("/ocrimg/")
    Call<OcrResponse> ocr_data(@Part MultipartBody.Part data);

    @GET("/arrival/")
    Call<List<TrainNum>> get_trainnum();

    @GET("/naviroot/")
    Call<List<TransportData>> get_transport();

    @GET("/userstatus/")
    Call<List<UserpositonData>> get_userposition();


}
