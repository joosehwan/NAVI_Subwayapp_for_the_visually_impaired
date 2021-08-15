package org.tensorflow.demo.data;

import android.widget.Toast;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.tensorflow.demo.network.ServiceApi;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Subwayapi {
    //restapi 서버 통신 객체 선언
    private ServiceApi serviceApi;

    //공공데이터 인증키
    final static String key = "zWzMth1ANw2%2F4ne5OjB8q8nqI4E%2Bzd6niSgLNpkcx1Y8IaSzo8fbu6IaR%2FfDtQhHpldYIdgtQwna%2FdXSCvgkHg%3D%3D";

    private String Dst_gpsX;
    private String Dst_gpsY;

    public String getDst_gpsX2() {
        return Dst_gpsX2;
    }


    public String getDst_gpsY2() {
        return Dst_gpsY2;
    }

    private String Dst_gpsX2;
    private String Dst_gpsY2;
    public static String Src_gpsX;
    public static String Src_gpsY;
    public static String Src_gpsX2;
    public static String Src_gpsY2;

    public static String getSrc_gpsX2() {
        return Src_gpsX2;
    }

    public static void setSrc_gpsX2(String src_gpsX2) {
        Src_gpsX2 = src_gpsX2;
    }

    public static String getSrc_gpsY2() {
        return Src_gpsY2;
    }

    public static void setSrc_gpsY2(String src_gpsY2) {
        Src_gpsY2 = src_gpsY2;
    }


    public static void setSrc_Station_name2(String src_Station_name2) {
        Src_Station_name2 = src_Station_name2;
    }

    public static String Src_Station_name2;
    public static String Src_Station_name;
    public static String Dst_Station_name;

    public static String Dst_Station_name2;

    //처음 받을 배열리스트
    ArrayList<String> tr_Fname = new ArrayList<String>();
    ArrayList<String> tr_Tname = new ArrayList<String>();
    ArrayList<String> tr_RouteNm = new ArrayList<String>();
    ArrayList<String> tr_Time = new ArrayList<String>();
    //저장을 위한 clone 리스트
    ArrayList<String> tr_Fname_clone = new ArrayList<String>();
    ArrayList<String> tr_Tname_clone = new ArrayList<String>();
    ArrayList<String> tr_RouteNm_clone = new ArrayList<String>();
    ArrayList<String> tr_Time_clone = new ArrayList<String>();


    public String getTransfer_tts() {
        return transfer_tts;
    }

    public void setTransfer_tts(String transfer_tts) {
        this.transfer_tts = transfer_tts;
    }

    String transfer_tts = "";


    public static String getSrc_Station_name() {
        return Src_Station_name;
    }


    public static String getDst_Station_name() {
        return Dst_Station_name;
    }


    // 열차위치역과 열차번호 받는 함수
    public void request_Getsubwaynum() {
        Call<List<TrainNum>> getCall = serviceApi.get_trainnum();
        getCall.enqueue(new Callback<List<TrainNum>>() {
            @Override
            public void onResponse(Call<List<TrainNum>> call, Response<List<TrainNum>> response) {
                if (response.isSuccessful()) {
                    List<TrainNum> trainNumList = response.body();
                    String result = "";
                    for (TrainNum item : trainNumList) {
                        result += "열차위치역 : " + item.getTrainlocation() + "\n"
                                + "열차번호 : " + item.getTrainnum() + "\n";
                    }
                }
            }

            @Override
            public void onFailure(Call<List<TrainNum>> call, Throwable throwable) {
            }
        });
    }

    public void post_Time_Src(String nowtime, String Src_station) {
        TrainNum tr = new TrainNum();
        tr.setNowtime(nowtime);
        tr.setSrcstation(Src_station);
        Call<TrainNum> postCall = serviceApi.post_trainnum(tr);
        postCall.enqueue(new Callback<TrainNum>() {
            @Override
            public void onResponse(Call<TrainNum> call, Response<TrainNum> response) {
                TrainNum trainNum = response.body();
                System.out.println("전체 바디=" + new Gson().toJson(response.body()));
                System.out.println("responsebody =" + trainNum);
            }

            @Override
            public void onFailure(Call<TrainNum> call, Throwable throwable) {
            }
        });
    }

    //지하철 환승데이터를 받는 함수
    public String gettransfer(String src_gpsX, String src_gpsY, String dst_gpsX, String dst_gpsY) {

        StringBuffer buffer = new StringBuffer();


        String queryUrl = "http://ws.bus.go.kr/api/rest/pathinfo/getPathInfoBySubway?ServiceKey=" + key
                + "&startX=" + src_gpsX + "&startY=" + src_gpsY + "&endX=" + dst_gpsX + "&endY=" + dst_gpsY;

        try {
            URL url = new URL(queryUrl);//문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is = url.openStream(); //url위치로 입력스트림 연결

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new InputStreamReader(is, "UTF-8")); //inputstream 으로부터 xml 입력받기

            String tag;

            xpp.next();
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {

                switch (eventType) {

                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("파싱 시작...\n\n");
                        break;

                    case XmlPullParser.START_TAG:

                        tag = xpp.getName();//태그 이름 얻어오기
                        if (tag.equals("itemList")) {
//                            buffer.append("--------------------------");
                            xpp.next();
//                            buffer.append("\n"); //줄바꿈 문자 추가
                        }// 첫번째 검색결과
                        else if (tag.equals("pathList")) {
//                            buffer.append("------------");
                            xpp.next();
//                            buffer.append("\n"); //줄바꿈 문자 추가

                        } else if (tag.equals("fname")) {

//                            buffer.append("fname : ");
                            xpp.next();
//                            buffer.append(xpp.getText());//addr 요소의 TEXT 읽어와서 문자열버퍼에 추가
                            tr_Fname.add(xpp.getText());
//                            buffer.append("\n"); //줄바꿈 문자 추가

                        } else if (tag.equals("routeNm")) {

//                            buffer.append("routeNm :");
                            xpp.next();
//                            buffer.append(xpp.getText());//cpNm
                            tr_RouteNm.add(xpp.getText());

                            buffer.append("\n");
                        } else if (tag.equals("tname")) {
//                            buffer.append("tname :");
                            xpp.next();
//                            buffer.append(xpp.getText());//cpNm
                            tr_Tname.add(xpp.getText());
                            buffer.append("\n");

                        } else if (tag.equals("time")) {
//                            buffer.append("time :");
                            xpp.next();
//                            buffer.append(xpp.getText());//cpNm
                            tr_Time.add(xpp.getText());
//                            buffer.append("\n");
                            tr_Time.add("/");
                            tr_RouteNm.add("/");
                            tr_Fname.add("/");
                            tr_Tname.add("/");
                            tr_Fname_clone = (ArrayList<String>) tr_Fname.clone();
                            System.out.println(tr_Fname_clone);
                            tr_RouteNm_clone = (ArrayList<String>) tr_RouteNm.clone();
                            tr_Tname_clone = (ArrayList<String>) tr_Tname.clone();
                            tr_Time_clone = (ArrayList<String>) tr_Time.clone();

                        }
                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xpp.getName(); //태그 이름 얻어오기


                        if (tag.equals("itemList")) buffer.append("\n\n");// 첫번째 검색결과종료..줄바꿈

                        break;

                }

                eventType = xpp.next();
            }

        } catch (Exception e) {
            // TODO Auto-generated catch blocke.printStackTrace();
            System.out.println(e.getMessage());
        }
        int find_slash = tr_Fname_clone.indexOf("/");
        System.out.println("slash " + find_slash);
        tr_Fname.clear();
        tr_RouteNm.clear();
        tr_Time.clear();
        tr_Tname.clear();

        ArrayList<String> tr_Fname_sliced = new ArrayList<String>(tr_Fname_clone.subList(0, tr_Fname_clone.indexOf("/")));
        ArrayList<String> tr_RouteNm_sliced = new ArrayList<String>(tr_RouteNm_clone.subList(0, tr_RouteNm_clone.indexOf("/")));
        ArrayList<String> tr_Tname_sliced = new ArrayList<String>(tr_Tname_clone.subList(0, tr_Tname_clone.indexOf("/")));
        ArrayList<String> tr_Time_sliced = new ArrayList<String>(tr_Time_clone.subList(0, tr_Time_clone.indexOf("/")));

        System.out.println(tr_RouteNm_sliced);
        System.out.println(tr_Fname_sliced);
        System.out.println(tr_Tname_sliced);
        System.out.println(tr_Time_sliced);

        tr_Fname_clone.clear();
        tr_RouteNm_clone.clear();
        tr_Tname_clone.clear();
        tr_Time_clone.clear();

        for (int i = 0; i < tr_Fname_sliced.size(); i++) {
            System.out.println(tr_RouteNm_sliced.get(i) + " " + tr_Fname_sliced.get(i) + " 탑승 후  \n\n" + tr_Tname_sliced.get(i) + " 하차 \n\n");
            transfer_tts += (tr_RouteNm_sliced.get(i) + tr_Fname_sliced.get(i) + " 탑승 후 " + tr_Tname_sliced.get(i) + " 하차 \n\n");
        }
        System.out.println("예상 소요시간은 " + tr_Time_sliced.get(0) + "분 입니다.");
        transfer_tts += ("예상 소요시간은" + tr_Time_sliced.get(0) + "분 입니다.");
        if (transfer_tts.isEmpty() == true) {
            transfer_tts = "경로설정에러";
        }


        buffer.append("파싱 끝\n");


        return buffer.toString();//StringBuffer 문자열 객체 반환


    }

    //지하철 정보를 얻어오는 함수
    public String get_Src_XmlData(@NotNull String location) {

        if (location.contains("역") != true) {
            location += "역";
        }
        System.out.println("location : " + location);

        System.out.println("location : " + location);
        StringBuffer buffer = new StringBuffer();
        String queryUrl = "http://ws.bus.go.kr/api/rest/pathinfo/getLocationInfo"//요청 URL
                + "?ServiceKey=" + key
                + "&stSrch=" + URLEncoder.encode(location);

        try {
            URL url = new URL(queryUrl);//문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is = url.openStream(); //url위치로 입력스트림 연결

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new InputStreamReader(is, "UTF-8")); //inputstream 으로부터 xml 입력받기

            String tag;

            xpp.next();
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                int count = 0;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("\n\n파싱 시작...\n\n");


                        break;

                    case XmlPullParser.START_TAG:
                        tag = xpp.getName();//태그 이름 얻어오기

                        if (tag.equals("itemList")) ;// 첫번째 검색결과
                        else if (tag.equals("gpsX")) {
//                            buffer.append("Src_gpsX : ");
                            xpp.next();
//                            buffer.append(xpp.getText());
                            Src_gpsX = xpp.getText();

                            buffer.append("\n"); //줄바꿈 문자 추가
                        } else if (tag.equals("gpsY")) {
//                            buffer.append("Src_gpsY : ");
                            xpp.next();
//                            buffer.append(xpp.getText());
                            Src_gpsY = xpp.getText();

                            buffer.append("\n");
                        } else if (tag.equals("poiId")) {
//                            buffer.append("poiId :");
                            xpp.next();
//                            buffer.append(xpp.getText());//cpId
                            buffer.append("\n");
                        } else if (tag.equals("poiNm")) {
//                            buffer.append("poiNm :");
                            xpp.next();
//                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                            System.out.println("equals " + xpp.getText().equals(location));
                            if (xpp.getText().equals(location) == true) {
                                Src_Station_name = xpp.getText();

                                buffer.append("확정 :  ");

                                Src_gpsX2 = Src_gpsX;
                                Src_gpsY2 = Src_gpsY;
                                Src_Station_name2 = Src_Station_name;
                                setSrc_Station_name2(Src_Station_name2);
                                setSrc_gpsX2(Src_gpsX2);
                                setSrc_gpsY2(Src_gpsY2);

                                xpp.next();


                                break;
                            }
                            buffer.append("\n");
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xpp.getName(); //태그 이름 얻어오기
                        if (tag.equals("itemList")) buffer.append("\n\n\n");// 첫번째 검색결과종료..줄바꿈

                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + eventType);
                }

                eventType = xpp.next();
            }

        } catch (Exception e) {
            // TODO Auto-generated catch blocke.printStackTrace();
            System.out.println(e.getMessage());
        }
        System.out.println("Src_Station_name2 : " + Src_Station_name2);
        System.out.println("Src_gpsX2 : " + Src_gpsX2);
        System.out.println("Src_gpsY2 : " + Src_gpsY2);

        buffer.append("파싱 끝\n");


        return buffer.toString();//StringBuffer 문자열 객체 반환

    }

    //지하철 정보를 얻어오는 함수
    public String get_Dst_XmlData(@NotNull String location) {

        if (location.contains("역") != true) {
            location += "역";
        }
        System.out.println("location : " + location);
        //   location = URLEncoder.encode(location);

        StringBuffer buffer = new StringBuffer();

        String queryUrl = "http://ws.bus.go.kr/api/rest/pathinfo/getLocationInfo"//요청 URL
                + "?ServiceKey=" + key
                + "&stSrch=" + URLEncoder.encode(location);

        try {
            URL url = new URL(queryUrl);//문자열로 된 요청 url을 URL 객체로 생성.
            InputStream is = url.openStream(); //url위치로 입력스트림 연결

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new InputStreamReader(is, "UTF-8")); //inputstream 으로부터 xml 입력받기

            String tag;

            xpp.next();
            int eventType = xpp.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                int count = 0;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        buffer.append("\n\n파싱 시작...\n\n");


                        break;

                    case XmlPullParser.START_TAG:
                        tag = xpp.getName();//태그 이름 얻어오기

                        if (tag.equals("itemList")) ;// 첫번째 검색결과
                        else if (tag.equals("gpsX")) {
                            //    buffer.append("Dst_gpsX : ");
                            xpp.next();
                            //  buffer.append(xpp.getText());
                            Dst_gpsX = xpp.getText();


                            buffer.append("\n"); //줄바꿈 문자 추가
                        } else if (tag.equals("gpsY")) {
                            //   buffer.append("Dst_gpsY : ");
                            xpp.next();
                            // buffer.append(xpp.getText());
                            Dst_gpsY = xpp.getText();

                            buffer.append("\n");
                        } else if (tag.equals("poiId")) {
                           // buffer.append("poiId :");
                            xpp.next();
                         //   buffer.append(xpp.getText());//cpId
                            buffer.append("\n");
                        } else if (tag.equals("poiNm")) {
//                            buffer.append("poiNm :");
                            xpp.next();
//                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                            System.out.println("이퀄스 : " + xpp.getText().equals(location));
                            if (xpp.getText().equals(location) == true) {
                                Dst_Station_name = xpp.getText();

                                buffer.append("확정 :  ");
                              Dst_gpsX2=Dst_gpsX;
                              Dst_gpsY2=Dst_gpsY;
                              Dst_Station_name2=Dst_Station_name;
                                xpp.next();


                                break;
                            }
                            buffer.append("\n");
                        }


                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xpp.getName(); //태그 이름 얻어오기
                        if (tag.equals("itemList")) buffer.append("\n\n\n");// 첫번째 검색결과종료..줄바꿈

                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + eventType);
                }

                eventType = xpp.next();
            }

        } catch (Exception e) {
            // TODO Auto-generated catch blocke.printStackTrace();
            System.out.println(e.getMessage());
        }

        buffer.append("파싱 끝\n");


        return buffer.toString();//StringBuffer 문자열 객체 반환

    }

}
