package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tensorflow.demo.network.RetrofitClient;
import org.tensorflow.demo.R;
import org.tensorflow.demo.data.JoinData;
import org.tensorflow.demo.data.JoinResponse;
import org.tensorflow.demo.network.ServiceApi;
import org.tensorflow.demo.vision_module.Voice;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

public class Subpage extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera3);

        Button back = findViewById(R.id.backto_navi);
        Button cancel =findViewById(R.id.navi_cancel);
        TextView tv1 = findViewById(R.id.station_start);
        Button emCall = findViewById(R.id.emergency_call);
        TextView tv2 = findViewById(R.id.station_destination);
        Intent get_intent = getIntent();
        final String Src_station;
        Src_station = get_intent.getStringExtra("src");
        final String Dst_station;
        Dst_station = get_intent.getStringExtra("dst");
        System.out.println("src : "+Src_station);
        System.out.println("dst : "+Dst_station);
        Log.d("", "dst_station =" + Dst_station);
        try {
            tv1.setText(Src_station);
            tv2.setText(Dst_station);
            setResult(Activity.RESULT_OK, get_intent);
        } catch (Exception e) {
            tv1.setText("출발역 : 정보없음");
            tv2.setText("도착역 : 정보없음");

        }


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                intent.putExtra("Src", Src_station);
                intent.putExtra("Dst", Dst_station);
                startActivity(intent);
                finish();
            }
        });
        emCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    /**
                     *  사용자 단말기의 권한 중 전화걸기 권한이 허용되어 있는지 체크한다.
                     */
                    int permissionResult = checkSelfPermission(Manifest.permission.CALL_PHONE);

                    // call_phong의 권한이 없을 떄
                    if (permissionResult == PackageManager.PERMISSION_DENIED) {
                        //  Package는 Android Application의 ID이다.
                        /**
                         *  사용자가 CALL_PHONE 권한을 한번이라도 거부한 적이 있는지 조사한다.
                         *  거부한 이력이 한번이라도 있다면, true를 리턴한다.
                         *  거부한 이력이 없다면 false를 리턴한다.
                         */
                        if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {

                            AlertDialog.Builder dialog = new AlertDialog.Builder(Subpage.this);
                            dialog.setTitle("권한이 필요합니다.")
                                    .setMessage("이 기능을 사용하기 위해서는 단말기의 \"전화걸기\"권한이 필요합니다. 계속하시겠습니까?")
                                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // 위 리스너랑 다른 범위여서 마쉬멜로우인지 또 체크해주어야 한다.
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1000);
                                            }
                                        }
                                    })
                                    .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(Subpage.this, "기능을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .create()
                                    .show();

                        }
                        // 최초로 권한을 요청 할 때
                        else {
                            // CALL_PHONE 권한을 안드로이드 OS에 요청합니다.
                            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1000);
                        }
                    }
                    // call_phonne의 권한이 있을 떄
                    else {

                    }

                }

                Intent intent_call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:01052662394"));
                startActivity(intent_call);
            }
        });


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 사용자 요청, 요청한 권한들, 응답들

        if (requestCode == 1000) {
            // 요청한 권한을 사용자가 허용했다면
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:010-1111-2222"));
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    startActivity(intent);
                }
            }
            else {
                Toast.makeText(Subpage.this, "권한요청을 거부했습니다.", Toast.LENGTH_SHORT).show();
            }
        }



    }
}
