package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import org.tensorflow.demo.blescan.BeaconListAdapter;
import org.tensorflow.demo.blescan.UserRssi;
import org.tensorflow.demo.data.UserpositonData;
import org.tensorflow.demo.network.RetrofitClient;
import org.tensorflow.demo.network.ServiceApi;
import org.tensorflow.demo.vision_module.Voice;

import java.util.ArrayList;
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

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera3);
        Button back = findViewById(R.id.backto_navi);
        Button readinfo = findViewById(R.id.readinfo);
        Button btn_userpositon =findViewById(R.id.get_userposition);
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
        try{
            mMinewBeaconManager.startScan();
        }
        catch(NullPointerException e) {
            voice.TTS("블루투스를 켜주세요");
            System.out.println("블루투스를 켜주세요");
            e.printStackTrace();
        }

        initView();
        initManager();
        checkBluetooth();
        initListener();

        Toast.makeText(this, transfer_info, Toast.LENGTH_SHORT).show();

        readinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    voice.TTS("환승정보"+transfer_info +"열차도착정보"+arrival + "실시간열차정보"+userPosition_info);
                    if (transfer_info.isEmpty() == true && arrival.isEmpty() == true) {
                        voice.TTS("현재정보가 없습니다. 경로설정이나 열차를 조회하십시오");
                    }
                    voice.TTS(arrival);
                    if (transfer_info.isEmpty() == true) {
                        voice.TTS("환승정보 없음");
                    } else if (arrival.isEmpty() == true) {
                        voice.TTS("열차정보 없음");
                    } else if(userPosition_info.isEmpty()==true){
                        voice.TTS("실시간 도착 정보없음");
//                        voice.TTS("현재 정보 읽기 오류");
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
            } if (Dst_station == null) {
                tv2.setText("도착역 : 정보없음");
            } if (transfer_info.isEmpty() == true) {
                tv3.setText("환승역 : 정보없음");
            } if (arrival.isEmpty() == true) {
                tv4.setText("열차도착 : 정보없음");
            } if (userPosition_info.isEmpty() == true) {
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

                Intent intent_call = new Intent(Intent.ACTION_CALL, Uri.parse("tel:01052662394"));
                startActivity(intent_call);
            }
        });

        btn_userpositon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request_getUserposition();
                tv5.setText(userPosition_info);
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
       final Subpage subpage =new Subpage();
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
                                + "현재 위치는 [ " + usersta_clone.get(i)+" ] 입니다.";
//
                    }
                    System.out.println(userposition_info);
                    setUserPosition_info(userposition_info);

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
                                BeaconName.add("화장실");
                            } else if (minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue().contains("AB8190D5") == true) {
                                BeaconName.add("엘레베이터");
                            } else if (minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue().contains("74278BDA") == true) {
                                BeaconName.add("개찰구");
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

                                Collections.sort(BeaconName_clone);

                                String past = "";
                                String current = "";

                                for(String value : BeaconName_clone){
                                    past += value;
                                }
                                System.out.println("past = " + past);
                                for(String value : nBeacon){
                                    current += value;
                                }

                                System.out.println("current = " + current);
                                if (past.equals(current)) {
                                    System.out.println("비콘 변화 X");
                                }
                                else if(current.isEmpty() == true){
                                    voice.TTS("주변에 " + past +  "가 있습니다");
                                }
                                else {
                                    voice.TTS("주변에 " + current +"가 있습니다");
                                }


                                nBeacon = (ArrayList<String>) BeaconName_clone.clone();
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
}
