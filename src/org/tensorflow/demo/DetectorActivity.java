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

package org.tensorflow.demo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import org.tensorflow.demo.OverlayView.DrawCallback;
import org.tensorflow.demo.data.SubwayData;
import org.tensorflow.demo.data.SubwayResponse;
import org.tensorflow.demo.data.Subwayapi;
import org.tensorflow.demo.data.TrainNum;
import org.tensorflow.demo.env.BorderedText;
import org.tensorflow.demo.env.ImageUtils;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.network.RetrofitClient;
import org.tensorflow.demo.network.ServiceApi;
import org.tensorflow.demo.tracking.MultiBoxTracker;
import org.tensorflow.demo.vision_module.Compass;
import org.tensorflow.demo.vision_module.MyCallback;
import org.tensorflow.demo.vision_module.MyGps;
import org.tensorflow.demo.vision_module.SOTWFormatter;
import org.tensorflow.demo.vision_module.Sector;
import org.tensorflow.demo.vision_module.Service;
import org.tensorflow.demo.vision_module.Voice;
import org.tensorflow.demo.vision_module.senario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class DetectorActivity<Resultlabel> extends CameraActivity implements OnImageAvailableListener {


    private static final Logger LOGGER = new Logger();

    // Configuration values for tiny-yolo-voc. Note that the graph is not included with TensorFlow and
    // must be manually placed in the assets/ directory by the user.
    // Graphs and models downloaded from http://pjreddie.com/darknet/yolo/ may be converted e.g. via
    // DarkFlow (https://github.com/thtrieu/darkflow). Sample command:
    // ./flow --model cfg/tiny-yolo-voc.cfg --load bin/tiny-yolo-voc.weights --savepb --verbalise

    private static final String YOLO_MODEL_FILE = "file:///android_asset/hanium_subway_items.pb";
    private static final int YOLO_INPUT_SIZE = 416;
    private static final String YOLO_INPUT_NAME = "input";
    private static final String YOLO_OUTPUT_NAMES = "output";
    private static final int YOLO_BLOCK_SIZE = 32;
    private String Src_gpsX;
    private String Src_gpsY;
    private String Dst_gpsX;
    private String Dst_gpsY;

    private enum DetectorMode {
        YOLO;
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

    private long lastProcessingTimeMs;
    private long lastProcessingTimeMs1;
    private long lastDetectStartTime = 0;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private Bitmap cropSignBitmap = null;
    private float bitmapWidth = 0;
    private float bitmapHeight = 0;
    private int N = 5; // N * N 사분면

    private static final int BUFFERTIME = 3;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    public MultiBoxTracker tracker;

    private OverlayView trackingOverlay;

    private byte[] luminanceCopy;

    private BorderedText borderedText;

    private RequestQueue requestQueue;
    private LocationRequest locationRequest;
    private MyGps myGps;
    private Service service;
    private Voice voice;
    private Compass compass;
    private SOTWFormatter sotwFormatter;
    private Sector curSector = new Sector(false);
    private boolean dotFlag = false;
    private boolean yoloFirstStartFlag = false;

    public InstanceMatrix instanceMatrix = new InstanceMatrix();
    public static String Src_station;
    public static String Dst_station;
    //  SubPage를 거쳐온 지하철 데이터들. 유지되어야하는 변수들_static
    public static String Src_static = "";
    public static String Dst_static = "";
    public static String transfer_static = "";
    TensorFlowYoloDetector tensorFlowYoloDetector = new TensorFlowYoloDetector();
    TreeSet<String> arr;
    ArrayList<String> Deduplicated_labellist;
    SoundPool soundPool;
    int soundID;
    // 출발역 도착역 api전송하기 위한 변수
    String src_station_data;
    String dst_station_data;
    String transfer_data;
    // api를 통해 받아온 출발역 도착역의 gps좌표변수
    static String src_gps_x;
    static String src_gps_y;
    static String dst_gps_x;
    static String dst_gps_y;
    static boolean isgpsready = false;

    //post로 보낼 src데이터
    String src_post_data;
    String dst_post_data;
    //restapi 서버 통신 객체 선언
    public ServiceApi serviceApi;

    //공공데이터 인증키
    final static String key = "zWzMth1ANw2%2F4ne5OjB8q8nqI4E%2Bzd6niSgLNpkcx1Y8IaSzo8fbu6IaR%2FfDtQhHpldYIdgtQwna%2FdXSCvgkHg%3D%3D";

    //    지하철api 객체선언
    Subwayapi subwayapi = new Subwayapi();
    boolean is_station_perfect = false;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serviceApi = RetrofitClient.getClient().create(ServiceApi.class);

//      subPage값들 받아오기
        Button Path_Settings = findViewById(R.id.input_dest);
        Button readocr = findViewById(R.id.readOCR);
        TextView station_transport = findViewById(R.id.station_transport);
        Button takesubway = findViewById(R.id.takesubway);
        station_transport.bringToFront();

        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        soundID = soundPool.load(this, R.raw.voice_effect, 1);

        final TextView ocrtext = findViewById(R.id.cameracatch);
        TextView detectedClass = findViewById(R.id.cameraclick);
        Button goSub = findViewById(R.id.goto_subpage);

        readocr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


            }
        });
//        탑승할때 버튼
        takesubway.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (src_post_data.isEmpty() != true && dst_post_data.isEmpty() != true) {
                        startPost((new SubwayData(src_post_data, dst_post_data)));
                    }
                } catch (Exception e) {

                }

//                startPost(new SubwayData("먹골", "먹골2"));
                request_Getsubwaynum();
            }
        });

        goSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Subpage.class);
                System.out.println("time : " + getTime());
                intent.putExtra("src", Src_station);
                intent.putExtra("dst", Dst_station);
                intent.putExtra("example", "값확인");
                if (subwayapi.getTransfer_tts() != null) {
                    intent.putExtra("transfer", subwayapi.getTransfer_tts());
                }


                startActivity(intent);

                finish();
//
            }
        });


        detectedClass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    for (String i : Deduplicated_labellist) { //for문을 통한 전체출력
                        System.out.println("제거 후 = " + i);
                    }
                }
                String front = "";
                try {
                    for (int i = 0; i < Deduplicated_labellist.size(); i++) {
                        front += Deduplicated_labellist.get(i) + "  ";
                    }
                } catch (Exception e) {
                    voice.TTS("전방 내용없음.");
                }

                ocrtext.setText(front);
                if (front.isEmpty() == true) {
                    voice.TTS("전방에 장애물이 없습니다");
                    ocrtext.setText("전방에 장애물이 없습니다");
                } else {
                    voice.TTS("전방에" + front + "들이 있습니다.");
                }


//
//
            }
        });

        Intent get_intent = getIntent();    //값을 받아오면 isActFirst = false
        TextView start = findViewById(R.id.station_start);
        TextView destination = findViewById(R.id.station_destination);
//        destination.bringToFront();start.bringToFront();
//        destination.invalidate(); start.invalidate();
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


        // GPS가 꺼져있다면 On Dialog
        createLocationRequest();
        turn_on_GPS_dialog();


        //Gps
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

//                    service.setReadyFlag(true);
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

        addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (!isDebug()) {
                            return;
                        }

                        final Vector<String> lines = new Vector<String>();

                        lines.add("");
                        lines.add("");
                        lines.add("Compass: " + sotwFormatter.format(service.getAzimuth()));
                        lines.add("");
                        lines.add("GPS");
                        lines.add(" Latitude: " + service.getLatitude());
                        lines.add(" Longitude: " + service.getLongitude());
                        lines.add("");
                        lines.add("Src Station: " + service.getSource_Station());
                        lines.add("Dst Station: " + service.getDest_Station());
                        lines.add("");

                        borderedText.drawLines(canvas, 10, canvas.getHeight() - 100, lines);

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
                            voice.TTS("로딩 완료! Vision, 시작 가능합니다.");
                        }
//                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();

                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        DetectorActivity.this.lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        if (bitmapHeight == 0 || bitmapWidth == 0) {
                            DetectorActivity.this.bitmapHeight = croppedBitmap.getHeight();
                            DetectorActivity.this.bitmapWidth = croppedBitmap.getWidth();
//                            Log.e("bitmapSize", "width: " + bitmapWidth);
//                            Log.e("bitmapSize", "height: " + bitmapWidth);
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
                            // dot block이 존재한다면 check
                            Classifier.Recognition result = resultb.clone();
                            if (result.getIdx() == 0) dotFlag = true;
                            curSector.setCurSector(result.getIdx());
                            final RectF location = result.getLocation();

                            instanceMatrix.putRecog(result);

                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }
                        DetectorActivity.this.lastProcessingTimeMs1 += SystemClock.uptimeMillis() - startTime;


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

    public void onFront() {


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

    /* 한글을 영어로 변환 */
    //초성 - 가(의 ㄱ), 날(ㄴ) 닭(ㄷ)
    public static String[] arrChoSungEng = {"k", "K", "n", "d", "D", "r", "m", "b", "B", "s", "S",
            "a", "j", "J", "ch", "c", "t", "p", "h"};

    //중성 - 가(의 ㅏ), 야(ㅑ), 뺨(ㅑ)
    public static String[] arrJungSungEng = {
            "a", "e", "ya", "ae", "eo", "e", "yeo", "e", "o", "wa", "wae", "oe",
            "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    };

    //종성 - 가(없음), 갈(ㄹ)
    public static String[] arrJongSungEng = {"", "k", "K", "ks", "n", "nj", "nh",
            "d", "l", "lg", "lm", "lb", "ls", "lt", "lp", "lh", "m", "b", "bs", "s", "ss",
            "ng", "j", "ch", "c", "t", "p", "h"};

    //단일 자음 - ㄱ,ㄴ,ㄷ,ㄹ... (ㄸ,ㅃ,ㅉ은 단일자음(초성)으로 쓰이지만 단일자음으론 안쓰임)
    public static String[] arrSingleJaumEng = {"r", "R", "rt", "s", "sw", "sg", "e", "E", "f",
            "fr", "fa", "fq", "ft", "fx", "fv", "fg", "a", "q", "Q", "qt", "t", "T", "d", "w", "W",
            "c", "z", "x", "v", "g"};

    //어디 지하철 역인지 파악하는 메소드
    public String recognizeStation(String stt_Station) {
        String resultEng = "", targetStation = "";

//        if (stt_Station.contains("역")) {
//            targetStation = stt_Station.split("역")[0];
//        } else
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

        // transfer_static을 null로 초기화 -> subpage에서 돌아왔을때를 대비

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


                            TrainNum tn = new TrainNum();

//                            subwayapi.post_Src(Src_station);
                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    System.out.println("service.getSource_Station() : " + service.getSource_Station());
                                    System.out.println("service.getDest_Station() : " + service.getDest_Station());

                                    src_station_data = subwayapi.get_Src_XmlData(service.getSource_Station());
                                    dst_station_data = subwayapi.get_Dst_XmlData(service.getDest_Station());


                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            System.out.println("검색된 출발역이름 : " + subwayapi.getSrc_Station_name());
                                            System.out.println(subwayapi.getSrc_Station_name() + "의 gps_X좌표 :" + subwayapi.getSrc_gpsX2());
                                            System.out.println(subwayapi.getSrc_Station_name() + "의 gps_Y좌표 :" + subwayapi.getSrc_gpsY2());

                                            src_gps_x = subwayapi.getSrc_gpsX2();
                                            src_gps_y = subwayapi.getSrc_gpsY2();
                                            src_post_data = subwayapi.getSrc_Station_name();
                                            System.out.println("x1=" + src_gps_x);
                                            System.out.println("y1=" + src_gps_y);

                                            System.out.println("검색된 도착역이름 : " + subwayapi.getDst_Station_name());
                                            System.out.println(subwayapi.getDst_Station_name() + "의 gps_X좌표 :" + subwayapi.getDst_gpsX2());
                                            System.out.println(subwayapi.getDst_Station_name() + "의 gps_Y좌표 :" + subwayapi.getDst_gpsY2());
                                            dst_gps_x = subwayapi.getDst_gpsX2();
                                            dst_gps_y = subwayapi.getDst_gpsY2();
                                            dst_post_data = subwayapi.getDst_Station_name();
                                            System.out.println("x2=" + dst_gps_x);
                                            System.out.println("y2=" + dst_gps_y);
                                            isgpsready = true;
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    transfer_data = subwayapi.gettransfer(src_gps_x, src_gps_y, dst_gps_x, dst_gps_y);
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            System.out.println("환승데이터 : " + transfer_data);

                                                            voice.TTS(subwayapi.getTransfer_tts());
                                                            System.out.println(subwayapi.getTransfer_tts());

                                                        }
                                                    });
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

//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        src_station_data = subwayapi.get_Src_XmlData(Src_station);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
////                                System.out.println(src_station_data);
//
//                                System.out.println("검색된 역이름 : "+subwayapi.getSrc_Station_name());
//                                System.out.println(subwayapi.getSrc_Station_name()+"의 gps_X좌표 :" +subwayapi.getSrc_gpsX());
//                                System.out.println(subwayapi.getSrc_Station_name()+"의 gps_Y좌표 :"+subwayapi.getSrc_gpsY());
//                            }
//                        });
//                    }
//                }).start();

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

    Bitmap cropBitmap(Bitmap bitmap, RectF location) {
        return Bitmap.createBitmap(bitmap, (int) location.left, (int) location.top, (int) (location.right - location.left), (int) (location.bottom - location.top));
    }


    //    볼륨 '하'키를 누르면 서비스가 시작된다
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_BUTTON_L1 || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            this.debug = !this.debug;
            requestRender();
            onSetDebug(debug);
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

            //debugSangsuMapdata();

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    // GPS 꺼져있을 경우 alert dialog
    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //  GPS 켜는 dialog 뛰우기
    protected void turn_on_GPS_dialog() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(DetectorActivity.this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        //GPS get에 실패시 (GPS가 꺼져있는 경우)
        task.addOnFailureListener(DetectorActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(DetectorActivity.this,
                                0x1);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    } finally {

                        myGps.startGps(DetectorActivity.this.service);
                        // GPS를 켜고나면 다시 재부팅하라는 안내가 있어야함
                        // GPS를 중간에
                    }
                }
            }
        });
    }//turn_on_gps end


    @Override
    public void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DetectorActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
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

                }
            }

            @Override
            public void onFailure(Call<SubwayResponse> call, Throwable throwable) {
                System.out.println(throwable.getMessage());

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


    // 열차위치역과 열차번호 받는 함수
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
                        arrival_info += request_trainline_clone.get(i) + " 열차 ";
                        if (request_arrivetime_clone.get(i) / 60 == 0) {
                            arrival_info += " 곧 도착.";
                        } else {
                            arrival_info += request_arrivetime_clone.get(i) / 60 + "분 후 도착.\n";
                        }


                    }
                    System.out.println(arrival_info);
                    voice.TTS(arrival_info);

                }
            }

            @Override
            public void onFailure(Call<List<TrainNum>> call, Throwable throwable) {
                System.out.println(throwable.getMessage());
            }
        });
    }
}