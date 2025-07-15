package com.qrcode.tischpacken;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PlanListAdapter extends RecyclerView.Adapter<PlanListAdapter.PlanViewHolder> {
    ArrayList<HashMap<String, String>> list;
    private Set<Integer> selectedPositions = new HashSet<>();

    public PlanListAdapter(ArrayList<HashMap<String, String>> list, Set<Integer> selectedPositions) {
        this.list = list;
        this.selectedPositions = selectedPositions;
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView txtPartNumber;
        TextView txtType;
        TextView txtNoOfCarton;
        TextView txtCartonNumber;
        TextView txtScanCounter;

        public PlanViewHolder(View itemView) {
            super(itemView);
            txtPartNumber = itemView.findViewById(R.id.txtPartNumber);
            txtType = itemView.findViewById(R.id.txtType);
            txtNoOfCarton = itemView.findViewById(R.id.txtNoOfCarton);
            txtCartonNumber = itemView.findViewById(R.id.txtCartonNumber);
            txtScanCounter = itemView.findViewById(R.id.txtScanCounter);
        }
    }

    @Override
    public PlanViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_plan, parent, false);
        return new PlanViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PlanViewHolder holder, int position) {
        HashMap<String, String> localList = list.get(position);

        holder.txtPartNumber.setText(localList.getOrDefault(Constants.PART_NUMBER, ""));
        holder.txtType.setText(localList.getOrDefault(Constants.TYPE, ""));
        holder.txtNoOfCarton.setText(localList.getOrDefault(Constants.NO_OF_CARTON, ""));
        holder.txtCartonNumber.setText(localList.getOrDefault(Constants.CARTON_NUMBER, ""));
        holder.txtScanCounter.setText(localList.getOrDefault(Constants.SCAN_COUNTER, "0"));

        // Apply background if selected
        if (selectedPositions.contains(position)) {
            holder.itemView.setBackgroundResource(R.color.green);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}
