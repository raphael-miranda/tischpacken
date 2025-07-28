package com.qrcode.tischpacken;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.share.DiskShare;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements PlanListAdapter.OnSkipButtonClickListener {

    int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    private TextView txtInspectorCounter, txtTotalInspectors;
    private TextInputEditText txtName;
    private AppCompatButton btnNext;

    private TextView txtInspectorName, txtInspectorNumber, txtInspectionDate, txtPlannedCartons;

    private RecyclerView planListView;

    private TextInputEditText txtScan;
    private AppCompatButton btnClear;
    private ImageButton btnSettings, btnUpdate, btnViewRecord, btnSave;

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

    private final ColorStateList redColors = new ColorStateList(
            new int[][]{
                    new int[]{android.R.attr.state_focused}, // Focused
                    new int[]{-android.R.attr.state_enabled}, // Disabled
                    new int[]{} // Default
            },
            new int[]{
                    Color.RED,
                    Color.RED,
                    Color.RED
            }
    );

    ArrayList<HashMap<String, String>> cartonsFromFile = new ArrayList<>();
    ArrayList<HashMap<String, String>> cartonsFromPlan = new ArrayList<>();

    ArrayList<String> controlledParts = new ArrayList<>();

    ArrayList<HashMap<String, String>> scannedList = new ArrayList<>();
    int totalNoOfCartons = 0;

    PlanListAdapter planListAdapter;

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

        txtInspectorCounter = findViewById(R.id.txtInspectorCounter);
        txtTotalInspectors = findViewById(R.id.txtTotalInspectors);

        txtName = findViewById(R.id.txtName);
        btnNext = findViewById(R.id.btnNext);

        txtInspectorName = findViewById(R.id.txtInspectorName);
        txtInspectorNumber = findViewById(R.id.txtInspectorNumber);
        txtInspectionDate = findViewById(R.id.txtInspectionDate);
        txtPlannedCartons = findViewById(R.id.txtPlannedCartons);

        planListView = findViewById(R.id.planListView);
        planListView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(planListView.getContext(), LinearLayoutManager.VERTICAL);
        planListView.addItemDecoration(divider);

        txtScan = findViewById(R.id.txtScan);
        btnClear = findViewById(R.id.btnClear);

        btnSettings = findViewById(R.id.btnSettings);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnViewRecord = findViewById(R.id.btnViewRecord);
        btnSave = findViewById(R.id.btnSave);

        normalColors = txtName.getBackgroundTintList();
        if (txtName.getText().toString().isEmpty()) {
            txtName.setBackgroundTintList(yellowColors);
        }
        if (txtScan.getText().toString().isEmpty()) {
            txtScan.setBackgroundTintList(yellowColors);
        }

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

        btnNext.setOnClickListener(view -> {
            saveRecords();
            clearAll();
        });

        btnClear.setOnClickListener(view -> {
            txtScan.setText("");
        });

        btnSettings.setOnClickListener(view -> {
            showSettingsDialog();
        });

        btnUpdate.setOnClickListener(view -> {
            downloadPlanFromSMB();
        });

        btnViewRecord.setOnClickListener(view -> {
            Intent intent = new Intent(this, RecordViewActivity.class);
            startActivity(intent);
        });

        btnSave.setOnClickListener(view -> {

        });

        showTotalInspectorsCount();

        readControlledParts();
        readCartons();

        initNameInput();
        initContentsInput();
    }

    private void showTotalInspectorsCount() {
        ArrayList<String> inspectorNames = new ArrayList<>();

        try{
            String FilePath = Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/plan.xls";
            FileInputStream fs = new FileInputStream(FilePath);
            Workbook wb = new HSSFWorkbook(fs);

            Sheet sheet = wb.getSheetAt(0);

            for (Row row: sheet) {
                if (row.getRowNum() == 0) continue;

                HashMap<String, String> rowValue= new HashMap<>();
                Cell inspectorCell = row.getCell(1);
                String inspectorName = inspectorCell.getStringCellValue();

                if (!inspectorNames.contains(inspectorName)) {
                    inspectorNames.add(inspectorName);
                }
            }

            wb.close();
            fs.close();
        }catch(Exception exp){
            exp.printStackTrace();
        }

        txtTotalInspectors.setText(String.valueOf(inspectorNames.size()));
    }

    private void readControlledParts() {
        try {
            String filePath = Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/controlledparts.txt";

            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                controlledParts.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCartons() {
        cartonsFromFile = new ArrayList<>();

        try{
            String FilePath = Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/cartons.xlsx";
            FileInputStream fs = new FileInputStream(FilePath);
            Workbook wb = new XSSFWorkbook(fs);

            Sheet sheet = wb.getSheetAt(0);

            for (Row row: sheet) {
                if (row.getRowNum() < 6) continue;

                HashMap<String, String> rowValue= new HashMap<>();

                for (Cell cell: row) {

                    String value = "";

                    switch (cell.getCellType()) {
                        case STRING:
                            value = cell.getStringCellValue();
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                                value = simpleDateFormat.format(date);
                            } else {
                                value = String.valueOf((int)cell.getNumericCellValue());
                            }
                            break;
                        default:
                            break;
                    }

                    if (cell.getColumnIndex() == 1) {
                        rowValue.put(Constants.CT_NR, value);
                    }

                    if (cell.getColumnIndex() == 2) {
                        rowValue.put(Constants.CARTON_TYPE, value);
                    }

                    if (cell.getColumnIndex() == 8) {
                        rowValue.put(Constants.PART_NUMBER, value);
                    }

                    if (cell.getColumnIndex() == 11) {
                        rowValue.put(Constants.UNCHECKED, value);
                    }

                }
                cartonsFromFile.add(rowValue);
            }

            wb.close();
            fs.close();
        } catch(Exception exp){
            exp.printStackTrace();
        }
    }

    private void initNameInput() {

        txtName.requestFocus();

        // disable keyboard
        txtName.setShowSoftInputOnFocus(false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        txtName.post(() -> txtName.setSelection(txtName.getText().length()));

        txtName.setOnKeyListener((view, keyCode, event) -> {

            if (keyCode== KeyEvent.KEYCODE_ENTER)
            {
                // Just ignore the [Enter] key
                return true;
            }
            // Handle all other keys in the default way
            return (keyCode == KeyEvent.KEYCODE_ENTER);
        });

        txtName.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged() {
                String strName = txtName.getText().toString();
                if (strName.isEmpty()) {
                    txtName.setBackgroundTintList(yellowColors);
                } else {
                    checkNameFromExcel(strName);
                }
            }
        });
    }

    private void initContentsInput() {
        txtScan.setText("");
        txtScan.setEnabled(false);

        // disable keybaord
        txtScan.setShowSoftInputOnFocus(false);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        txtScan.post(() -> txtScan.setSelection(txtScan.getText().length()));

        txtScan.setOnKeyListener((view, keyCode, event) -> {

            if (keyCode==KeyEvent.KEYCODE_ENTER)
            {
                // Just ignore the [Enter] key
                return true;
            }
            // Handle all other keys in the default way
            return (keyCode == KeyEvent.KEYCODE_ENTER);
        });
        txtScan.addTextChangedListener(new SimpleTextWatcher() {

            @Override
            public void afterTextChanged() {
                String strCtNr = txtScan.getText().toString();
                int count = strCtNr.split(";").length;

                if(count == 4) {
                    String partNr = strCtNr.split(";")[1];
                    String strDNr = strCtNr.split(";")[2];
                    String strQtty = strCtNr.split(";")[3];
                    String strCartonNr = strCtNr.split(";")[0];

                    try {
                        int qtty = Integer.parseInt(strQtty);
                        HashMap<String, String> scannedCarton = new HashMap<>();
                        scannedCarton.put(Constants.PART_NUMBER, partNr);
                        scannedCarton.put(Constants.QTTY, strQtty);
                        scannedCarton.put(Constants.CT_NR, strCartonNr);
                        scannedCarton.put(Constants.D_NR, strDNr);

                        checkPartNumber(scannedCarton);
                    } catch (NumberFormatException e) {
                        txtScan.setBackgroundTintList(redColors);
                        showInformationDialog("Error", "Invalid Qtty value");
                    }
                }

                if (txtScan.getText().toString().isEmpty()) {
                    txtScan.setBackgroundTintList(yellowColors);
                    btnClear.setEnabled(false);
                } else {
                    btnClear.setEnabled(true);
                }
            }
        });
    }

    private void checkPartNumber(HashMap<String, String> scannedCarton) {

        String partNr = scannedCarton.getOrDefault(Constants.PART_NUMBER, "");

        boolean result = false;
        int selectedPosition = -1;
        HashMap<String, String> matchedCarton = new HashMap<>();

        boolean isPartNumberFounded = false;
        for (int i = 0; i < cartonsFromPlan.size(); i++) {
            matchedCarton = cartonsFromPlan.get(i);
            String partNumber = matchedCarton.getOrDefault(Constants.PART_NUMBER, "");
            if (partNumber.equals(partNr)) {
                isPartNumberFounded = true;

                result = verificationChecks(matchedCarton, scannedCarton);
                if (result) {
                    txtScan.setBackgroundTintList(normalColors);
                    txtScan.setText("");

                    cartonsFromPlan.set(i, matchedCarton);
                    selectedPosition = i;

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
                    String currentDate = simpleDateFormat.format(new Date());
                    scannedCarton.put(Constants.SCAN_DATE, currentDate);
                    scannedCarton.put(Constants.INSPECTOR, matchedCarton.getOrDefault(Constants.INSPECTOR, ""));

                    scannedList.add(matchedCarton);

                    planListAdapter = new PlanListAdapter(cartonsFromPlan, selectedPosition, scannedList, this);
                    planListView.setAdapter(planListAdapter);

                } else {
                    txtScan.setBackgroundTintList(redColors);
                }

                break;
            }
        }
        if (!isPartNumberFounded) {
            txtScan.setBackgroundTintList(redColors);
            showInformationDialog("Error", "Part number not found in plan.");
        }

        checkNext();
    }

    private void checkNext() {
        if (scannedList.size() >= totalNoOfCartons) {
            btnNext.setEnabled(true);
        } else {
            btnNext.setEnabled(false);
        }
    }

    private void clearAll() {
        txtName.setText("");
        txtScan.setText("");
        txtInspectorName.setText("");
        txtInspectorNumber.setText("");
        txtInspectionDate.setText("");
        txtPlannedCartons.setText("");

        cartonsFromPlan.clear();
        scannedList.clear();

        planListAdapter = new PlanListAdapter(cartonsFromPlan, -1, scannedList, this);
        planListView.setAdapter(planListAdapter);
    }

    private boolean verificationChecks(HashMap<String, String> matchedCarton, HashMap<String, String> scannedCarton) {
        String scannedCartonNr = scannedCarton.getOrDefault(Constants.CT_NR, "");
        String strScannedQtty = scannedCarton.getOrDefault(Constants.QTTY, "0");
        int scannedQtty = Integer.parseInt(strScannedQtty);

        String cartonNrs = matchedCarton.getOrDefault(Constants.CT_NR, "");
        String type = matchedCarton.getOrDefault(Constants.TYPE, "");
        String partNumber = matchedCarton.getOrDefault(Constants.PART_NUMBER, "");

        if (!cartonNrs.isEmpty()) {
            String[] ctNrs = cartonNrs.split("\\s*\\s");
            List<String> arrCartonNrs = Arrays.asList(ctNrs);

            if (!arrCartonNrs.contains(scannedCartonNr)) {
                showInformationDialog("Verification Failed", "Scanned carton number is not in the planned carton list.");
                return false;
            }

        }

        // Controlled Part Check (controlledparts.txt)
        if (partNumber != null && !partNumber.isEmpty()) {
            if (controlledParts.contains(partNumber) && type.equals("1st Check")) {

                String inspectorNr = matchedCarton.getOrDefault(Constants.INSPECTOR_NR, "0");
                showInformationDialog("", "Inspector Nr: " + inspectorNr);

                if (inspectorNr.isEmpty() || inspectorNr.equals("0")) {
                    showInformationDialog("Verification Failed", "Carton is not \"JO\" inspected");
                    return false;
                }

            }
        }

        // Carton validation (cartons.xlsx)
        if (type.equals("1st Check") || type.equals("Jig Only")) {
            boolean isChecked = false;
            for (HashMap<String, String> cartonFromFile : cartonsFromFile) {
                if (scannedCartonNr.equals(cartonFromFile.getOrDefault(Constants.CT_NR, ""))) {
                    isChecked = true;
                    String strQtty = cartonFromFile.getOrDefault(Constants.UNCHECKED, "0");
                    try {
                        int qtty = Integer.parseInt(strQtty);
                        if (qtty != scannedQtty) {
                            showInformationDialog("Verification Failed", "The carton is not unchecked.");
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        showInformationDialog("Verification Failed", "The carton is not unchecked.");
                        return false;
                    }

                }
            }
            if (!isChecked) {
                showInformationDialog("Verification Failed", "The carton is not unchecked.");
                return false;
            }
        }

        return true;
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


    public void checkNameFromExcel(String inspectorName) {
        cartonsFromPlan = new ArrayList<>();
        try{
            String FilePath = Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/plan.xls";
            FileInputStream fs = new FileInputStream(FilePath);
            Workbook wb = new HSSFWorkbook(fs);

            Sheet sheet = wb.getSheetAt(0);

            for (Row row: sheet) {
                if (row.getRowNum() == 0) continue;

                HashMap<String, String> rowValue= new HashMap<>();
                Cell inspectorCell = row.getCell(1);

                if (inspectorCell.getStringCellValue().equals(inspectorName)) {
                    for (Cell cell: row) {

                        String value = "";

                        switch (cell.getCellType()) {
                            case STRING:
                                value = cell.getStringCellValue();
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    Date date = cell.getDateCellValue();
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                                    value = simpleDateFormat.format(date);
                                } else {
                                    value = String.valueOf((int)cell.getNumericCellValue());
                                }
                                break;
                            default:
                                break;
                        }

                        if (cell.getColumnIndex() == 0) {
                            rowValue.put(Constants.DATE, value);
                        }
                        if (cell.getColumnIndex() == 1) {
                            rowValue.put(Constants.INSPECTOR, value);
                        }

                        if (cell.getColumnIndex() == 2) {
                            rowValue.put(Constants.PART_NUMBER, value);
                        }

                        if (cell.getColumnIndex() == 3) {
                            rowValue.put(Constants.TYPE, value);
                        }

                        if (cell.getColumnIndex() == 4) {
                            rowValue.put(Constants.NO_OF_CARTON, value);

                            int noOfCartons = (int)cell.getNumericCellValue();
                            totalNoOfCartons += noOfCartons;
                        }

                        if (cell.getColumnIndex() == 5) {
                            rowValue.put(Constants.QTTY, value);
                        }

                        if (cell.getColumnIndex() == 12) {
                            rowValue.put(Constants.CT_NR, value);
                        }

                        if (cell.getColumnIndex() == 13) {
                            rowValue.put(Constants.INSPECTOR_NR, value);
                        }
                    }
//                    rowValue.put(Constants.SCAN_COUNTER, "0");
                    cartonsFromPlan.add(rowValue);
                }
            }

            wb.close();
            fs.close();
        }catch(Exception exp){
            exp.printStackTrace();
        }

        if (cartonsFromPlan.isEmpty()) {
            txtInspectorName.setText("");
            txtInspectorNumber.setText("");
            txtInspectionDate.setText("");
            txtPlannedCartons.setText("");

            txtName.setEnabled(true);
            txtName.setBackgroundTintList(yellowColors);
            showInformationDialog("Error", "Name not found in plan.");
        } else {
            HashMap<String, String> rowValue = cartonsFromPlan.get(0);
            txtInspectorName.setText(rowValue.getOrDefault(Constants.INSPECTOR, ""));
            txtInspectorNumber.setText(rowValue.getOrDefault(Constants.INSPECTOR_NR, ""));
            txtInspectionDate.setText(rowValue.getOrDefault(Constants.DATE, ""));
            txtPlannedCartons.setText(String.valueOf(totalNoOfCartons));

            txtName.setBackgroundTintList(normalColors);
            txtName.setEnabled(false);
            txtScan.setEnabled(true);
            txtScan.requestFocus();
            scannedList = new ArrayList<>();
        }

        planListAdapter = new PlanListAdapter(cartonsFromPlan, -1, scannedList, this);
        planListView.setAdapter(planListAdapter);
    }

    @Override
    public void onSkipButtonClick(int position, HashMap<String, String> item) {
        showSkipDialog(position, item);
    }

    private void showSkipDialog(int position, HashMap<String, String> item) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_skip, null);
        builder.setView(dialogView)
                .setCancelable(true);
        AlertDialog dialog = builder.create();

        NumberPicker skipCounterPicker = dialogView.findViewById(R.id.skipCounterPicker);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        String strMaxNoOfCartons = item.getOrDefault(Constants.NO_OF_CARTON, "0");
        int maxNoOfCartons = Integer.parseInt(strMaxNoOfCartons);

        String selectedPartNr = item.getOrDefault(Constants.PART_NUMBER, "");
        int skippedCounter = 0, scannedCounter = 0;
        for (HashMap<String, String> scannedCarton : scannedList) {
            String scannedPartNr = scannedCarton.getOrDefault(Constants.PART_NUMBER, "");
            if (scannedPartNr.equals(selectedPartNr)) {
                int skipped = Integer.parseInt(scannedCarton.getOrDefault(Constants.SKIP_COUNTER, "0"));
                if (skipped > 0) {
                    skippedCounter += skipped;
                } else {
                    scannedCounter += 1;
                }
            }
        }

        skipCounterPicker.setMinValue(1);
        skipCounterPicker.setMaxValue(maxNoOfCartons - scannedCounter - skippedCounter);
        skipCounterPicker.setWrapSelectorWheel(true);

        btnConfirm.setOnClickListener(view -> {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
            String currentDate = simpleDateFormat.format(new Date());

            HashMap<String, String> skippedItem = new HashMap<>();

            skippedItem.put(Constants.SCAN_DATE, currentDate);
            skippedItem.put(Constants.INSPECTOR, item.getOrDefault(Constants.INSPECTOR, ""));
            skippedItem.put(Constants.CT_NR, "");
            skippedItem.put(Constants.PART_NUMBER, item.getOrDefault(Constants.PART_NUMBER, ""));
            skippedItem.put(Constants.D_NR, "");
            skippedItem.put(Constants.QTTY, item.getOrDefault(Constants.QTTY, ""));
            skippedItem.put(Constants.SCAN_STATUS, "Skipped");
            skippedItem.put(Constants.SKIP_COUNTER, String.valueOf(skipCounterPicker.getValue()));
            scannedList.add(skippedItem);

            planListAdapter = new PlanListAdapter(cartonsFromPlan, -1, scannedList, this);
            planListView.setAdapter(planListAdapter);

            checkNext();

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override public void afterTextChanged(Editable s) {
            afterTextChanged();
        }
        public abstract void afterTextChanged();
    }

    private void showSettingsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_settings, null);
        builder.setView(dialogView)
                .setCancelable(true);
        AlertDialog dialog = builder.create();

        TextInputEditText txtHost = dialogView.findViewById(R.id.txtHostAddress);
        TextInputEditText txtSharedFolder = dialogView.findViewById(R.id.txtSharedFolder);
        TextInputEditText txtUserName = dialogView.findViewById(R.id.txtUserName);
        TextInputEditText txtPassword = dialogView.findViewById(R.id.txtPassword);

        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnTestConnection = dialogView.findViewById(R.id.btnTestConnection);

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        txtHost.setText(sharedPreferences.getString(Constants.SMB_SERVER_ADDRESS, ""));
        txtSharedFolder.setText(sharedPreferences.getString(Constants.SMB_SHARED_FOLDER, ""));
        txtUserName.setText(sharedPreferences.getString(Constants.SMB_USERNAME, ""));
        txtPassword.setText(sharedPreferences.getString(Constants.SMB_PASSWORD, ""));

        btnSave.setOnClickListener(view -> {

            String hostAddress = txtHost.getText().toString();
            String portNumber = txtSharedFolder.getText().toString();
            String username = txtUserName.getText().toString();
            String password = txtPassword.getText().toString();

            SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), MODE_PRIVATE).edit();
            editor.putString(Constants.SMB_SERVER_ADDRESS, hostAddress);
            editor.putString(Constants.SMB_SHARED_FOLDER, portNumber);
            editor.putString(Constants.SMB_USERNAME, username);
            editor.putString(Constants.SMB_PASSWORD, password);
            editor.apply();

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());

        btnTestConnection.setOnClickListener(view -> {
            String hostAddress = txtHost.getText().toString();
            String sharedFolder = txtSharedFolder.getText().toString();
            String username = txtUserName.getText().toString();
            String password = txtPassword.getText().toString();

            testSmbConnection(hostAddress, sharedFolder, username, password);
        });

        dialog.show();
    }

    private void testSmbConnection(String serverIp, String shareName, String username, String password) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // SMB upload logic here
                SMBClient client = new SMBClient();
                try (Connection connection = client.connect(serverIp)) {
                    AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), "");
                    com.hierynomus.smbj.session.Session session = connection.authenticate(ac);
                    DiskShare share = (DiskShare) session.connectShare(shareName);
                    share.close();
                    session.close();
                }

                // Notify success on main thread
                mainHandler.post(() -> {
                    showInformationDialog("Test Success", "Your SMB Server is available!");
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showInformationDialog("Test Failed", "Your SMB Server is unavailable!");
                });
            }
        });
    }

    private void downloadPlanFromSMB() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        String hostname = sharedPreferences.getString(Constants.SMB_SERVER_ADDRESS, "");
        String shareName = sharedPreferences.getString(Constants.SMB_SHARED_FOLDER, "");
        String username = sharedPreferences.getString(Constants.SMB_USERNAME, "");
        String password = sharedPreferences.getString(Constants.SMB_PASSWORD, "");

        if (hostname.isEmpty() || shareName.isEmpty()) {
            showInformationDialog("Error", "Please set the SMB Server");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<String> successFiles = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();

            try {
                // SMB download logic here
                SMBClient client = new SMBClient();
                try (Connection connection = client.connect(hostname)) {
                    AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), "");
                    com.hierynomus.smbj.session.Session session = connection.authenticate(ac);
                    DiskShare share = (DiskShare) session.connectShare(shareName);



                    // Remote SMB file path
                    Map<String, String> fileMap = new HashMap<>();
                    fileMap.put("input/plan.xls", "plan.xls");
                    fileMap.put("tischPacken/cartons.xlsx", "cartons.xlsx");
                    fileMap.put("tischPacken/controlledparts.txt", "controlledparts.txt");

                    for (Map.Entry<String, String> entry: fileMap.entrySet()) {
                        String remotePath = entry.getKey();
                        String localFileName = entry.getValue();

                        try {
                            // Local destination file
                            File localFile = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + localFileName);
                            if (!localFile.exists()) {
                                localFile.getParentFile().mkdirs(); // ensure directory exists
                                localFile.createNewFile();
                            }

                            try (InputStream is = share.openFile(
                                    remotePath,
                                    EnumSet.of(AccessMask.GENERIC_READ),
                                    null,
                                    SMB2ShareAccess.ALL,
                                    SMB2CreateDisposition.FILE_OPEN,
                                    null
                            ).getInputStream();
                                 FileOutputStream fos = new FileOutputStream(localFile)) {

                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                                Log.d("SMB", localFileName + " downloaded successfully.");
                                successFiles.add(localFileName);
                            }
                        } catch (Exception fileEx) {
                            Log.e("SMB", "Failed to download " + remotePath + ": " + fileEx.getMessage());
                            failedFiles.add(localFileName);
                        }
                    }

                    // Close everything safely
                    share.close();
                    session.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                return;
            }

            // Notify success on main thread
            mainHandler.post(() -> {
                String message = "Downloaded: " + successFiles.size() + " file(s)";
                if (!failedFiles.isEmpty()) {
                    message += "\nFailed: " + String.join(", ", failedFiles);
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                readControlledParts();
                readCartons();
            });
        });
    }

    private void saveRecords() {

        try {
            String fileName = getFileName();
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + fileName);

                if (!file.exists()) {
                    createExcelFile(file);
                }

                FileInputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis);
                Sheet sheet = workbook.getSheetAt(0); // or getSheet("SheetName")

                for (HashMap<String, String> rowData : scannedList) {
                    int lastRowNum = sheet.getLastRowNum();
                    Row newRow = sheet.createRow(lastRowNum + 1);

                    Cell dateCell = newRow.createCell(0);
                    dateCell.setCellValue(rowData.getOrDefault(Constants.SCAN_DATE, ""));

                    Cell inspectorCell = newRow.createCell(1);
                    inspectorCell.setCellValue(rowData.getOrDefault(Constants.INSPECTOR, ""));

                    Cell ctNrCell = newRow.createCell(2);
                    ctNrCell.setCellValue(rowData.getOrDefault(Constants.CT_NR, ""));

                    Cell partNrCell = newRow.createCell(3);
                    partNrCell.setCellValue(rowData.getOrDefault(Constants.PART_NUMBER, ""));

                    Cell dNrCell = newRow.createCell(4);
                    dNrCell.setCellValue(rowData.getOrDefault(Constants.D_NR, ""));

                    Cell qttyCell = newRow.createCell(5);
                    qttyCell.setCellValue(rowData.getOrDefault(Constants.QTTY, ""));

                    Cell commentsCell = newRow.createCell(6);
                    commentsCell.setCellValue(rowData.getOrDefault(Constants.SCAN_STATUS, ""));

                }

                fis.close(); // important

                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                workbook.close();
                fos.close();

                Log.d("Excel", "Data appended successfully.");
            } else {
                System.out.println("External storage not available.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Excel", "Error appending data: " + e.getMessage());
        }
    }


    private void createExcelFile(File file) {
        try {
            if (!file.exists()) {
                // Create a new workbook and sheet
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Sheet1");

                // (Optional) Create header row
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Planned Date");
                header.createCell(1).setCellValue("Inspector Name");
                header.createCell(2).setCellValue("Carton Nr");
                header.createCell(3).setCellValue("Part Nr");
                header.createCell(4).setCellValue("D-Nr");
                header.createCell(5).setCellValue("Qtty");
                header.createCell(6).setCellValue("Comments");

                // Save to file
                FileOutputStream fos = new FileOutputStream(file);
                workbook.write(fos);
                fos.close();
                workbook.close();

                Log.d("Excel", "Excel file created.");
            } else {
                Log.d("Excel", "Excel file already exists.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Excel", "Failed to create Excel file: " + e.getMessage());
        }
    }

    private String getFileName() {
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy", Locale.getDefault());
        String strDate = format.format(new Date());

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        String fileDate = sharedPreferences.getString(Constants.FILE_DATE, "");

        if (fileDate.isEmpty() || !strDate.equals(fileDate)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(Constants.FILE_DATE, strDate);
            editor.apply();
        }

        String fileName = String.format(Locale.getDefault(), "tischPacken_%s.xlsx", strDate);

        return fileName;
    }

    private void showInformationDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("OK", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .show();
    }
}