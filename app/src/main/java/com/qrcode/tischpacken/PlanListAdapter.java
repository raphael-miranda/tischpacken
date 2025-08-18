package com.qrcode.tischpacken;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;



public class PlanListAdapter extends RecyclerView.Adapter<PlanListAdapter.PlanViewHolder> {


    public interface OnSkipButtonClickListener {
        void onSkipButtonClick(int position, HashMap<String, String> item);
    }

    private ArrayList<HashMap<String, String>> list;
    private ArrayList<HashMap<String, String>> scannedList;
    private int selectedPosition = -1;
    private OnSkipButtonClickListener listener;


    public PlanListAdapter(ArrayList<HashMap<String, String>> list,
                           int selectedPosition,
                           ArrayList<HashMap<String, String>> scannedList,
                           OnSkipButtonClickListener listener) {
        this.list = list;
        this.scannedList = scannedList;
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView txtPartNumber;
        TextView txtType;
        TextView txtNoOfCarton;
        TextView txtSkipped;
        TextView txtScanCounter;
        ImageButton btnSkip;

        public PlanViewHolder(View itemView) {
            super(itemView);
            txtPartNumber = itemView.findViewById(R.id.txtPartNumber);
            txtType = itemView.findViewById(R.id.txtType);
            txtNoOfCarton = itemView.findViewById(R.id.txtNoOfCarton);
            txtSkipped = itemView.findViewById(R.id.txtSkipped);
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

        String partNr = localList.getOrDefault(Constants.PART_NUMBER, "");
        String type = localList.getOrDefault(Constants.TYPE, "");
        String strNoOfCarton = localList.getOrDefault(Constants.NO_OF_CARTON, "0");
        int noOfCarton = Integer.parseInt(strNoOfCarton);
        holder.txtPartNumber.setText(partNr);
        holder.txtType.setText(type);
        holder.txtNoOfCarton.setText(strNoOfCarton);

        int scannedCounter = 0;
        int skippedCounter = 0;
        for (HashMap<String, String> scannedCarton : scannedList) {
            String scannedPartNr = scannedCarton.getOrDefault(Constants.PART_NUMBER, "");
            String scannedType = scannedCarton.getOrDefault(Constants.TYPE, "");
            if (scannedPartNr.equals(partNr) && type.equals(scannedType)) {
                String strSkippedCounter = scannedCarton.getOrDefault(Constants.SKIP_COUNTER, "0");
                if (strSkippedCounter.isEmpty()) strSkippedCounter = "0";
                int skipped = Integer.parseInt(strSkippedCounter);
                if (skipped > 0) {
                    skippedCounter += skipped;
                } else {
                    scannedCounter += 1;
                }
            }
        }
        holder.txtScanCounter.setText(String.valueOf(scannedCounter));
        if (skippedCounter > 0) {
            holder.txtSkipped.setText(String.valueOf(skippedCounter));
        }

        // Apply background if selected
        if (noOfCarton <= (skippedCounter + scannedCounter)) {
            holder.itemView.setBackgroundResource(R.color.green_light);
            if (selectedPosition == position) {
                holder.itemView.setBackgroundResource(R.color.green);
            }
            holder.btnSkip.setEnabled(false);
        } else {
            if (selectedPosition == position) {
                holder.itemView.setBackgroundResource(R.color.green);
            } else {
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
            holder.btnSkip.setEnabled(true);
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
