package com.qrcode.tischpacken;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;



public class PlanListAdapter extends RecyclerView.Adapter<PlanListAdapter.PlanViewHolder> {


    public interface OnSkipButtonClickListener {
        void onSkipButtonClick(int position, HashMap<String, String> item);
    }

    ArrayList<HashMap<String, String>> list;
    private Set<Integer> selectedPositions = new HashSet<>();
    private OnSkipButtonClickListener listener;

    public PlanListAdapter(ArrayList<HashMap<String, String>> list,
                           Set<Integer> selectedPositions,
                           OnSkipButtonClickListener listener) {
        this.list = list;
        this.selectedPositions = selectedPositions;
        this.listener = listener;
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView txtPartNumber;
        TextView txtType;
        TextView txtNoOfCarton;
        TextView txtScanCounter;
        ImageButton btnSkip;

        public PlanViewHolder(View itemView) {
            super(itemView);
            txtPartNumber = itemView.findViewById(R.id.txtPartNumber);
            txtType = itemView.findViewById(R.id.txtType);
            txtNoOfCarton = itemView.findViewById(R.id.txtNoOfCarton);
            txtScanCounter = itemView.findViewById(R.id.txtScanCounter);
            btnSkip = itemView.findViewById(R.id.btnSkip);
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
        holder.txtScanCounter.setText(localList.getOrDefault(Constants.SCAN_COUNTER, "0"));

        // Apply background if selected
        if (selectedPositions.contains(position)) {
            holder.itemView.setBackgroundResource(R.color.green);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }

        holder.btnSkip.setOnClickListener(view -> {
            if (listener != null) {
                listener.onSkipButtonClick(position, localList);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }



}
