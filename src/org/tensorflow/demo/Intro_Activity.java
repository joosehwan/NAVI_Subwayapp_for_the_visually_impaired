package org.tensorflow.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import org.tensorflow.demo.activity.LoginActivity;
import org.tensorflow.demo.vision_module.Voice;

public class Intro_Activity extends Activity {

    Voice voice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //voice 객체
        voice = new Voice(this, null);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {

                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                voice.TTS("시각장애인을 위한 지하철어플. 나비에 오신것을 환영합니다. 로그인을 진행해주세요");
                startActivity(intent);
                finish();
            }

        },3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
