 package org.tensorflow.demo.vision_module;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class Service {
    private double longitude;
    private double latitude;
    private String source_Station;
    private String source_Exit;
    private String dest_Station;
    private String dest_Exit;
    private String way;
    private String nextWay;
    private float azimuth;
    private int sectorArraySize;
    private boolean readyFlag;
    private int matchingFlag;
    private int userSectorNum;
    public int score;
    public int idx;
    private int cur_Idx;
    // 사용자가 현재 찾아갈 섹터
    private Sector current_Sector;
    //private jsonObject Array
    private ArrayList<Sector> sectorArrayList;
    //instances data structure class;
    private ArrayList<Sector> path;

    public Service(){
        this.sectorArrayList = new ArrayList<Sector>();
        this.path = new ArrayList<Sector>();
        this.current_Sector = new Sector();
    }



    public void setLongitude(double longitude){
        this.longitude = longitude;
    }

    public void setLatitude(double latitude){
        this.latitude = latitude;
    }

    public void setSource_Station(String source_Station){ this.source_Station = source_Station; }



    public void setSource_Exit(String source_Exit){ this.source_Exit = source_Exit; }

    public void setDest_Station(String dest_Station){ this.dest_Station = dest_Station; }

    public void setDest_Exit(String dest_Exit){ this.dest_Exit= dest_Exit; }

    public void setAzimuth(float azimuth) {this.azimuth = azimuth;}

    public void setCurrent_Sector(int number) { this.current_Sector = this.path.get(number); }



    public double getLongitude(){
        return this.longitude;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public  String getSource_Station() {return this.source_Station;}


    public String getDest_Station() {return this.dest_Station;}



    public float getAzimuth() { return this.azimuth;}

    public Sector getCurrent_Sector() { return this.current_Sector; }

    public String getWay() { return way; }

    public String getNextWay() { return nextWay; }


}


