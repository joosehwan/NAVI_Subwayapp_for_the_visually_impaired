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

//RecyclerView.Adapter<BeaconListAdapter.MyViewHolder>
public class BeaconListAdapter {

    public String getMyuid() {
        return myuid;
    }

    public void setMyuid(String myuid) {
        this.myuid = myuid;
    }

    // 배열리스트
    private String myuid;
    private List<MinewBeacon> mMinewBeacons = new ArrayList<>();

    public List<MinewBeacon> getmMinewBeacons() {
        return mMinewBeacons;
    }

    //    @Override
    public int getItemCount() {
        if (mMinewBeacons != null) {
//           System.out.println("mbeacons size = "+mMinewBeacons.size());
            return mMinewBeacons.size();

        }
        return 0;
    }

    public void setData(List<MinewBeacon> minewBeacons) {
        this.mMinewBeacons = minewBeacons;

    }

    public void setItems(List<MinewBeacon> newItems) {
//        validateItems(newItems);


        int startPosition = 0;
        int preSize = 0;
        if (this.mMinewBeacons != null) {
            preSize = this.mMinewBeacons.size();

        }
        if (preSize > 0) {
            this.mMinewBeacons.clear();
        }
        this.mMinewBeacons.addAll(newItems);
    }

}
