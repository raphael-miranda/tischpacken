package com.qrcode.tischpacken;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    TextView txtTitle;
    TextInputEditText txtName;
    AppCompatButton btnNext;

    TextView txtInspectorName, txtInspectorNumber, txtInspectorDate, txtPlannedCartons;

    private RecyclerView planListView;

    private ColorStateList normalColors;
    private final ColorStateList yellowColors = new ColorStateList(
            new int[][]{
                    new int[]{android.R.attr.state_focused}, // Focused
                    new int[]{-android.R.attr.state_enabled}, // Disabled
                    new int[]{} // Default
            },
            new int[]{
                    Color.YELLOW,
                    Color.YELLOW,
                    Color.YELLOW
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txtTitle = findViewById(R.id.txtTitle);
        txtName = findViewById(R.id.txtName);
        btnNext = findViewById(R.id.btnNext);

        txtInspectorName = findViewById(R.id.txtInspectorName);
        txtInspectorNumber = findViewById(R.id.txtInspectorNumber);
        txtInspectorDate = findViewById(R.id.txtInspectorDate);
        txtPlannedCartons = findViewById(R.id.txtPlannedCartons);

        planListView = findViewById(R.id.planListView);
        planListView.setLayoutManager(new LinearLayoutManager(this));


        txtTitle.setText(getString(R.string.title, 0, 0));

        normalColors = txtName.getBackgroundTintList();
        if (txtName.getText().toString().isEmpty()) {
            txtName.setBackgroundTintList(yellowColors);
        } else {
            txtName.setBackgroundTintList(normalColors);
        }
        txtName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String strName = txtName.getText().toString();
                if (strName.isEmpty()) {
                    txtName.setBackgroundTintList(yellowColors);
                    btnNext.setEnabled(true);
                } else {
                    txtName.setBackgroundTintList(normalColors);
                    readPlanExcel(strName);
                    btnNext.setEnabled(true);
                }
            }
        });


        if (!checkPermission()) {
            List<String> permissionsNeeded = new ArrayList<>();
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Request manage external storage permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

        if (checkPermission() &&
                (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R &&
                        Environment.isExternalStorageManager())) {
            // All permissions are granted
            new Thread(this::checkLoggerFolder).start();
        } else {
            Toast.makeText(getApplication(),"You didn't provided all the permissions", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public void checkLoggerFolder(){

        File appFolder = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

        if(!appFolder.exists()){
            boolean isLoggerCreated = appFolder.mkdir();
        }else{
            File exlFile2 = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

            File loginExcel = new File(appFolder, "/plan.xls");
            if(!loginExcel.exists()){
                copyAssets();
            }
        }
    }

    private void copyAssets() {

        AssetManager assetManager = getApplication().getAssets();
        String[] files = null;
        try {
            files = assetManager.list(Constants.FolderName);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fPath = Utils.getMainFilePath(getApplicationContext());

        if (files != null) {
            for (String filename : files) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = assetManager.open(Constants.FolderName + "/"+filename);
                    File outFile = new File(fPath+"/" + Constants.FolderName + "/", filename);
                    out = Files.newOutputStream(outFile.toPath());
                    copyFile(in, out);
                } catch (IOException e) {
                    Log.e("tag", "Failed to copy asset file: " + filename, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            // NOOP
                        }
                    }
                }
            }
        }else{
            Log.e(getClass().getName(), "LOGIN No ASSETS FILES");
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }


    public void readPlanExcel(String inspectorName) {
        ArrayList<ArrayList<String>> cellList = new ArrayList<>();
        int totalNoOfCartons = 0;
        try{
            String FilePath = Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/plan.xls";
            FileInputStream fs = new FileInputStream(FilePath);
            Workbook wb = new HSSFWorkbook(fs);

            Sheet sheet = wb.getSheetAt(0);

            for (Row row: sheet) {
                if (row.getRowNum() == 0) continue;

                ArrayList<String> rowValue= new ArrayList<>();
                Cell inspectorCell = row.getCell(1);

                if (inspectorCell.getStringCellValue().equals(inspectorName)) {
                    for (Cell cell: row) {
                        switch (cell.getCellType()) {
                            case STRING:
                                rowValue.add(cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    rowValue.add(cell.getDateCellValue().toString());
                                } else {
                                    rowValue.add(String.valueOf((int)cell.getNumericCellValue()));
                                    if (cell.getColumnIndex() == 4) {
                                        int noOfCartons = (int)cell.getNumericCellValue();
                                        totalNoOfCartons += noOfCartons;
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    cellList.add(rowValue);
                }
            }

            wb.close();
            fs.close();
        }catch(Exception exp){
            exp.printStackTrace();
        }

        if (cellList.isEmpty()) {
            txtInspectorName.setText("");
            txtInspectorNumber.setText("");
            txtInspectorDate.setText("");
            txtPlannedCartons.setText("");
        } else {
            ArrayList<String> rowValue = cellList.get(0);
            txtInspectorName.setText(rowValue.get(1));
            txtInspectorNumber.setText("");
            txtInspectorDate.setText(rowValue.get(0));
            txtPlannedCartons.setText(String.valueOf(totalNoOfCartons));
        }

        Set<Integer> selectedPositions = new HashSet<>();
        PlanListAdapter planListAdapter = new PlanListAdapter(cellList, selectedPositions);
        planListView.setAdapter(planListAdapter);
    }

}