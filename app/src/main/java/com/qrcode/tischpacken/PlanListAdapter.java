package com.qrcode.tischpacken;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PlanListAdapter extends RecyclerView.Adapter<PlanListAdapter.PlanViewHolder> {
    ArrayList<ArrayList<String>> list;
    private Set<Integer> selectedPositions = new HashSet<>();

    public PlanListAdapter(ArrayList<ArrayList<String>> list, Set<Integer> selectedPositions) {
        this.list = list;
        this.selectedPositions = selectedPositions;
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView txtPartNumber;
        TextView txtType;
        TextView txtNoOfCarton;
        TextView txtCartonNumber;

        public PlanViewHolder(View itemView) {
            super(itemView);
            txtPartNumber = itemView.findViewById(R.id.txtPartNumber);
            txtType = itemView.findViewById(R.id.txtType);
            txtNoOfCarton = itemView.findViewById(R.id.txtNoOfCarton);
            txtCartonNumber = itemView.findViewById(R.id.txtCartonNumber);
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
        ArrayList<String> localList = list.get(position);

        holder.txtPartNumber.setText(localList.get(2));
        holder.txtType.setText(localList.get(3));
        holder.txtNoOfCarton.setText(localList.get(4));
        holder.txtCartonNumber.setText("");

        // Apply background if selected
        if (selectedPositions.contains(position)) {
            holder.itemView.setBackgroundColor(Color.GREEN);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}
