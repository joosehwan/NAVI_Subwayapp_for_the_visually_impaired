package org.tensorflow.navi.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.tensorflow.navi.R;
import org.tensorflow.navi.network.RetrofitClient;

import org.tensorflow.navi.data.JoinData;
import org.tensorflow.navi.data.JoinResponse;
import org.tensorflow.navi.network.ServiceApi;
import org.tensorflow.navi.vision_module.Voice;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class JoinActivity extends Activity {
    private EditText mIdView;
    private EditText mPasswordView;
    private Voice voice;
    private Button mJoinButton;
    private EditText mPasswordcheck;
    private ServiceApi service;
    private Button mReturnButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        //voice 객체
        voice = new Voice(this, null);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);
        voice.TTS("회원가입을 시작합니다. 이름과 비밀번호, 그리고 비밀번호를 다시 입력하세요");

        mReturnButton = findViewById(R.id.join_cancel);
        mIdView = findViewById(R.id.join_id);
        mPasswordView = findViewById(R.id.join_password);
        mPasswordcheck = findViewById(R.id.join_pwck);
//        mIdView = (EditText) findViewById(R.id.join_id);
        mJoinButton = findViewById(R.id.join_button);
//        mProgressView = (ProgressBar) findViewById(R.id.join_progress);

        service = RetrofitClient.getClient().create(ServiceApi.class);

        mJoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptJoin();
            }
        });
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                voice.TTS("로그인 화면으로 돌아갑니다.");
                voice.close();
                finish();
            }
        });

    }

    private void attemptJoin() {
        mIdView.setError(null);
//        mIdView.setError(null);
        mPasswordView.setError(null);

//        String id = mIdView.getText().toString();
        String id = mIdView.getText().toString();
        String password = mPasswordView.getText().toString();
        String passwordcheck = mPasswordcheck.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // 패스워드의 유효성 검사
        if (password.isEmpty()) {
            mIdView.setError("비밀번호를 입력해주세요.");
            voice.TTS("비밀번호를 입력해주세요.");
            focusView = mIdView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError("비밀번호로 숫자 4자리를 입력해주세요.");
            voice.TTS("비밀번호로 숫자 4자리를 입력해주세요");
            focusView = mPasswordView;
            cancel = true;
        }

        // 아이디의 유효성 검사
        else if (id.isEmpty()) {
            mIdView.setError("아이디를 입력해주세요.");
            voice.TTS("아이디를 입력해주세요.");
            focusView = mIdView;
            cancel = true;
        } else if (password.equals(passwordcheck) != true) {//비밀번호 재확인이 일치하는지
            mPasswordcheck.setError("비밀번호를 재확인 해주세요.");
            voice.TTS("비밀번호 재확인 해주세요.");
            focusView = mPasswordView;
            cancel = true;
        }


        if (cancel) {
            focusView.requestFocus();
        } else {
            startJoin(new JoinData(id, password));
        }
    }

    public void startJoin(JoinData data) {
        service.userJoin(data).enqueue(new Callback<JoinResponse>() {
            @Override
            public void onResponse(Call<JoinResponse> call, Response<JoinResponse> response) {
                JoinResponse result = response.body();
                if (response.isSuccessful()){
                    String result_body = new Gson().toJson(response.body());
                    System.out.println(result_body + "==== result-body ");
                }
            }

            @Override
            public void onFailure(Call<JoinResponse> call, Throwable throwable) {
                Toast.makeText(JoinActivity.this,"로그인에러",Toast.LENGTH_SHORT);

            }
        });
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 4;
    }
}
