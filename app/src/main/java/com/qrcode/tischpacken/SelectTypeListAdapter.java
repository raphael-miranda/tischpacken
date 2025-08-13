package com.qrcode.tischpacken;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SelectTypeListAdapter extends RecyclerView.Adapter<SelectTypeListAdapter.SelectTypeViewHolder> {

    private JSONArray list;
    private ArrayList<HashMap<String, String>> arrSelectedTypes = new ArrayList<>();

    public SelectTypeListAdapter(JSONArray list) {
        this.list = list;

        arrSelectedTypes = new ArrayList<>();

        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject object = list.getJSONObject(i);
                String partNr = object.getString(Constants.PART_NUMBER);
                JSONArray arrTypes = object.getJSONArray(Constants.TYPE);
                String firstType = arrTypes.getString(0);
                HashMap<String, String> selectedType = new HashMap<>();
                selectedType.put(Constants.PART_NUMBER, partNr);
                selectedType.put(Constants.TYPE, firstType);
                arrSelectedTypes.add(selectedType);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }

    public static class SelectTypeViewHolder extends RecyclerView.ViewHolder {
        TextView txtPartNr;
        Spinner typeSpinner;

        public SelectTypeViewHolder(View itemView) {
            super(itemView);
            txtPartNr = itemView.findViewById(R.id.txtPartNr);
            typeSpinner = itemView.findViewById(R.id.typeSpinner);
        }
    }

    @Override
    public SelectTypeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_select_type, parent, false);
        return new SelectTypeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SelectTypeViewHolder holder, int position) {

        try {
            JSONObject object = list.getJSONObject(position);
            String partNr = object.getString(Constants.PART_NUMBER);
            JSONArray arrTypes = object.getJSONArray(Constants.TYPE);

            List<String> items = new ArrayList<>();
            for (int i = 0; i < arrTypes.length(); i++) {
                String type = arrTypes.getString(i);
                items.add(type);
            }

            holder.txtPartNr.setText(partNr);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    holder.itemView.getContext(),
                    android.R.layout.simple_spinner_item,
                    items
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.typeSpinner.setAdapter(adapter);
            holder.typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position1, long id) {
                    String selectedItem = adapterView.getItemAtPosition(position1).toString();
                    HashMap<String, String> selectedType = arrSelectedTypes.get(position);
                    selectedType.put(Constants.TYPE, selectedItem);
                    arrSelectedTypes.set(position, selectedType);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return list.length();
    }

    public ArrayList<HashMap<String, String>> getArrSelectedTypes() {
        return arrSelectedTypes;
    }

}
