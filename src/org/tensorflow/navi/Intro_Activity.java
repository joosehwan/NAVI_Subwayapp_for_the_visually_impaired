package org.tensorflow.navi;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import org.tensorflow.navi.activity.LoginActivity;
import org.tensorflow.navi.vision_module.Voice;

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

                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                voice.TTS("시각장애인을 위한 지하철어플. 나비에 오신것을 환영합니다");

                startActivity(intent);
                finish();
            }

        },3500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
