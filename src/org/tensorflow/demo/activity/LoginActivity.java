package org.tensorflow.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.google.gson.Gson;

import org.tensorflow.demo.DetectorActivity;
import org.tensorflow.demo.R;
import org.tensorflow.demo.TensorFlowYoloDetector;
import org.tensorflow.demo.blescan.MainActivity;
import org.tensorflow.demo.data.LoginData;
import org.tensorflow.demo.data.LoginResponse;
import org.tensorflow.demo.network.RetrofitClient;
import org.tensorflow.demo.network.ServiceApi;
import org.tensorflow.demo.vision_module.Voice;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;


public class LoginActivity extends Activity {
    private EditText mIdView;
    private EditText mPasswordView;
    private Button mnameLoginButton;
    private Button mJoinButton;
    private Voice voice;
    private Button btButton;
    private ServiceApi service;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mIdView = findViewById(R.id.login_id);
        mPasswordView = findViewById(R.id.login_password);
        mnameLoginButton = findViewById(R.id.login_gotoLogin);
        mJoinButton = findViewById(R.id.login_gotoJoin);
        btButton = findViewById(R.id.bt);
        //voice 객체
        voice = new Voice(this, null);
        service = RetrofitClient.getClient().create(ServiceApi.class);

        voice.TTS("시각장애인을 위한 지하철어플. 나비에 오신것을 환영합니다. 로그인을 진행해주세요");
        mnameLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                startActivity(intent);
                attemptLogin();
            }
        });
        mJoinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), JoinActivity.class);
                startActivity(intent);
                voice.TTS("회원가입으로 이동합니다.");
                voice.close();
                finish();
            }
        });
        btButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void attemptLogin() {
        mIdView.setError(null);
        mPasswordView.setError(null);

        String id = mIdView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // 패스워드의 유효성 검사
        if (password.isEmpty()) {
            mIdView.setError("비밀번호를 입력해주세요.");
            voice.TTS("비밀번호를 입력해주세요");
            focusView = mIdView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError("4자 이상의 비밀번호를 입력해주세요.");

            voice.TTS("4자 이상의 비밀번호를 입력해주세요.");
            focusView = mPasswordView;
            cancel = true;
        }

        // 아이디의 유효성 검사
        if (id.isEmpty()) {
            mIdView.setError("아이디를 입력해주세요.");
            voice.TTS("아이디를 입력해주세요.");
            focusView = mIdView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            startLogin(new LoginData(id, password));
        }
    }

    public void startLogin(LoginData data) {
        service.userLogin(data).enqueue(new Callback<LoginResponse>() {
            @Override

            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
//
//                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
//                startActivity(intent);
                if (response.isSuccessful()) {
                    LoginResponse result = response.body();

                    String result_body = new Gson().toJson(response.body());
                    System.out.println(result_body + "==== result-body ");

                    Toast.makeText(LoginActivity.this, "message :" + result.getMessage() + " 와 status : " + result.getstatus(), Toast.LENGTH_SHORT).show();

                    System.out.println("getstatus=" + result.getstatus());
                    Log.d(TAG, "response.body : " + response.body());
                    System.out.println("전체바디=" + new Gson().toJson(response.body()));



                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                Toast.makeText(LoginActivity.this, "로그인 에러 발생", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Fail msg : " + throwable.getMessage());
            }

        });

    }


    private boolean isPasswordValid(String password) {
        return password.length() >= 4;
    }


}

