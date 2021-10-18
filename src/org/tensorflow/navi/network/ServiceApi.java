package org.tensorflow.navi.network;

import org.tensorflow.navi.data.JoinData;
import org.tensorflow.navi.data.JoinResponse;
import org.tensorflow.navi.data.LoginData;
import org.tensorflow.navi.data.LoginResponse;
import org.tensorflow.navi.data.OcrResponse;
import org.tensorflow.navi.data.Ocrdata;
import org.tensorflow.navi.data.SubwayData;
import org.tensorflow.navi.data.SubwayResponse;
import org.tensorflow.navi.data.TrainNum;
import org.tensorflow.navi.data.TransportData;
import org.tensorflow.navi.data.UserpositonData;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
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

    @Multipart
    @PUT("/ocrimg/")
    Call<OcrResponse> put_ocr_data(@Part MultipartBody.Part data);

    @GET("/ocrimg/")
    Call<List<Ocrdata>> get_ocr_data();

    @GET("/arrival/")
    Call<List<TrainNum>> get_trainnum();

    @GET("/naviroot/")
    Call<List<TransportData>> get_transport();

    @GET("/userstatus/")
    Call<List<UserpositonData>> get_userposition();


}
