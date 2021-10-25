package org.tensorflow.navi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.minew.beacon.BeaconValueIndex;
import com.minew.beacon.BluetoothState;
import com.minew.beacon.MinewBeacon;
import com.minew.beacon.MinewBeaconManager;
import com.minew.beacon.MinewBeaconManagerListener;

import org.tensorflow.navi.blescan.BeaconListAdapter;
import org.tensorflow.navi.blescan.UserRssi;
import org.tensorflow.navi.data.Subwayapi;
import org.tensorflow.navi.data.UserpositonData;
import org.tensorflow.navi.network.RetrofitClient;
import org.tensorflow.navi.network.ServiceApi;
import org.tensorflow.navi.vision_module.MyCallback;
import org.tensorflow.navi.vision_module.Service;
import org.tensorflow.navi.vision_module.Voice;
import org.tensorflow.navi.vision_module.senario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RequiresApi(api = Build.VERSION_CODES.N)
public class Subpage extends Activity {
    private Voice voice;
    DetectorActivity detectorActivity = new DetectorActivity();
    public ServiceApi serviceApi;
    public Service service;
    public Subwayapi subwayapi;
    //request_userposition의 결과를 저장해주는 변수-----------------------------------------------------
    static String userPosition_info = "";
    UserRssi comp = new UserRssi();
    private MinewBeaconManager mMinewBeaconManager;
    private BeaconListAdapter mAdapter;
    private static final int REQUEST_ENABLE_BT = 2;
    // 비콘 출력 배열리스트
    ArrayList<String> uuuid = new ArrayList<String>();
    ArrayList<String> rsssi = new ArrayList<String>();
    ArrayList<String> ttx_power = new ArrayList<String>();
    ArrayList<Double> values = new ArrayList<Double>();
    ArrayList<String> BeaconName = new ArrayList<String>();
    ArrayList<String> nBeacon = new ArrayList<String>();
    // 클론할 배열리스트
    ArrayList<String> uuuid_clone = new ArrayList<String>();
    ArrayList<String> rsssi_clone = new ArrayList<String>();
    ArrayList<String> ttx_power_clone = new ArrayList<String>();
    ArrayList<Double> values_clone = new ArrayList<Double>();
    ArrayList<String> BeaconName_clone = new ArrayList<String>();
    private int state;
    //음성인식에 띠링 소리를 삽입하기위한 설정변수
    SoundPool soundPool;
    int soundID;
    boolean is_station_perfect = false;
    private List<MinewBeacon> mMinewBeacons = new ArrayList<>();


    public static String getUserPosition_info() {
        return userPosition_info;
    }

    public static void setUserPosition_info(String userPosition_info) {
        Subpage.userPosition_info = userPosition_info;
    }

    protected void onCreate(Bundle savedInstanceState) {
        serviceApi = RetrofitClient.getClient().create(ServiceApi.class);

        voice = new Voice(this, null);
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        soundID = soundPool.load(this, R.raw.voice_effect, 1);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera3);
        Button back = findViewById(R.id.backto_navi);
        Button readinfo = findViewById(R.id.readinfo);
        Button btn_userpositon = findViewById(R.id.get_userposition);
        TextView tv1 = findViewById(R.id.station_start);
        Button emCall = findViewById(R.id.emergency_call);
        TextView tv2 = findViewById(R.id.station_destination);
        TextView tv3 = findViewById(R.id.maininfo);
        TextView tv4 = findViewById(R.id.arrivalinfo);
        final TextView tv5 = findViewById(R.id.userpositionInfo);


        Intent get_intent = getIntent();
        final String Src_station;
        Src_station = get_intent.getStringExtra("src");
        final String Dst_station;
        Dst_station = get_intent.getStringExtra("dst");
        final String transfer_info;
        transfer_info = get_intent.getStringExtra("transfer");
        final String arrival;
        arrival = get_intent.getStringExtra("arrivalinfo");

        //비콘세팅
        try {
            mMinewBeaconManager.startScan();
        } catch (NullPointerException e) {
            voice.TTS("블루투스를 켜주세요");
            System.out.println("블루투스를 켜주세요");
            e.printStackTrace();
        }

        initView();
        initManager();
        checkBluetooth();
        initListener();

//        Toast.makeText(this, transfer_info, Toast.LENGTH_SHORT).show();

        readinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tr_Info = "", arr_Info = "", up_Info = "";

                try {
                    tr_Info = transfer_info;
                    arr_Info = arrival;
                    up_Info = userPosition_info;
                    if (transfer_info.isEmpty() == true && arrival.isEmpty() == true && userPosition_info.isEmpty() == true) {
                        voice.TTS("현재정보가 없습니다. 경로설정이나 열차를 조회하십시오");
                    } else {
                        if (tr_Info.isEmpty()) {
                            tr_Info = " 없음. \n";
                        }
                        if (arr_Info.isEmpty()) {
                            arr_Info = " 없음. \n";
                        }
                        if (up_Info.isEmpty()) {
                            up_Info = " 없음. \n";
                        }
                        voice.TTS("환승정보.\n" + tr_Info + "열차도착정보.\n" + arr_Info + "실시간열차정보.\n" + up_Info);
                    }

                } catch (NullPointerException e) {
                    e.getMessage();
                    e.getCause();
                }

            }
        });


        try {
            tv3.setText(transfer_info);
            tv4.setText(arrival);
            tv5.setText(userPosition_info);
            setResult(Activity.RESULT_OK, get_intent);
            if (Src_station == null) {
                tv1.setText("출발역 : 정보없음");
            }
            if (Dst_station == null) {
                tv2.setText("도착역 : 정보없음");
            }
            if (transfer_info.isEmpty() == true) {
                tv3.setText("환승역 : 정보없음");
            }
            if (arrival.isEmpty() == true) {
                tv4.setText("열차도착 : 정보없음");
            }
            if (userPosition_info.isEmpty() == true) {
                tv5.setText("실시간 열차정보 : 없음");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());

        }


        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DetectorActivity.class);
                intent.putExtra("Src", Src_station);
                intent.putExtra("Dst", Dst_station);
                intent.putExtra("transfer", transfer_info);

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
                // Service
                service = new Service();

                initService(initCompletedStatus, new MyCallback() {
                    @Override
                    public void callback() {
                        Log.e("n", "Navigate 시작");
                    }

                    @Override
                    public void callbackBundle(Bundle result) {
                    }
                });


            }
        });

        btn_userpositon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request_getUserposition();
//                tv5.setText(userPosition_info);
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
            } else {
                Toast.makeText(Subpage.this, "권한요청을 거부했습니다.", Toast.LENGTH_SHORT).show();
            }
        }


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            request_getUserposition();

        }
        return super.onKeyDown(keyCode, event);

    }

    // userposition 을 받기위한 리스트 선언
    ArrayList<Integer> usertrain = new ArrayList<>();
    ArrayList<String> usersta = new ArrayList<>();
    ArrayList<Integer> usertrain_clone = new ArrayList<>();
    ArrayList<String> usersta_clone = new ArrayList<>();

    public void request_getUserposition() {
        final TextView tv5 = findViewById(R.id.userpositionInfo);
        Call<List<UserpositonData>> getCall = serviceApi.get_userposition();
        getCall.enqueue(new Callback<List<UserpositonData>>() {
            @Override
            public void onResponse(Call<List<UserpositonData>> call, Response<List<UserpositonData>> response) {
                if (response.isSuccessful()) {
                    List<UserpositonData> userpositonData = response.body();
                    String result = "";
                    for (UserpositonData item : userpositonData) {
                        result += "실시간 열차 번호 : " + item.getusertrain()
                                + "\n실시간 열차위치(역) :" + item.getusersta();

                        usertrain.add(item.getusertrain());
                        usersta.add(item.getusersta());
                    }
                    System.out.println(result);
                    usertrain_clone = (ArrayList<Integer>) usertrain.clone();
                    usersta_clone = (ArrayList<String>) usersta.clone();
                    usertrain.clear();
                    usersta.clear();

                    int size = usertrain_clone.size();
                    String userposition_info = "";
                    for (int i = 0; i < size; i++) {
                        userposition_info += "열차 번호 : " + usertrain_clone.get(i) + "\n"
                                + "현재 위치는 [ " + usersta_clone.get(i) + " ] 입니다.";
//
                    }
                    System.out.println(userposition_info);
                    setUserPosition_info(userposition_info);
                    tv5.setText(userposition_info);

                    voice.TTS(userposition_info);
                }
            }

            @Override
            public void onFailure(Call<List<UserpositonData>> call, Throwable throwable) {
                voice.TTS("실시간 열차정보를 받을 수 없습니다. ");
            }
        });

    }

    public void checkBluetooth() {
        BluetoothState bluetoothState = mMinewBeaconManager.checkBluetoothState();
        switch (bluetoothState) {
            case BluetoothStateNotSupported:
                finish();
                break;
            case BluetoothStatePowerOff:
                showBLEDialog();
                break;
            case BluetoothStatePowerOn:
                break;
        }
    }

    private void initView() {

        mAdapter = new BeaconListAdapter();
    }

    private void initManager() {
        mMinewBeaconManager = MinewBeaconManager.getInstance(this);
    }

    public void initListener() {
        if (mMinewBeaconManager != null) {
            BluetoothState bluetoothState = mMinewBeaconManager.checkBluetoothState();
            switch (bluetoothState) {
                case BluetoothStateNotSupported:
                    finish();
                    break;
                case BluetoothStatePowerOff:
                    showBLEDialog();
                    return;
                case BluetoothStatePowerOn:
                    break;
            }
        }

        try {
            mMinewBeaconManager.startScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mMinewBeaconManager.setDeviceManagerDelegateListener(new MinewBeaconManagerListener() {

            @Override
            public void onAppearBeacons(List<MinewBeacon> minewBeacons) {
//
            }

            @Override
            public void onDisappearBeacons(List<MinewBeacon> minewBeacons) {
                //
            }

            @Override
            public void onRangeBeacons(final List<MinewBeacon> minewBeacons) {

                final ArrayList<String> stacompare = new ArrayList<String>();
                stacompare.add("74278BDA-B644-4520-8F0C-720EAF059935");
                stacompare.add("AB8190D5-D11E-4941-ACC4-42F30510B408");
                stacompare.add("FDA50693-A4E2-4FB1-AFCF-C6EB07647825");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Collections.sort(minewBeacons, comp);
                        Log.e("tag", state + "");
                        if (state == 1 || state == 2) {
                        } else {
                            mAdapter.setItems(minewBeacons);
                        }
                        for (MinewBeacon minewBeacon : minewBeacons) {
                            String deviceName = minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue();
//                            Toast.makeText(getApplicationContext(), deviceName + "  on range", Toast.LENGTH_SHORT).show();
                            System.out.println("on range UUID : " + minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue());
                            System.out.println("on range RSSI : " + minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_RSSI).getStringValue());
                            double d = ((-50 - (double) minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_RSSI).getIntValue()) / 20.0);
                            d = Math.pow(10.0, d);
//                          d=Math.round(d*100)/100;
//
                            if (minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue().contains("FDA50693") == true) {
                                BeaconName.add("5번출구");
                            } else if (minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue().contains("AB8190D5") == true) {
                                BeaconName.add("7번 출구");
                            } else if (minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue().contains("74278BDA") == true) {
                                BeaconName.add("엘레베이터");
                            }

                            values.add(d);

                            System.out.println("d의값=" + d);
                            System.out.println(mAdapter.getItemCount());
                            if (stacompare.contains(minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue()) == true) {
                                uuuid.add(minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue());
                                rsssi.add(minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_RSSI).getStringValue());
                                ttx_power.add(minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_TxPower).getStringValue());
                            }

                            if (mAdapter.getItemCount() == values.size()) {
                                uuuid_clone = (ArrayList<String>) uuuid.clone();
                                values_clone = (ArrayList<Double>) values.clone();
                                BeaconName_clone = (ArrayList<String>) BeaconName.clone();
                                values.clear();
                                uuuid.clear();
                                BeaconName.clear();

                                if (d <= 10.0) {


                                    Collections.sort(BeaconName_clone);

                                    String past = "";
                                    String current = "";

                                    for (String value : BeaconName_clone) {
                                        past += value;
                                    }
                                    System.out.println("past = " + past);
                                    for (String value : nBeacon) {
                                        current += value;
                                    }

                                    System.out.println("current = " + current);
                                    if (past.equals(current)) {
                                        System.out.println("비콘 변화 X");
                                    } else if (current.isEmpty() == true) {
                                        voice.TTS("주변에 " + past + "가 있습니다");
                                    } else {
                                        voice.TTS("주변에 " + current + "가 있습니다");
                                    }


                                    nBeacon = (ArrayList<String>) BeaconName_clone.clone();

                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onUpdateState(BluetoothState bluetoothState) {
                bluetoothState = mMinewBeaconManager.checkBluetoothState();
                switch (bluetoothState) {
                    case BluetoothStatePowerOn:
                        Toast.makeText(getApplicationContext(), "BluetoothStatePowerOn", Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothStatePowerOff:
                        Toast.makeText(getApplicationContext(), "BluetoothStatePowerOff", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                break;
        }
    }

    private void showBLEDialog() {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    public RecognitionListener getRecognitionListner(final MyCallback myCallback) {

        return new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                soundPool.play(soundID, 1f, 1f, 0, 0, 2.0f);
                Toast.makeText(getApplicationContext(), "음성인식을 시작합니다.", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                voice.TTS("음성 에러. 다시 눌러주세요");
                String message;

                switch (i) {

                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "오디오 에러";
                        break;

                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "클라이언트 에러";
                        break;

                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "퍼미션없음";
                        break;

                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "네트워크 에러";
                        break;

                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "네트웍 타임아웃";
                        break;

                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "찾을수 없음";
                        ;
                        break;

                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "바쁘대";
                        break;

                    case SpeechRecognizer.ERROR_SERVER:
                        message = "서버이상";
                        ;
                        break;

                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "말하는 시간초과";
                        break;

                    default:
                        message = "알수없음";
                        break;
                }
                Log.e("GoogleActivity", "SPEECH ERROR : " + message);
            }

            @Override
            public void onResults(Bundle results) {
                myCallback.callbackBundle(results);
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        };
    }


    //어디 지하철 역인지 파악하는 메소드
    public String recognizeStation(String stt_Station) {
        String targetStation = "";
        targetStation = stt_Station;
        Log.e("11", "stepppp done.");

        Log.e("최종결과는?", targetStation);
        return targetStation;
    }


    private int initCompletedStatus = 0;

    // 서비스 시작함수*******
    // 서비스에 필요한 변수들을 초기화한 후, 안내 시작 함수!
    public void initService(int status, final MyCallback myCallback) {

        final RecognitionListener sourceStationVoiceListener;

        // 마지막 변수 확정 리스너 -> 네, 아니요 답변에 따라, 재귀함수 시작 or navigate 함수 시작.
        final RecognitionListener confirmVoiceListener = getRecognitionListner(new MyCallback() {
            @Override

            public void callback() {
            }

            @Override
            public void callbackBundle(Bundle results) {

                String key = "";
                key = SpeechRecognizer.RESULTS_RECOGNITION;
                ArrayList<String> mResult = results.getStringArrayList(key);

                String answer = mResult.get(0);
                Log.e("v", "answer: " + answer);

                try {
                    Thread.sleep(2000);
////
//
//                    if (answer.charAt(0) == '아' && answer.charAt(1) == '니') {    // 아니오 라고 말했을때
//                        Subpage.this.initCompletedStatus = 0;
//                        is_station_perfect = false;
//                    } else if (answer.charAt(0) != '네' && answer.charAt(0) != '내' && answer.charAt(0) != '예') { //대답이 애매하거나 다른대답일때
//                        // 출발지, 도착지가 제대로 체크되지 않았다면, 함수 다시 시작!
//                        voice.TTS("다시 버튼을 눌러주세요.");
//                    } else {
//                        //제대로 체크됬다면 확정짓고 출발역의 맵데이터를 가져온다.
//                        Log.e("v", "Result src & dst: " + service.getSource_Station());
//                        Toast.makeText(Subpage.this, "연결할 역센터 = " + service.getSource_Station(), Toast.LENGTH_SHORT).show();
//
////                      서버 통신을 시작한다. 현재시간과 출발역 도착역 데이터를 서버에 전송한다.
//                        is_station_perfect = true;
//                        System.out.println("is station perfect =" + is_station_perfect);
//                        if (is_station_perfect == true) {
////
////
////          1.출발지와 목적지가 정확하다면 현재시각과 출발역을 서버로 post통신한다
//                            new Thread(new Runnable() {
//
//                                @Override
//                                public void run() {
//
//                                }
//                            }).start();
////
//                        }
//
//                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


        sourceStationVoiceListener = getRecognitionListner(new MyCallback() {
            @Override
            public void callback() {
            }

            @Override
            public void callbackBundle(Bundle results) {
                String key = "", stt_srcStation = "";
                key = SpeechRecognizer.RESULTS_RECOGNITION;
                ArrayList<String> mResult = results.getStringArrayList(key);
                stt_srcStation = mResult.get(0);
                stt_srcStation = recognizeStation(stt_srcStation);//입력받은 단어 파싱
                System.out.println("get : " + stt_srcStation);
//
                String st = stt_srcStation;
                String st2 = st.substring(st.length() - 1, st.length() - 0);
                System.out.println("st : " + st);
                System.out.println(("st2 :" + st2));
                if (st2.equals("역")) {
                    System.out.println("역자른거 :" + st.substring(0, st.length() - 1));
                    service.setSource_Station(st.substring(0, st.length() - 1));

                } else {
                    service.setSource_Station(stt_srcStation);

                }

                try {
                    Log.e("v", "Start Station onResults: " + service.getSource_Station()); //입력값 파싱 후 역 이름 로그 찍어보기

                    voice.TTS(service.getSource_Station() + "역 센터번호로 전화를 겁니다");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int i = Arrays.asList(subway_Station_name).indexOf(service.getSource_Station());
                                String num = "02-" + Emg_num[i];
                                System.out.println("전번" + num);
                                Subpage.this.initCompletedStatus = 1;
                                Intent intent_call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num));
                                startActivity(intent_call);

                            } catch (Exception e) {
                                voice.TTS("입력에러.현재역을 다시 말해주세요.");
                                Subpage.this.initCompletedStatus = 0;
                            }

                        }
                    }, 3000);

                } catch (NullPointerException e) {
                    voice.TTS("입력에러.현재역을 다시 말해주세요.");
                }

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        ArrayList<RecognitionListener> ListenerArray = new ArrayList<RecognitionListener>(Arrays.asList(sourceStationVoiceListener));

        // init 시작
        try {
            voice.setRecognitionListener(ListenerArray.get(status));
            voice.TTS(senario.getI2(status));
            Thread.sleep(2500);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        voice.STT();
    }

    String[] subway_Station_name = {
            "서울",
            "시청",
            "종각",
            "종로3가",
            "종로5가",
            "동대문",
            "신설동",
            "제기동",
            "청량리",
            "동묘앞",
            "시청",
            "을지로입구",
            "을지로3가",
            "을지로4가",
            "동대문역사문화공원",
            "신당",
            "상왕십리",
            "왕십리",
            "한양대",
            "뚝섬",
            "성수",
            "건대입구",
            "구의",
            "강변",
            "잠실나루",
            "잠실",
            "잠실새내",
            "종합운동장",
            "삼성",
            "선릉",
            "역삼",
            "강남",
            "교대)",
            "서초",
            "방배",
            "사당",
            "낙성대",
            "서울대입구",
            "봉천",
            "신림",
            "신대방",
            "구로디지털단지",
            "대림",
            "신도림",
            "문래",
            "영등포구청",
            "당산",
            "합정",
            "홍대입구",
            "신촌",
            "이대",
            "아현",
            "충정로",
            "용답",
            "신답",
            "신설동",
            "도림천",
            "양천구청",
            "신정네거리",
            "용두",
            "지축",
            "구파발",
            "연신내",
            "불광",
            "녹번",
            "홍제",
            "무악재",
            "독립문",
            "경복궁",
            "안국",
            "종로3가",
            "을지로3가",
            "충무로",
            "동대입구",
            "약수",
            "금호",
            "옥수",
            "압구정",
            "신사",
            "잠원",
            "고속터미널",
            "교대",
            "남부터미널",
            "양재",
            "매봉",
            "도곡",
            "대치",
            "학여울",
            "대청",
            "일원",
            "수서",
            "가락시장",
            "경찰병원",
            "오금",
            "당고개",
            "상계",
            "노원",
            "창동",
            "쌍문",
            "수유",
            "미아",
            "미아사거리 ",
            "길음",
            "성신여대입구",
            "한성대입구",
            "혜화",
            "동대문",
            "동대문역사문화공원",
            "충무로",
            "명동",
            "회현(",
            "서울",
            "숙대입구(갈월)",
            "삼각지",
            "신용산",
            "이촌(국립중앙박물관)",
            "동작(현충원)",
            "총신대입구(이수)",
            "사당",
            "남태령",
            "방화",
            "개화산",
            "김포공항",
            "송정",
            "마곡",
            "발산",
            "우장산",
            "화곡",
            "까치산",
            "신정(은행정)",
            "목동",
            "오목교(목동운동장앞)",
            "양평",
            "영등포구청",
            "영등포시장",
            "신길",
            "여의도",
            "여의나루",
            "마포",
            "공덕",
            "애오개",
            "충정로(경기대입구)",
            "서대문",
            "광화문(세종문화회관)",
            "종로3가(탑골공원)",
            "을지로4가",
            "동대문역사문화공원(DDP)",
            "청구",
            "신금호",
            "행당",
            "왕십리(성동구청)",
            "마장",
            "답십리",
            "장한평",
            "군자",
            "아차산",
            "광나루",
            "천호",
            "강동",
            "길동",
            "굽은다리",
            "명일",
            "고덕",
            "상일동",
            "둔촌동",
            "올림픽공원",
            "방이",
            "오금",
            "개롱",
            "거여",
            "마천",
            "강일",
            "미사",
            "하남풍산",
            "하남시청",
            "하남검단산",
            "응암",
            "역촌",
            "불광",
            "독바위",
            "연신내",
            "구산",
            "새절",
            "증산",
            "디지털미디어시티",
            "월드컵경기장",
            "마포구청",
            "망원",
            "합정",
            "상수",
            "광흥창",
            "대흥",
            "공덕",
            "효창공원앞",
            "삼각지",
            "녹사평",
            "이태원",
            "한강진",
            "버티고개",
            "약수",
            "청구",
            "신당",
            "동묘앞",
            "창신",
            "보문",
            "안암",
            "고려대",
            "월곡",
            "상월곡",
            "돌곶이",
            "석계",
            "태릉입구",
            "화랑대",
            "봉화산",
            "신내",
            "장암",
            "도봉산",
            "수락산",
            "마들",
            "노원",
            "중계",
            "하계",
            "공릉",
            "태릉입구",
            "먹골",
            "중화",
            "상봉",
            "면목",
            "사가정",
            "용마산",
            "중곡",
            "군자",
            "어린이대공원",
            "건대입구",
            "뚝섬유원지",
            "청담",
            "강남구청",
            "학동",
            "논현",
            "반포",
            "고속터미널",
            "내방",
            "이수",
            "남성",
            "숭실대입구",
            "상도",
            "장승배기",
            "신대방삼거리",
            "보라매",
            "신풍",
            "대림",
            "남구로",
            "가산디지털단지",
            "철산",
            "광명사거리",
            "천왕",
            "온수",
            "까치울",
            "부천종합운동장",
            "춘의",
            "신중동",
            "부천시청",
            "상동",
            "삼산체육관",
            "굴포천",
            "부평구청",
            "암사",
            "천호",
            "강동구청",
            "몽촌토성",
            "잠실",
            "석촌",
            "송파",
            "가락시장",
            "문정",
            "장지",
            "복정",
            "산성",
            "남한산성입구",
            "단대오거리",
            "신흥",
            "수진",
            "모란",
            "언주",
            "선정릉",
            "삼성중앙",
            "봉은사",
            "종합운동장",
            "삼전",
            "석촌고분",
            "석촌",
            "송파나루",
            "한성백제",
            "올림픽공원",
            "둔촌오륜",
            "중앙보훈병원",
    };

    String[] Emg_num = {
            "6110-1331",
            "6110-1321",
            "6110-1311",
            "6110-1301",
            "6110-1291",
            "6110-1281",
            "6110-1261",
            "6110-1251",
            "6110-1241",
            "6110-1271",
            "6110-2011",
            "6110-2021",
            "6110-2031",
            "6110-2041",
            "6110-2051",
            "6110-2061",
            "6110-2071",
            "6110-2081",
            "6110-2091",
            "6110-2101",
            "6110-2111",
            "6110-2121",
            "6110-2131",
            "6110-2141",
            "6110-2151",
            "6110-2161",
            "6110-2171",
            "6110-2181",
            "6110-2191",
            "6110-2201",
            "6110-2211",
            "6110-2221",
            "6110-2231",
            "6110-2241",
            "6110-2251",
            "6110-2261",
            "6110-2271",
            "6110-2281",
            "6110-2291",
            "6110-2301",
            "6110-2311",
            "6110-2321",
            "6110-2331",
            "6110-2341",
            "6110-2351",
            "6110-2361",
            "6110-2371",
            "6110-2381",
            "6110-2391",
            "6110-2401",
            "6110-2411",
            "6110-2421",
            "6110-2431",
            "6110-1341",
            "6110-1351",
            "6110-1371",
            "6110-2441",
            "6110-2451",
            "6110-2461",
            "6110-1361",
            "6110-3191",
            "6110-3201",
            "6110-3211",
            "6110-3221",
            "6110-3231",
            "6110-3241",
            "6110-3251",
            "6110-3261",
            "6110-3271",
            "6110-3281",
            "6110-3291",
            "6110-3301",
            "6110-4231",
            "6110-3321",
            "6110-3331",
            "6110-3341",
            "6110-3351",
            "6110-3361",
            "6110-3371",
            "6110-3381",
            "6110-3391",
            "6110-3401",
            "6110-3411",
            "6110-3421",
            "6110-3431",
            "6110-3441",
            "6110-3451",
            "6110-3461",
            "6110-3471",
            "6110-3481",
            "6110-3491",
            "6110-3501",
            "6110-3511",
            "6110-3521",
            "6110-4091",
            "6110-4101",
            "6110-4111",
            "6110-4121",
            "6110-4131",
            "6110-4141",
            "6110-4151",
            "6110-4161",
            "6110-4171",
            "6110-4181",
            "6110-4191",
            "6110-4201",
            "6110-4211",
            "6110-4221",
            "6110-4231",
            "6110-4241",
            "6110-4251",
            "6110-4261",
            "6110-4271",
            "6110-4281",
            "6110-4291",
            "6110-4301",
            "6110-4311",
            "6110-4321",
            "6110-4331",
            "6110-4341",
            "6311-5101",
            "6311-5111",
            "6311-5121",
            "6311-5131",
            "6311-5141",
            "6311-5151",
            "6311-5161",
            "6311-5171",
            "6311-5181",
            "6311-5191",
            "6311-5201",
            "6311-5211",
            "6311-5221",
            "6311-5231",
            "6311-5241",
            "6311-5251",
            "6311-5261",
            "6311-5271",
            "6311-5281",
            "6311-5291",
            "6311-5301",
            "6311-5311",
            "6311-5321",
            "6311-5331",
            "6311-5341",
            "6311-5351",
            "6311-5361",
            "6311-5371",
            "6311-5381",
            "6311-5391",
            "6311-5401",
            "6311-5411",
            "6311-5421",
            "6311-5431",
            "6311-5441",
            "6311-5451",
            "6311-5461",
            "6311-5471",
            "6311-5481",
            "6311-5491",
            "6311-5501",
            "6311-5511",
            "6311-5521",
            "6311-5531",
            "6311-5541",
            "6311-5551",
            "6311-5561",
            "6110-3521",
            "6311-5581",
            "6311-5591",
            "6311-5601",
            "6311-5611",
            "6311-5621",
            "6311-5631",
            "6311-5641",
            "6311-5651",
            "6311-6101",
            "6311-6111",
            "6311-6121",
            "6311-6131",
            "6110-3211",
            "6311-6151",
            "6311-6161",
            "6311-6171",
            "6311-6181",
            "6311-6191",
            "6311-6201",
            "6311-6211",
            "6311-6221",
            "6311-6231",
            "6311-6241",
            "6311-6251",
            "6311-5291",
            "6311-6271",
            "6311-6281",
            "6311-6291",
            "6311-6301",
            "6311-6311",
            "6311-6321",
            "6311-6331",
            "6311-5371",
            "6311-6351",
            "6311-6361",
            "6311-6371",
            "6311-6381",
            "6311-6391",
            "6311-6401",
            "6311-6411",
            "6311-6421",
            "6311-6431",
            "6311-6441",
            "6311-7171",
            "6311-6461",
            "6311-6471",
            "6311-6481",
            "6311-7091",
            "6311-7101",
            "6311-7111",
            "6311-7121",
            "6311-7131",
            "6311-7141",
            "6311-7151",
            "6311-7161",
            "6311-7171",
            "6311-7181",
            "6311-7191",
            "6311-7201",
            "6311-7211",
            "6311-7221",
            "6311-7231",
            "6311-7241",
            "6311-5441",
            "6311-7261",
            "6311-7271",
            "6311-7281",
            "6311-7291",
            "6311-7301",
            "6311-7311",
            "6311-7321",
            "6311-7331",
            "6311-7341",
            "6311-7351",
            "6311-7361",
            "6311-7371",
            "6311-7381",
            "6311-7391",
            "6311-7401",
            "6311-7411",
            "6311-7421",
            "6311-7431",
            "6311-7441",
            "6311-7451",
            "6311-7461",
            "6311-7471",
            "6311-7481",
            "6311-7491",
            "6311-7501",
            "6311-7511",
            "6311-7521",
            "6311-7531",
            "6311-7541",
            "6311-7551",
            "6311-7561",
            "6311-7571",
            "6311-7581",
            "6311-7591",
            "6311-8101",
            "6311-5471",
            "6311-8121",
            "6311-8131",
            "6311-8141",
            "6311-8151",
            "6311-8161",
            "6311-8171",
            "6311-8181",
            "6311-8191",
            "6311-8201",
            "6311-8211",
            "6311-8221",
            "6311-8231",
            "6311-8241",
            "6311-8251",
            "6311-8261",
            "2656-0926",
            "2656-0927",
            "2656-0928",
            "2656-0929",
            "2656-0930",
            "2656-0931",
            "2656-0932",
            "2656-0933",
            "2656-0934",
            "2656-0935",
            "2656-0936",
            "2656-0937",
            "2656-0938",
    };
}
