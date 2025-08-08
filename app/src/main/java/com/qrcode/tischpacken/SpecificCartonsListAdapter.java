package com.qrcode.tischpacken;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class SpecificCartonsListAdapter extends RecyclerView.Adapter<SpecificCartonsListAdapter.SpecificCartonViewHolder> {

    private ArrayList<String> list;
    private ArrayList<String> scannedCartonNrs;


    public SpecificCartonsListAdapter(ArrayList<String> list, ArrayList<String> scannedCartonNrs) {
        this.list = list;
        this.scannedCartonNrs = scannedCartonNrs;
    }

    public static class SpecificCartonViewHolder extends RecyclerView.ViewHolder {
        TextView txtCartonNr;
        CheckBox chbStatus;


        public SpecificCartonViewHolder(View itemView) {
            super(itemView);
            txtCartonNr = itemView.findViewById(R.id.txtCartonNr);
            chbStatus = itemView.findViewById(R.id.chbStatus);
        }
    }

    @Override
    public SpecificCartonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_specific_carton, parent, false);
        return new SpecificCartonViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SpecificCartonViewHolder holder, int position) {
        String cartonNr = list.get(position);

        holder.txtCartonNr.setText(cartonNr);
        if (scannedCartonNrs.contains(cartonNr)) {
            holder.chbStatus.setText("Scanned");
            holder.chbStatus.setChecked(true);
        } else {
            holder.chbStatus.setText("Not Scanned");
            holder.chbStatus.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}
