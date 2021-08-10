package org.tensorflow.demo.data;

import android.widget.Toast;

import com.google.gson.Gson;

import org.tensorflow.demo.network.ServiceApi;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

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
    public static String Src_gpsX;
    public static String Src_gpsY;
    public static String Src_Station_name;
    public static String Dst_Station_name;
    String transfer_station;

    public String getTransfer_station() {
        return transfer_station;
    }

    public void setTransfer_station(String transfer_station) {
        this.transfer_station = transfer_station;
    }


    public String getDst_gpsX() {
        return Dst_gpsX;
    }

    public void setDst_gpsX(String dst_gpsX) {
        Dst_gpsX = dst_gpsX;
    }

    public String getDst_gpsY() {
        return Dst_gpsY;
    }

    public void setDst_gpsY(String dst_gpsY) {
        Dst_gpsY = dst_gpsY;
    }


    public static String getSrc_gpsX() {
        return Src_gpsX;
    }

    public static String getSrc_gpsY() {
        return Src_gpsY;
    }

    public static String getSrc_Station_name() {
        return Src_Station_name;
    }


    public static void setSrc_gpsX(String src_gpsX) {
        Src_gpsX = src_gpsX;
    }

    public static void setSrc_gpsY(String src_gpsY) {
        Src_gpsY = src_gpsY;
    }

    public static void setSrc_Station_name(String src_Station_name) {
        Src_Station_name = src_Station_name;
    }


    public static String getDst_Station_name() {
        return Dst_Station_name;
    }

    public static void setDst_Station_name(String dst_Station_name) {
        Dst_Station_name = dst_Station_name;
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
        int count = 0;

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

                        }// 첫번째 검색결과
                        else if (tag.equals("fname")) {

                            buffer.append("fname : ");
                            xpp.next();
                            buffer.append(xpp.getText());//addr 요소의 TEXT 읽어와서 문자열버퍼에 추가
                            buffer.append("\n"); //줄바꿈 문자 추가
                        } else if (tag.equals("fx")) {

                            buffer.append("fx : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n");
                        } else if (tag.equals("fy")) {

                            buffer.append("fy :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpId
                            buffer.append("\n");
                        } else if (tag.equals("routeNm")) {
                            buffer.append("routeNm :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");

                        } else if (tag.equals("tname")) {
                            buffer.append("tname :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                        } else if (tag.equals("tx")) {
                            buffer.append("tx :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                        } else if (tag.equals("ty")) {
                            buffer.append("ty :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                        } else if (tag.equals("time")) {
                            buffer.append("time :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                        }
                        break;

                    case XmlPullParser.TEXT:
                        break;

                    case XmlPullParser.END_TAG:
                        tag = xpp.getName(); //태그 이름 얻어오기

                        if (tag.equals("itemList")) buffer.append("\n\n\n");// 첫번째 검색결과종료..줄바꿈
                        break;
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

    //지하철 정보를 얻어오는 함수
    public String get_Src_XmlData(String location) {
        location = URLEncoder.encode(location);


        StringBuffer buffer = new StringBuffer();
        String queryUrl = "http://ws.bus.go.kr/api/rest/pathinfo/getLocationInfo"//요청 URL
                + "?ServiceKey=" + key
                + "&stSrch=" + location;

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
                            buffer.append("Src_gpsX : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            Src_gpsX = xpp.getText();

                            buffer.append("\n"); //줄바꿈 문자 추가
                        } else if (tag.equals("gpsY")) {
                            buffer.append("Src_gpsY : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            Src_gpsY = xpp.getText();

                            buffer.append("\n");
                        } else if (tag.equals("poiId")) {
                            buffer.append("poiId :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpId
                            buffer.append("\n");
                        } else if (tag.equals("poiNm")) {
                            buffer.append("poiNm :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                            if (xpp.getText().contains("번출구")) {
                                Src_Station_name = xpp.getText();
                                count = 1;
                                if (count == 1) {
                                    buffer.append("확정 :  ");
                                    setSrc_gpsX(Src_gpsX);
                                    setSrc_gpsY(Src_gpsY);
                                    setSrc_Station_name(Src_Station_name);
                                    xpp.next();
                                    count += 1;
                                }
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

    //지하철 정보를 얻어오는 함수
    public String get_Dst_XmlData(String location) {
        location = URLEncoder.encode(location);


        StringBuffer buffer = new StringBuffer();

        String queryUrl = "http://ws.bus.go.kr/api/rest/pathinfo/getLocationInfo"//요청 URL
                + "?ServiceKey=" + key
                + "&stSrch=" + location;

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
                            buffer.append("Dst_gpsX : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            Dst_gpsX = xpp.getText();


                            buffer.append("\n"); //줄바꿈 문자 추가
                        } else if (tag.equals("gpsY")) {
                            buffer.append("Dst_gpsY : ");
                            xpp.next();
                            buffer.append(xpp.getText());
                            Dst_gpsY = xpp.getText();

                            buffer.append("\n");
                        } else if (tag.equals("poiId")) {
                            buffer.append("poiId :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpId
                            buffer.append("\n");
                        } else if (tag.equals("poiNm")) {
                            buffer.append("poiNm :");
                            xpp.next();
                            buffer.append(xpp.getText());//cpNm
                            buffer.append("\n");
                            if (xpp.getText().contains("번출구")) {
                                Dst_Station_name = xpp.getText();
                                count = 1;
                                if (count == 1) {
                                    buffer.append("확정 :  ");
                                    setDst_gpsX(Dst_gpsX);
                                    setDst_gpsY(Dst_gpsY);
                                    setDst_Station_name(Dst_Station_name);
                                    xpp.next();
                                    count += 1;
                                }
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
