package com.qrcode.tischpacken;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecordViewAdapter extends RecyclerView.Adapter<RecordViewAdapter.RecordViewHolder>{

    ArrayList<ArrayList<String>> list;

    public RecordViewAdapter(ArrayList<ArrayList<String>> list) {
        this.list = list;
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {
        LinearLayout containerView;

        public RecordViewHolder(View itemView) {
            super(itemView);
            containerView = itemView.findViewById(R.id.ll);
        }
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_record, parent, false);
        return new RecordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecordViewHolder holder, int position) {
        ArrayList<String> localList = list.get(position);

        for(int i = 0; i < holder.containerView.getChildCount(); i++){
            ((TextView)(holder.containerView.getChildAt(i))).setText("");
            ((TextView)(holder.containerView.getChildAt(i))).setText(localList.get(i));
        }

    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
