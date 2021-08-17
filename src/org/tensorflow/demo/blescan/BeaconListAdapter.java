package org.tensorflow.demo.blescan;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.minew.beacon.BeaconValueIndex;
import com.minew.beacon.MinewBeacon;


import org.tensorflow.demo.R;

import java.util.ArrayList;
import java.util.List;


public class BeaconListAdapter extends RecyclerView.Adapter<BeaconListAdapter.MyViewHolder> {

    private List<MinewBeacon> mMinewBeacons = new ArrayList<>();

    public String getMyuid() {
        return myuid;
    }

    public void setMyuid(String myuid) {
        this.myuid = myuid;
    }

    private String myuid;




    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = View.inflate(parent.getContext(), R.layout.main_item, null);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        holder.setDataAndUi(mMinewBeacons.get(position));
    }

    @Override
    public int getItemCount() {
        if (mMinewBeacons != null) {
            return mMinewBeacons.size();
        }
        return 0;
    }

    public void setData(List<MinewBeacon> minewBeacons) {
        this.mMinewBeacons = minewBeacons;

//        notifyItemRangeChanged(0,minewBeacons.size());
        notifyDataSetChanged();

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
            notifyItemRangeRemoved(startPosition, preSize);
        }
        this.mMinewBeacons.addAll(newItems);
        notifyItemRangeChanged(startPosition, newItems.size());
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView mDevice_mac;
        private MinewBeacon mMinewBeacon;
        private final TextView mDevice_temphumi;
        private final TextView mDevice_name;
        private final TextView mDevice_uuid;
        private final TextView mDevice_other;

//        String[] myuuid = new String[3];
        String[] mymac= new String[3];
        String[] myname= new String[3];

        ArrayList<String> myuid = new ArrayList<String>();
        public MyViewHolder(View itemView) {
            super(itemView);
            mDevice_name = (TextView) itemView.findViewById(R.id.device_name);
            mDevice_uuid = (TextView) itemView.findViewById(R.id.device_uuid);
            mDevice_other = (TextView) itemView.findViewById(R.id.device_other);
            mDevice_temphumi = (TextView) itemView.findViewById(R.id.device_temphumi);
            mDevice_mac = (TextView) itemView.findViewById(R.id.device_mac);
        }

        public void setDataAndUi(MinewBeacon minewBeacon) {
            mMinewBeacon = minewBeacon;
            mDevice_name.setText(mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Name).getStringValue());
            mDevice_mac.setText(mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_MAC).getStringValue());

//            String uid= mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue();
            myuid.add(mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue());



            myuid.add(mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue()) ;
            System.out.println("myuid : "+ myuid);



            setMyuid(mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_UUID).getStringValue());
//            myuid= uuid값이 들어가있다.


            String battery = mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_BatteryLevel).getStringValue();
            int batt = Integer.parseInt(battery);
            if (batt > 100) {
                batt = 100;
            }

            String format = String.format("Major:%s Minor:%s Rssi:%s Battery:%s",
                    mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Major).getStringValue(),
                    mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Minor).getStringValue(),
                    mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_RSSI).getStringValue(),
                    batt + "");
            mDevice_other.setText(format);

            if (mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Humidity).getFloatValue() == 0 &&
                    mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Temperature).getFloatValue() == 0) {
                mDevice_temphumi.setVisibility(View.GONE);
            } else {
                mDevice_temphumi.setVisibility(View.VISIBLE);
                String temphumi = String.format("Temperature:%s ℃   Humidity:%s ",
                        mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Temperature).getFloatValue() + "",
                        mMinewBeacon.getBeaconValue(BeaconValueIndex.MinewBeaconValueIndex_Humidity).getFloatValue() + "");

                mDevice_temphumi.setText(temphumi + "%");
            }

        }
    }
}
