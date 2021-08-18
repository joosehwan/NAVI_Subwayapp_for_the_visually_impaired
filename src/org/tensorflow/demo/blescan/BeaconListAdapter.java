package org.tensorflow.demo.blescan;



import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.minew.beacon.BeaconValueIndex;
import com.minew.beacon.MinewBeacon;


import org.tensorflow.demo.R;

import java.util.ArrayList;
import java.util.List;


public class BeaconListAdapter  {
    private List<MinewBeacon> mMinewBeacons = new ArrayList<>();


    public void setData(List<MinewBeacon> minewBeacons) {
        this.mMinewBeacons = minewBeacons;

    }

    public void setItems(List<MinewBeacon> newItems) {

        int startPosition = 0;
        int preSize = 0;
        if (this.mMinewBeacons != null) {
            preSize = this.mMinewBeacons.size();

        }

    }

}
