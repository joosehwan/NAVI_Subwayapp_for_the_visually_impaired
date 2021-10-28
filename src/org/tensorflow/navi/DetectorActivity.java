/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.navi;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.icu.text.SimpleDateFormat;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationRequest;
import com.google.gson.Gson;
import com.minew.beacon.BeaconValueIndex;
import com.minew.beacon.BluetoothState;
import com.minew.beacon.MinewBeaconManager;
import com.minew.beacon.MinewBeacon;
import com.minew.beacon.MinewBeaconManagerListener;


import org.tensorflow.navi.R;
import org.tensorflow.navi.OverlayView.DrawCallback;
import org.tensorflow.navi.blescan.BeaconListAdapter;
import org.tensorflow.navi.blescan.UserRssi;
import org.tensorflow.navi.data.OcrResponse;
import org.tensorflow.navi.data.Ocrdata;
import org.tensorflow.navi.data.SubwayData;
import org.tensorflow.navi.data.SubwayResponse;
import org.tensorflow.navi.data.Subwayapi;
import org.tensorflow.navi.data.TrainNum;
import org.tensorflow.navi.data.TransportData;
import org.tensorflow.navi.env.BorderedText;
import org.tensorflow.navi.env.ImageUtils;
import org.tensorflow.navi.network.RetrofitClient;
import org.tensorflow.navi.network.ServiceApi;
import org.tensorflow.navi.tracking.MultiBoxTracker;
import org.tensorflow.navi.vision_module.Compass;
import org.tensorflow.navi.vision_module.MyCallback;
import org.tensorflow.navi.vision_module.MyGps;
import org.tensorflow.navi.vision_module.SOTWFormatter;
import org.tensorflow.navi.vision_module.Service;
import org.tensorflow.navi.vision_module.Voice;
import org.tensorflow.navi.vision_module.senario;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {

    // Configuration values for tiny-yolo-voc. Note that the graph is not included with TensorFlow and
    // must be manually placed in the assets/ directory by the user.
    // must be manually placed in the assets/ directory by the user.
    // Graphs and models downloaded from http://pjreddie.com/darknet/yolo/ may be converted e.g. via
    // DarkFlow (https://github.com/thtrieu/darkflow). Sample command:
    // ./flow --model cfg/tiny-yolo-voc.cfg --load bin/tiny-yolo-voc.weights --savepb --verbalise

    //Setting----------------------------------------------------------------------------------------------
    private static final String YOLO_MODEL_FILE = "file:///android_asset/hanium_subway_items.pb";
//    android_asset/hanium_subway_items.pb
    private static final int YOLO_INPUT_SIZE = 416;
    private static final String YOLO_INPUT_NAME = "input";
    private static final String YOLO_OUTPUT_NAMES = "output";
    private static final int YOLO_BLOCK_SIZE = 32;

    private enum DetectorMode {
        YOLO
    }

    private static final DetectorMode MODE = DetectorMode.YOLO;
    // Minimum detection confidence to track a detection.
    public static final float MINIMUM_CONFIDENCE_YOLO = 0.5f;
    private static final boolean MAINTAIN_ASPECT = MODE == DetectorMode.YOLO;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private Integer sensorOrientation;
    private Classifier detector;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private float bitmapWidth = 0;
    private float bitmapHeight = 0;
    private boolean computingDetection = false;
    private long timestamp = 0;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    public MultiBoxTracker tracker;
    public OverlayView trackingOverlay;

    private byte[] luminanceCopy;
    private BorderedText borderedText;
    private RequestQueue requestQueue;
    private LocationRequest locationRequest;
    private MyGps myGps;
    private Service service;
    private Voice voice;
    private Compass compass;
    private SOTWFormatter sotwFormatter;
    private boolean yoloFirstStartFlag = false;
    public InstanceMatrix instanceMatrix = new InstanceMatrix();
    TensorFlowYoloDetector tensorFlowYoloDetector = new TensorFlowYoloDetector();

    //각종 변수--------------------------------------------------------------------------------------

    //메인페이지 상단에 뜨는 출발역 도착역 변수
    public static String Src_station;
    public static String Dst_station;

    //  SubPage를 거쳐온  데이터들. 유지되어야하는 변수들_static
    public static String Src_static = "";
    public static String Dst_static = "";
    public static String transfer_static = "";
    public static String userposition_static = "";

    // ocr로 검출된 데이터의 중복제거를 위한 변수
    TreeSet<String> arr;
    ArrayList<String> Deduplicated_labellist;

    //음성인식에 띠링 소리를 삽입하기위한 설정변수
    SoundPool soundPool;
    int soundID;

    // 출발역 도착역 api전송하기 위한 변수
    String src_station_data;
    String dst_station_data;


    // 환승역 api에 사용되는 변수----------------------------------------------------------------------
    static String Transfer_data_tosub = "";

    public void setTransfer_data_tosub(String transfer_data_tosub) {
        Transfer_data_tosub = transfer_data_tosub;
    }

    //request_Getsubwaynum의 결과를 저장해주는 변수-----------------------------------------------------
    static String arrivalinfo = "";

    public static void setArrivalinfo(String arrivalinfo) {
        DetectorActivity.arrivalinfo = arrivalinfo;
    }

    //restapi 서버 통신 객체 선언
    public ServiceApi serviceApi;

    //지하철api 객체선언
    Subwayapi subwayapi = new Subwayapi();
    boolean is_station_perfect = false;

    //비콘 객체
    private MinewBeaconManager mMinewBeaconManager;
    private BeaconListAdapter mAdapter;
    private static final int REQUEST_ENABLE_BT = 2;
    UserRssi comp = new UserRssi();

    ArrayList<String> uuuid = new ArrayList<String>();
    ArrayList<String> rsssi = new ArrayList<String>();
    ArrayList<String> ttx_power = new ArrayList<String>();
    ArrayList<Double> values = new ArrayList<Double>();
    ArrayList<String> BeaconName = new ArrayList<String>();
    ArrayList<String> nBeacon = new ArrayList<String>();
    //클론할 비콘
    ArrayList<String> uuuid_clone = new ArrayList<String>();
    ArrayList<String> rsssi_clone = new ArrayList<String>();
    ArrayList<String> ttx_power_clone = new ArrayList<String>();
    ArrayList<Double> values_clone = new ArrayList<Double>();
    ArrayList<String> BeaconName_clone = new ArrayList<String>();
    private int state;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serviceApi = RetrofitClient.getClient().create(ServiceApi.class);

//      subPage값들 받아오기
        Button Path_Settings = findViewById(R.id.input_dest);
        Button readocr = findViewById(R.id.readOCR);
        Button takesubway = findViewById(R.id.takesubway);

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        soundID = soundPool.load(this, R.raw.voice_effect, 1);

        final TextView ocrtext = findViewById(R.id.cameraOCR);
        final TextView detectedClass = findViewById(R.id.cameraclick);
        Button goSub = findViewById(R.id.goto_subpage);
        initView();
        initManager();
        initListener();
        checkBluetooth();


        readocr.setOnClickListener(v -> {


        });
//       열차도착예정버튼()
        takesubway.setOnClickListener(v -> {

            try {
                request_Getsubwaynum();

            } catch (Exception e) {

            }
        });

        goSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Subpage.class);
                System.out.println("time : " + getTime());

                intent.putExtra("arrivalinfo", arrivalinfo);

                intent.putExtra("transfer", Transfer_data_tosub);

                startActivity(intent);

                finish();
//
            }
        });

        final CameraActivity cameraActivity = new CameraActivity() {
            @Override
            protected void processImage() {

            }

            @Override
            protected void onPreviewSizeChosen(Size size, int rotation) {

            }

            @Override
            protected int getLayoutId() {
                return 0;
            }

            @Override
            protected Size getDesiredPreviewFrameSize() {
                return null;
            }
        };

        detectedClass.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {

                    camera2Fragment.takePicture();
                    post_ocr_data();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            get_ocr_data();//딜레이 후 시작할 코드 작성
                            System.out.println("5초 딜레이");

                        }
                    }, 7000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                if (tensorFlowYoloDetector.clone == null) {

                    try {
                        voice.TTS("전방 내용없음.");
                    } catch (Exception e) {
                        Log.e("", "인식된값없음");
                    }
                } else {

                    // TreeSet으로 리스트 중복제거.
                    arr = new TreeSet<>(tensorFlowYoloDetector.clone);    // treeset에 labellist값 대입
                    Deduplicated_labellist = new ArrayList<String>(arr); //중복제거된 treeset을 다시대입

                    String signText = "sign";
                    if (Deduplicated_labellist.contains(signText)) {
//                        try {
//                            System.out.println("");
//                            camera2Fragment.takePicture();
//                            post_ocr_data();
//                            new Handler().postDelayed(new Runnable()
//                            {
//                                @Override
//                                public void run()
//                                {   get_ocr_data();//딜레이 후 시작할 코드 작성
//                                    System.out.println("5초 딜레이");
//
//                                }
//                            }, 5000);

//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                    }
                }
                String front = "";
                String changeResult = "";
                try {
                    for (int i = 0; i < Deduplicated_labellist.size(); i++) {

                        switch (Deduplicated_labellist.get(i)) {
                            case "elevator":
                                changeResult = "엘레베이터가";
                                break;
                            case "turnstile":
                                changeResult = "개찰구가";
                                break;
                            case "chair":
                                changeResult = "의자가";
                                break;
                            case "stairs":
                                changeResult = "계단이";
                                break;
                            case "escalator":
                                changeResult = "에스컬레이터가";
                                break;
                            case "toilet":
                                changeResult = "화장실이";
                                break;
                            case "sign":
                                changeResult = "이정표가";
                                break;
                            case "number":
                                changeResult = "번호칸이";
                                break;
                            default:
                                break;
                        }
                        front += changeResult + "  ";

                    }
                } catch (Exception e) {
                    voice.TTS("전방 내용없음.");
                }

                ocrtext.setText(front);
                if (front.isEmpty() == true) {
                    voice.TTS("전방에 장애물이 없습니다");
                    ocrtext.setText("전방에 장애물이 없습니다");
                } else {
                    voice.TTS("  전방에" + front + " 있습니다.");
                }


            }
        });

        Intent get_intent = getIntent();    //값을 받아오면 isActFirst = false
        TextView start = findViewById(R.id.station_start);
        TextView destination = findViewById(R.id.station_destination);
//
        try {

            Src_static = get_intent.getStringExtra("Src");
            Dst_static = get_intent.getStringExtra("Dst");
            transfer_static = get_intent.getStringExtra("transfer");

            destination.setText(Dst_static);
            start.setText(Src_static);

            System.out.println("받은 src" + Src_static);
            System.out.println("받은 dst" + Dst_static);


        } catch (Exception e) {

            destination.setText("도착역 미정");
            start.setText("출발역 미정");
        }


        // 5 * 5 분면의 InstanceBuffer 초기화
        instanceMatrix.initMat(5, 5);
        myGps = new MyGps(DetectorActivity.this, locationListener);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                myGps.startGps(DetectorActivity.this.service);
                Log.e("thread", "run: start");
            }
        }, 0);
        //Compass
        compass = new Compass(this);
        sotwFormatter = new SOTWFormatter(this); // 방향 포맷,,방위각 보고 N,NW ..써주는 친구..
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);

        // Voice
        voice = new Voice(this, null);

        // API Server
        requestQueue = Volley.newRequestQueue(DetectorActivity.this);  // 전송 큐

        // Service
        service = new Service();

        Path_Settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 터치 up이 되었을 때 화면을 갱신한다.
        TextView start = findViewById(R.id.station_start);
        TextView destination = findViewById(R.id.station_destination);

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
//              invalidate()을 호출하면 화면을 갱신한다.
                destination.bringToFront();
                start.bringToFront();
                destination.invalidate();
                start.invalidate();
                break;
        }
        return true;

    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {


        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        detector = TensorFlowYoloDetector.create(
                getAssets(),
                YOLO_MODEL_FILE,
                YOLO_INPUT_SIZE,
                YOLO_INPUT_NAME,
                YOLO_OUTPUT_NAMES,
                YOLO_BLOCK_SIZE);

        int cropSize = YOLO_INPUT_SIZE;

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
//    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);
//
//    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            //tracker.drawDebug(canvas);
                        }
                    }
                });
    }

    @Override
    protected void processImage() {

        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        tracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @TargetApi(Build.VERSION_CODES.N)
                    @Override
                    public void run() {
                        if (!DetectorActivity.this.yoloFirstStartFlag) {
                            DetectorActivity.this.yoloFirstStartFlag = true;
                            voice.TTS("로딩완료");
                        }
//                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();

                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

                        if (bitmapHeight == 0 || bitmapWidth == 0) {
                            DetectorActivity.this.bitmapHeight = croppedBitmap.getHeight();
                            DetectorActivity.this.bitmapWidth = croppedBitmap.getWidth();
//                            Log.e("bitmapSize", "width: " + bitmapWidth);
//                            Log.e("bitmapSize", "height: " + bitmapWidth);
//                          instanceTimeBuffer.setBitmapHeight(DetectorActivity.this.bitmapHeight);
//                          instanceTimeBuffer.setBitmapWidth(DetectorActivity.this.bitmapWidth);
                        }
                        // Canvas On/Off 기능 생각해보기
                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_YOLO;

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

// 이코드가 없으면 화면에 네모박스 안생김
                        for (final Classifier.Recognition resultb : results) {

                            Classifier.Recognition result = resultb.clone();
                            final RectF location = result.getLocation();

                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }
//                        DetectorActivity.this.lastProcessingTimeMs1 += SystemClock.uptimeMillis() - startTime;

                        tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                        trackingOverlay.postInvalidate();

                        requestRender();
                        computingDetection = false;
                    }
                });

    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onSetDebug(final boolean debug) {
        detector.enableStatLogging(debug);
    }


//--Listener----------------------------------------------------------------------------------------------------------------------------------------


    // GPS Location 정보 획득시 리스너 객체
    final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            service.setLatitude(location.getLatitude());
            service.setLongitude(location.getLongitude());

            Log.e("t", "service 위도: " + service.getLatitude());
            Log.e("t", "service 경도: " + service.getLongitude() + "\n..\n");

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e("t", "startGps: 상태변화");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e("t", "startGps: 사용가능");
            //myGps.startGps();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e("t", "startGps: 사용불가");
        }
    };

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


//--Function----------------------------------------------------------------------------------------------------------------------------------------


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
        final RecognitionListener destStationVoiceListener;
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
//
                    if (answer.charAt(0) == '아' && answer.charAt(1) == '니') {    // 아니오 라고 말했을때
                        DetectorActivity.this.initCompletedStatus = 0;
                        is_station_perfect = false;
                    } else if (answer.charAt(0) != '네' && answer.charAt(0) != '내' && answer.charAt(0) != '예') { //대답이 애매하거나 다른대답일때
                        // 출발지, 도착지가 제대로 체크되지 않았다면, 함수 다시 시작!
                        voice.TTS("다시 버튼을 눌러주세요.");
                    } else {
                        //제대로 체크됬다면 확정짓고 출발역의 맵데이터를 가져온다.
                        Log.e("v", "Result src & dst: " + service.getSource_Station() + " " + service.getDest_Station());
                        Toast.makeText(DetectorActivity.this, "출발역 = " + service.getSource_Station() + "\n 도착역 = " + service.getDest_Station(), Toast.LENGTH_SHORT).show();

//                      서버 통신을 시작한다. 현재시간과 출발역 도착역 데이터를 서버에 전송한다.
                        is_station_perfect = true;
                        System.out.println("is station perfect =" + is_station_perfect);
                        if (is_station_perfect == true) {
//
//
//          1.출발지와 목적지가 정확하다면 현재시각과 출발역을 서버로 post통신한다
                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("service.getSource_Station() : " + service.getSource_Station());
                                    System.out.println("service.getDest_Station() : " + service.getDest_Station());

                                    src_station_data = subwayapi.get_Src_XmlData(service.getSource_Station());
                                    dst_station_data = subwayapi.get_Dst_XmlData(service.getDest_Station());

                                    startPost(new SubwayData(service.getSource_Station(), service.getDest_Station()));
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    try {
                                                        //환승정보 요청
                                                        request_getTransportData();
                                                    } catch (Exception e) {
                                                        e.getMessage();
                                                        voice.TTS("환승정보를 불러올 수 없습니다.");
                                                    }


                                                }
                                            }).start();

                                        }
                                    });
                                }
                            }).start();
//
                        }

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });


        // 도착역 리스너
        destStationVoiceListener = getRecognitionListner(new MyCallback() {
            @Override
            public void callback() {

            }

            @Override
            public void callbackBundle(Bundle results) {
                final TextView station_destination = findViewById(R.id.station_destination);
                String key = "", stt_dstStation = "";

                key = SpeechRecognizer.RESULTS_RECOGNITION;
                ArrayList<String> mResult = results.getStringArrayList(key);
                stt_dstStation = mResult.get(0);
                stt_dstStation = recognizeStation(stt_dstStation);

                service.setDest_Station(stt_dstStation);

                Log.e("v", "End Station onResults: " + service.getDest_Station());
                Dst_station = service.getDest_Station();

                station_destination.setText(Dst_station);
                station_destination.bringToFront();


                try {
                    Thread.sleep(4000);
                    voice.TTS(service.getSource_Station() + "에서 출발하여서 " +
                            service.getDest_Station() + "으로 도착이 맞습니까? 네, 아니요로 대답해주세요.");
                    voice.setRecognitionListener(confirmVoiceListener);
                    Thread.sleep(8200);
                    voice.STT();
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
                final TextView station_start = findViewById(R.id.station_start);
                key = SpeechRecognizer.RESULTS_RECOGNITION;
                ArrayList<String> mResult = results.getStringArrayList(key);
                stt_srcStation = mResult.get(0);
                stt_srcStation = recognizeStation(stt_srcStation);//입력받은 단어 파싱
                service.setSource_Station(stt_srcStation);
                Log.e("v", "Start Station onResults: " + service.getSource_Station()); //입력값 파싱 후 역 이름 로그 찍어보기

                Src_station = service.getSource_Station();
                station_start.setText(Src_station);
                station_start.bringToFront();

                DetectorActivity.this.initCompletedStatus = 1;

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                voice.TTS(senario.destStationString);
//
                voice.setRecognitionListener(destStationVoiceListener);
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                voice.STT();
            }
        });
        ArrayList<RecognitionListener> ListenerArray = new ArrayList<RecognitionListener>(Arrays.asList(sourceStationVoiceListener,
                destStationVoiceListener, confirmVoiceListener));

        // init 시작
        try {
            voice.setRecognitionListener(ListenerArray.get(status));
            voice.TTS(senario.getI(status));
            Thread.sleep(2500);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        voice.STT();
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                DetectorActivity.this.service.setAzimuth(azimuth);
            }
        };
    }

    //    볼륨 '하'키를 누르면 서비스가 시작된다
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            this.debug = !this.debug;

            request_getTransportData();
            return true;

        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {


            //  서비스를 위한 초기화 작업 시작

            initService(initCompletedStatus, new MyCallback() {
                @Override
                public void callback() {
                    Log.e("n", "Navigate 시작");

//                    service.setReadyFlag(true);
                }

                @Override
                public void callbackBundle(Bundle result) {

                }
            });

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d("compass", "start compass");
        compass.start();
    }

    @Override
    public void onPause() {

        super.onPause();
        compass.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("compass", "stop compass");
        compass.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        voice.close();
    }


    //  현재 시간을 받아오는 함수
    private String getTime() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        String getTime = dateFormat.format(date);
        return getTime;
    }

    public void startPost(SubwayData data) {
        serviceApi.subwayPost(data).enqueue(new Callback<SubwayResponse>() {
            @Override
            public void onResponse(Call<SubwayResponse> call, Response<SubwayResponse> response) {
                SubwayResponse result = response.body();
                if (response.isSuccessful()) {
                    String result_body = new Gson().toJson(response.body());
                    System.out.println(result_body + " = result-body ");
                    System.out.println(result.toString() + " = result-body ");
                    System.out.println("포스트 성공");


                }
            }

            @Override
            public void onFailure(Call<SubwayResponse> call, Throwable throwable) {
                System.out.println(throwable.getMessage());
                System.out.println("출발역 보내기 실패");


            }
        });
    }


    //  request_Getsubwaynum의 리스트
    ArrayList<String> request_station = new ArrayList<String>();
    ArrayList<String> request_trainline = new ArrayList<String>();
    ArrayList<Integer> request_arrivetime = new ArrayList<Integer>();

    //저장을 위한 clone 리스트
    ArrayList<String> request_station_clone = new ArrayList<String>();
    ArrayList<String> request_trainline_clone = new ArrayList<String>();
    ArrayList<Integer> request_arrivetime_clone = new ArrayList<Integer>();


    // 열차위치역과 열차번호 받는 함수 arrival 테이블 긁어오는것
    public void request_Getsubwaynum() {
        Call<List<TrainNum>> getCall = serviceApi.get_trainnum();
        getCall.enqueue(new Callback<List<TrainNum>>() {
            @Override
            public void onResponse(Call<List<TrainNum>> call, Response<List<TrainNum>> response) {

                if (response.isSuccessful()) {
                    List<TrainNum> trainNumList = response.body();
                    String result = "";
//                  station역 출발, trainline 행 arrivetime 초 뒤 도착합니다
                    for (TrainNum item : trainNumList) {
                        result += "출발역 : " + item.getStation() + "\n"
                                + "trainline : " + item.getTrainline() + "\n"
                                + "arrivetime : " + item.getArrivetime();
                        request_station.add(item.getStation());
                        request_trainline.add(item.getTrainline());
                        request_arrivetime.add(item.getArrivetime());

                    }

                    request_station_clone = (ArrayList<String>) request_station.clone();
                    request_trainline_clone = (ArrayList<String>) request_trainline.clone();
                    request_arrivetime_clone = (ArrayList<Integer>) request_arrivetime.clone();

                    request_station.clear();
                    request_trainline.clear();
                    request_arrivetime.clear();

                    System.out.println("request_station_clone :" + request_station_clone);
                    System.out.println("request_trainline_clone :" + request_trainline_clone);
                    System.out.println("request_arrivetime_clone :" + request_arrivetime_clone);

                    int size = request_trainline_clone.size();
                    String arrival_info = "";
                    for (int i = 0; i < size; i++) {
                        arrival_info += request_trainline_clone.get(i) + "\n";
                        if (request_arrivetime_clone.get(i) / 60 == 0) {
                            arrival_info += " 곧 도착.";
                        } else {
                            arrival_info += request_arrivetime_clone.get(i) / 60 + "분 후 도착 \n\n";
                        }


                    }
                    System.out.println(arrival_info);
                    setArrivalinfo(arrival_info);
                    voice.TTS(arrival_info);

                }
            }

            @Override
            public void onFailure(Call<List<TrainNum>> call, Throwable throwable) {
                System.out.println(throwable.getMessage());
                voice.TTS("열차도착정보를 받을 수 없습니다. 출발역을 입력하세요");
            }
        });
    }

    //    환승역 서버로 받기 위한 리스트 선언 .

    ArrayList<String> startwname = new ArrayList<>(); //출발역
    ArrayList<String> startline = new ArrayList<>(); // 출발역 방향
    ArrayList<String> exchaline = new ArrayList<>(); // 환승역 방향
    ArrayList<String> exchawname = new ArrayList<>(); //환승역

    //    저장을 위한 clone 리스트 선언
    ArrayList<String> startwname_clone = new ArrayList<>(); //출발역
    ArrayList<String> startline_clone = new ArrayList<>(); // 출발역 방향
    ArrayList<String> exchaline_clone = new ArrayList<>(); // 환승역 방향
    ArrayList<String> exchawname_clone = new ArrayList<>(); //환승역

    // 환승역정보를 get하는 함수
    public void request_getTransportData() {
        Call<List<TransportData>> getCall = serviceApi.get_transport();
        getCall.enqueue(new Callback<List<TransportData>>() {
            @Override
            public void onResponse(Call<List<TransportData>> call, Response<List<TransportData>> response) {
                if (response.isSuccessful()) {
                    List<TransportData> transportData = response.body();
                    String result = "";
                    for (TransportData item : transportData) {
                        result += "출발역 : " + item.getStartname() + "\n"
                                + "출발역 방향 : " + item.getStartline() + "\n"
                                + "환승역 : " + item.getExchawname() + "\n"
                                + "환승역 방향 : " + item.getExchaline();
                        startwname.add(item.getStartname());
                        startline.add(item.getStartline());
                        exchawname.add(item.getExchawname());
                        exchaline.add(item.getExchaline());

                    }
                    startwname_clone = (ArrayList<String>) startwname.clone();
                    startline_clone = (ArrayList<String>) startline.clone();
                    exchawname_clone = (ArrayList<String>) exchawname.clone();
                    exchaline_clone = (ArrayList<String>) exchaline.clone();

                    startwname.clear();
                    startline.clear();
                    exchawname.clear();
                    exchaline.clear();

                    int size = startwname_clone.size();
                    String transport_info = "";
                    for (int i = 0; i < size; i++) {
                        transport_info += startwname_clone.get(i) + "역 "
                                + startline_clone.get(i) + "행 열차 승차 후 \n\n"
                                + exchawname_clone.get(i) + "역 "
                                + exchaline_clone.get(i) + " 행 열차 환승. \n\n";
                    }
                    transport_info += "목적지 " + Dst_station + "하차.";
                    System.out.println(transport_info);
                    //subpage로 보내기 위한 set
                    setTransfer_data_tosub(transport_info);
                    voice.TTS(transport_info);

                }
            }

            @Override
            public void onFailure(Call<List<TransportData>> call, Throwable throwable) {
                voice.TTS("환승정보를 받을 수 없습니다. ");
            }
        });

    }

//    // userposition 을 받기위한 리스트 선언
//    ArrayList<Integer> usertrain = new ArrayList<>();
//    ArrayList<String> usersta = new ArrayList<>();
//    ArrayList<Integer> usertrain_clone = new ArrayList<>();
//    ArrayList<String> usersta_clone = new ArrayList<>();
//    public void request_getUserposition() {
//        Call<List<UserpositonData>> getCall = serviceApi.get_userposition();
//        getCall.enqueue(new Callback<List<UserpositonData>>() {
//            @Override
//            public void onResponse(Call<List<UserpositonData>> call, Response<List<UserpositonData>> response) {
//                if (response.isSuccessful()) {
//                    List<UserpositonData> userpositonData = response.body();
//                    String result = "";
//                    for (UserpositonData item : userpositonData) {
//                        result += "실시간 열차 번호 : " + item.getusertrain()
//                                + "\n실시간 열차위치(역) :" + item.getusersta();
//
//                        usertrain.add(item.getusertrain());
//                        usersta.add(item.getusersta());
//                    }
//                    System.out.println(result);
//                    usertrain_clone = (ArrayList<Integer>) usertrain.clone();
//                    usersta_clone = (ArrayList<String>) usersta.clone();
//                    usertrain.clear();
//                    usersta.clear();
//
//                    int size = usertrain_clone.size();
//                    String userposition_info = "";
//                    for (int i = 0; i < size; i++) {
//                        userposition_info += "실시간 열차 번호 : " + usertrain_clone.get(i) + "\n"
//                                + "실시간 열차위치(역) :" + usersta_clone.get(i);
//
//                    }
//                    System.out.println(userposition_info);
//                    setUserPosition_info(userposition_info);
//                    voice.TTS(userposition_info);
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<UserpositonData>> call, Throwable throwable) {
//                voice.TTS("실시간 열차정보를 받을 수 없습니다. ");
//            }
//        });
//
//    }


    //OCR이미지를 post하는 함수
    public void post_ocr_data() {
        File mFile;
        String strFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Capture/capture.jpg";
        mFile = new File(strFolderPath);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), mFile);
        MultipartBody.Part fileToupload = MultipartBody.Part.createFormData("image", "capture.jpg", requestBody);
        serviceApi.ocr_data(fileToupload).enqueue(new Callback<OcrResponse>() {
            @Override
            public void onResponse(Call<OcrResponse> call, Response<OcrResponse> response) {
                OcrResponse result = response.body();
                if (response.isSuccessful()) {
                    String result_body = new Gson().toJson(response.body());
                    System.out.println(result_body + " = 이미지");
                    System.out.println(result.toString() + "이미지");

                } else {
                    System.out.println("이미지 삽입 실패");
                }
            }

            @Override
            public void onFailure(Call<OcrResponse> call, Throwable throwable) {
                System.out.println(throwable.getMessage());
            }
        });

    }

    //OCR이미지를 update하는 함수
    public void put_ocr_data() {
        File mFile;
        String strFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Capture/capture.jpg";
        mFile = new File(strFolderPath);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), mFile);
        MultipartBody.Part fileToupload = MultipartBody.Part.createFormData("image", "capture.jpg", requestBody);
        serviceApi.put_ocr_data(fileToupload).enqueue(new Callback<OcrResponse>() {
            @Override
            public void onResponse(Call<OcrResponse> call, Response<OcrResponse> response) {
                OcrResponse result = response.body();
                if (response.isSuccessful()) {
                    String result_body = new Gson().toJson(response.body());
                    System.out.println(result_body + " = 이미지");
                    System.out.println(result.toString() + "이미지");
                    System.out.println("PUT 성공");
                } else {
                    System.out.println("이미지 업데이트 실패");
                }
            }

            @Override
            public void onFailure(Call<OcrResponse> call, Throwable throwable) {
                System.out.println(throwable.getMessage());
            }
        });

    }

    //OCR의 결과를 Get 하는 함수

    public void get_ocr_data() {
        final TextView ocrtext = findViewById(R.id.cameraOCR);
        final Call<List<Ocrdata>> getCall = serviceApi.get_ocr_data();
        getCall.enqueue(new Callback<List<Ocrdata>>() {
            @Override
            public void onResponse(Call<List<Ocrdata>> call, Response<List<Ocrdata>> response) {
                String result = "";
                if (response.isSuccessful()) {
                    List<Ocrdata> ocrdata = response.body();
                    for (Ocrdata item : ocrdata) {

                        result += "/" + item.getTitle();
                    }

                    String str = result.substring(result.lastIndexOf("/")+1, result.length());
                    if (str != "null") {
                        System.out.println("str : " + str);
                        System.out.println("OCR 판독 결과값 : " + result);
                        voice.TTS(str + " 문자가 인식 됨");
                        ocrtext.setText("문자인식 : "+ str);
                        Toast.makeText(getApplicationContext(), "이정표 : " + result, Toast.LENGTH_LONG);
                    } else if (str == "/null") {
                        voice.TTS("문자가 인식 되지 않음.");
//                        get_ocr_data();
                    }

                } else {
                    System.out.println("문자 받아오기 실패");
                }
            }


            @Override
            public void onFailure(Call<List<Ocrdata>> call, Throwable throwable) {

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
        mMinewBeaconManager = mMinewBeaconManager.getInstance(this);
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
        //2번째 들어갔을때 둘다 기존

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
//                            System.out.println("on range UUID : " + minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue());
//                            System.out.println("on range RSSI : " + minewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_RSSI).getStringValue());
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
// 기존의 비콘 = BeaconName_clone new 비콘 = nBeacon
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

