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
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements PlanListAdapter.OnSkipButtonClickListener {

    int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    private TextView txtInspectorCounter, txtTotalInspectors;
    int inspectorCounter = 0;
    private TextInputEditText txtName;
    private AppCompatButton btnNext;

    private TextView txtInspectorNumber, txtInspectionDate, txtPlannedCartons;

    private RecyclerView planListView;

    private TextInputEditText txtScan;
    private AppCompatButton btnClear;
    private ImageButton btnSettings;

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
        txtInspectorCounter.setText(String.valueOf(inspectorCounter));
        txtTotalInspectors = findViewById(R.id.txtTotalInspectors);

        txtName = findViewById(R.id.txtName);
        btnNext = findViewById(R.id.btnNext);

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
            clearAll();
            txtName.setEnabled(true);
            txtName.requestFocus();
            txtScan.setEnabled(false);
        });

        btnClear.setOnClickListener(view -> {
            txtScan.setText("");
        });

        btnSettings.setOnClickListener(view -> {
            showSettingsDialog();
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
                                try {
                                    Date date = cell.getDateCellValue();
                                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                                    value = simpleDateFormat.format(date);
                                } catch (Exception e) {
                                    value = "";
                                }
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
        final int[] selectedPosition = {-1};
        HashMap<String, String> matchedCarton = new HashMap<>();

        boolean isPartNumberFounded = false;
        for (int i = 0; i < cartonsFromPlan.size(); i++) {
            matchedCarton = cartonsFromPlan.get(i);
            String partNumber = matchedCarton.getOrDefault(Constants.PART_NUMBER, "");
            if (partNumber.equals(partNr)) {
                isPartNumberFounded = true;


                HashMap<String, String> finalMatchedCarton = matchedCarton;
                int finalI = i;
                verificationChecks(matchedCarton, scannedCarton, new VerificationCallback() {
                    @Override
                    public void onResult(boolean verified) {
                        if (verified) {
                            txtScan.setBackgroundTintList(normalColors);
                            txtScan.setText("");

                            cartonsFromPlan.set(finalI, finalMatchedCarton);
                            selectedPosition[0] = finalI;

                            HashMap<String, String> cartonToSave = new HashMap<>();

                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yyyy", Locale.getDefault());
                            String currentDate = simpleDateFormat.format(new Date());
                            cartonToSave.put(Constants.SCAN_DATE, currentDate);
                            cartonToSave.put(Constants.INSPECTOR, finalMatchedCarton.getOrDefault(Constants.INSPECTOR, ""));
                            cartonToSave.put(Constants.CT_NR, scannedCarton.getOrDefault(Constants.CT_NR, ""));
                            cartonToSave.put(Constants.PART_NUMBER, scannedCarton.getOrDefault(Constants.PART_NUMBER, ""));
                            cartonToSave.put(Constants.D_NR, scannedCarton.getOrDefault(Constants.D_NR, ""));
                            cartonToSave.put(Constants.QTTY, scannedCarton.getOrDefault(Constants.QTTY, "0"));

                            scannedList.add(cartonToSave);
                            saveRecord(cartonToSave);

                            planListAdapter = new PlanListAdapter(cartonsFromPlan, selectedPosition[0], scannedList, MainActivity.this);
                            planListView.setAdapter(planListAdapter);
                        } else {
                            txtScan.setBackgroundTintList(redColors);
                        }
                    }
                });

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
        Log.d("========", "scanned " + scannedList.size() + " / " + totalNoOfCartons);
        if (scannedList.size() >= totalNoOfCartons) {
            btnNext.setEnabled(true);
        } else {
            int totalCounter = 0;
            for (HashMap<String, String> scannedCarton : scannedList) {
                int skipped = Integer.parseInt(scannedCarton.getOrDefault(Constants.SKIP_COUNTER, "0"));
                if (skipped > 0) {
                    totalCounter += skipped;
                } else {
                    totalCounter += 1;
                }
            }

            Log.d("========", "scanned " + totalCounter + " / " + totalNoOfCartons);

            if (totalCounter >= totalNoOfCartons) {
                btnNext.setEnabled(true);
            } else {
                btnNext.setEnabled(false);
            }
        }
    }

    private void clearAll() {

        txtScan.setText("");
        txtName.setText("");
        txtInspectorNumber.setText("");
        txtInspectionDate.setText("");
        txtPlannedCartons.setText("");

        cartonsFromPlan.clear();
        scannedList.clear();

        planListAdapter = new PlanListAdapter(cartonsFromPlan, -1, scannedList, this);
        planListView.setAdapter(planListAdapter);
    }

    public interface VerificationCallback {
        void onResult(boolean verified);
    }

    private void verificationChecks(HashMap<String, String> matchedCarton, HashMap<String, String> scannedCarton, VerificationCallback callback) {
        String scannedCartonNr = scannedCarton.getOrDefault(Constants.CT_NR, "");
        String strScannedQtty = scannedCarton.getOrDefault(Constants.QTTY, "0");
        int scannedQtty = Integer.parseInt(strScannedQtty);

        String inspector = matchedCarton.getOrDefault(Constants.INSPECTOR, "");
        String cartonNrs = matchedCarton.getOrDefault(Constants.CT_NR, "");
        String type = matchedCarton.getOrDefault(Constants.TYPE, "");
        String partNumber = matchedCarton.getOrDefault(Constants.PART_NUMBER, "");
        String inspectorNr = matchedCarton.getOrDefault(Constants.INSPECTOR_NR, "0");

        if (!cartonNrs.isEmpty()) {
            String[] ctNrs = cartonNrs.split("\\s*\\s");
            List<String> arrCartonNrs = Arrays.asList(ctNrs);

            if (!arrCartonNrs.contains(scannedCartonNr)) {
                showInformationDialog("Verification Failed", "Scanned carton number is not in the planned carton list.");
                callback.onResult(false);
                return;
            }

        }

        // check duplicated carton
        if (isExistedCarton(inspector, scannedCartonNr)) {
            showInformationDialog("Verification Failed", "Double scan!");
            callback.onResult(false);
            return;
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
                            callback.onResult(false);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        showInformationDialog("Verification Failed", "The carton is not unchecked.");
                        callback.onResult(false);
                        return;
                    }

                }
            }
            if (!isChecked) {
                showInformationDialog("Verification Failed", "The carton is not unchecked.");
                callback.onResult(false);
                return;
            }
        }

        // Controlled Part Check (controlledparts.txt)

        if (type.equals("1st Check") && controlledParts.contains(partNumber)) {
            if (partNumber != null && !partNumber.isEmpty()) {
                showFirstInspectorPrompt(new InputCallback() {
                    @Override
                    public void onResult(String userInput) {
                        if (userInput.equals(inspectorNr)) {
                            showInformationDialog("Verification Failed", "Inspector cannot inspect own carton");
                            callback.onResult(false);
                        } else {
                            callback.onResult(true);
                        }
                    }
                });
            }
        } else if (type.equals("Visual Only")) { // Visual Only plan
            showFirstInspectorPrompt(new InputCallback() {
                @Override
                public void onResult(String userInput) {
                    if (userInput.equals(inspectorNr)) {
                        showInformationDialog("Verification Failed", "Inspector cannot inspect own carton");
                        callback.onResult(false);
                    } else {
                        callback.onResult(true);
                    }
                }
            });
        } else {
            callback.onResult(true);
        }
    }

    public interface InputCallback {
        void onResult(String userInput);
    }


    private void showFirstInspectorPrompt(InputCallback callback) {
        EditText input = new EditText(this);
        input.setHint("Enter inspector number");
        input.setPadding(40, 30, 40, 30);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(50, 40, 50, 10); // outer padding for dialog
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("1st Inspector")
                .setView(container)
                .setPositiveButton("OK", null) // We'll override this later
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setEnabled(false); // Initially disable the OK button

            // Enable OK button only if input is not empty
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    okButton.setEnabled(s.toString().trim().length() > 0);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Manually handle click to check again before dismiss
            okButton.setOnClickListener(v -> {
                String scanInspectorNr = input.getText().toString().trim();
                if (!scanInspectorNr.isEmpty()) {
                    callback.onResult(scanInspectorNr);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private boolean isExistedCarton(String scannedInspector, String scannedCartonNr) {

        if (scannedInspector.isEmpty() || scannedCartonNr.isEmpty()) {
            return false;
        }

        for (HashMap<String, String> carton: scannedList) {
            String oldCtNr = carton.getOrDefault(Constants.CT_NR, "");
            if (scannedCartonNr.equals(oldCtNr)) {
                return true;
            }
        }

        boolean result = false;

        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + getFileName());

                FileInputStream fis = new FileInputStream(file);

                Workbook workbook = new XSSFWorkbook(fis);

                Sheet sheet = workbook.getSheetAt(0);

                for (Row row: sheet) {
                    if (row.getRowNum() == 0) continue;

                    Cell inspectorCell = row.getCell(1);
                    String inspector = inspectorCell.getStringCellValue();
                    Cell ctNrCell = row.getCell(2);
                    String ctNr = ctNrCell.getStringCellValue();

                    if (inspector.equals(scannedInspector) && scannedCartonNr.equals(ctNr)) {
                        result = true;
                    }

                }

                workbook.close();
                fis.close();
            }
        } catch(Exception exp) {
            exp.printStackTrace();
        }

        return result;
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
            totalNoOfCartons = 0;
            String FilePath = Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/plan.xls";
            FileInputStream fs = new FileInputStream(FilePath);
            Workbook wb = new HSSFWorkbook(fs);

            ArrayList<String> arrPartNrs = new ArrayList<>();

            Sheet sheet = wb.getSheetAt(0);

            for (Row row: sheet) {
                if (row.getRowNum() == 0) continue;

                HashMap<String, String> rowValue= new HashMap<>();
                Cell inspectorCell = row.getCell(1);
                String inspector = inspectorCell.getStringCellValue();
                Cell partNrCell = row.getCell(2);
                String partNr = partNrCell.getStringCellValue();

                if (inspector.equals(inspectorName) && !arrPartNrs.contains(partNr)) {
                    arrPartNrs.add(partNr);

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
                    cartonsFromPlan.add(rowValue);
                }
            }

            wb.close();
            fs.close();
        }catch(Exception exp){
            exp.printStackTrace();
        }

        if (cartonsFromPlan.isEmpty()) {
            txtInspectorNumber.setText("");
            txtInspectionDate.setText("");
            txtPlannedCartons.setText("");

            txtName.setEnabled(true);
            txtName.setBackgroundTintList(yellowColors);
            showInformationDialog("Error", "Name not found in plan.");
            txtName.setText("");
        } else {
            inspectorCounter += 1;
            txtInspectorCounter.setText(String.valueOf(inspectorCounter));

            HashMap<String, String> rowValue = cartonsFromPlan.get(0);
            txtInspectorNumber.setText(rowValue.getOrDefault(Constants.INSPECTOR_NR, ""));
            txtInspectionDate.setText(rowValue.getOrDefault(Constants.DATE, ""));
            txtPlannedCartons.setText(String.valueOf(totalNoOfCartons));

            txtName.setBackgroundTintList(normalColors);
            txtName.setEnabled(false);
            txtScan.setEnabled(true);
            txtScan.requestFocus();
            scannedList = readSavedScanList(inspectorName);
            checkNext();
        }

        planListAdapter = new PlanListAdapter(cartonsFromPlan, -1, scannedList, this);
        planListView.setAdapter(planListAdapter);
    }

    private ArrayList<HashMap<String, String>> readSavedScanList(String inspectorName) {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + getFileName());

                FileInputStream fis = new FileInputStream(file);

                Workbook workbook = new XSSFWorkbook(fis);

                Sheet sheet = workbook.getSheetAt(0);

                for (Row row: sheet) {
                    if (row.getRowNum() == 0) continue;

                    Cell inspectorCell = row.getCell(1);
                    String inspector = inspectorCell.getStringCellValue();

                    if (inspector.equals(inspectorName)) {
                        HashMap<String, String> savedCarton = new HashMap<>();
                        savedCarton.put(Constants.INSPECTOR, inspector);

                        for (Cell cell: row) {

                            String value = "";

                            switch (cell.getCellType()) {
                                case STRING:
                                    value = cell.getStringCellValue();
                                    break;
                                case NUMERIC:
                                    if (DateUtil.isCellDateFormatted(cell)) {
                                        try {
                                            Date date = cell.getDateCellValue();
                                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                                            value = simpleDateFormat.format(date);
                                        } catch (Exception e) {
                                            value = "";
                                        }
                                    } else {
                                        value = String.valueOf((int) cell.getNumericCellValue());
                                    }
                                    break;
                                default:
                                    break;
                            }

                            if (cell.getColumnIndex() == 2) {
                                savedCarton.put(Constants.CT_NR, value);
                            }

                            if (cell.getColumnIndex() == 3) {
                                savedCarton.put(Constants.PART_NUMBER, value);
                            }

                            if (cell.getColumnIndex() == 4) {
                                savedCarton.put(Constants.D_NR, value);
                            }

                            if (cell.getColumnIndex() == 5) {
                                savedCarton.put(Constants.QTTY, value);
                            }

                            if (cell.getColumnIndex() == 6) {
                                savedCarton.put(Constants.SCAN_STATUS, value);
                            }

                            if (cell.getColumnIndex() == 7) {
                                savedCarton.put(Constants.SKIP_COUNTER, value);
                            }

                        }

                        result.add(savedCarton);
                    }
                }

                workbook.close();
                fis.close();
            }
        }catch(Exception exp){
            exp.printStackTrace();
        }

        return result;
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
            saveRecord(skippedItem);

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

        ImageButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);
        ImageButton btnViewRecord = dialogView.findViewById(R.id.btnViewRecord);
        ImageButton btnUpload = dialogView.findViewById(R.id.btnUpload);

        // Check for Upload button
        String fileName = getFileName();
        File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + fileName);
        if (file.exists()) {
            btnUpload.setEnabled(true);
        } else {
            btnUpload.setEnabled(false);
        }

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

        btnUpdate.setOnClickListener(view -> {
            downloadPlanFromSMB();
        });

        btnViewRecord.setOnClickListener(view -> {
            Intent intent = new Intent(this, RecordViewActivity.class);
            startActivity(intent);
            dialog.dismiss();
        });

        btnUpload.setOnClickListener(view -> {
            uploadRecords();
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

    private void saveRecord(HashMap<String, String> record) {

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

                int lastRowNum = sheet.getLastRowNum();
                Row newRow = sheet.createRow(lastRowNum + 1);

                Cell dateCell = newRow.createCell(0);
                dateCell.setCellValue(record.getOrDefault(Constants.SCAN_DATE, ""));

                Cell inspectorCell = newRow.createCell(1);
                inspectorCell.setCellValue(record.getOrDefault(Constants.INSPECTOR, ""));

                Cell ctNrCell = newRow.createCell(2);
                ctNrCell.setCellValue(record.getOrDefault(Constants.CT_NR, ""));

                Cell partNrCell = newRow.createCell(3);
                partNrCell.setCellValue(record.getOrDefault(Constants.PART_NUMBER, ""));

                Cell dNrCell = newRow.createCell(4);
                dNrCell.setCellValue(record.getOrDefault(Constants.D_NR, ""));

                Cell qttyCell = newRow.createCell(5);
                qttyCell.setCellValue(record.getOrDefault(Constants.QTTY, ""));

                Cell commentsCell = newRow.createCell(6);
                commentsCell.setCellValue(record.getOrDefault(Constants.SCAN_STATUS, ""));

                Cell skippedCounterCell = newRow.createCell(7);
                skippedCounterCell.setCellValue(record.getOrDefault(Constants.SKIP_COUNTER, ""));


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

    private void uploadRecords() {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        String host = sharedPreferences.getString(Constants.SMB_SERVER_ADDRESS, "");
        String username = sharedPreferences.getString(Constants.SMB_USERNAME, "");
        String password = sharedPreferences.getString(Constants.SMB_PASSWORD, "");
        String sharedFolder = sharedPreferences.getString(Constants.SMB_SHARED_FOLDER, "");


        if (host.isEmpty() || !isValidUrl(host)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Error")
                    .setMessage("Please set valid server address in Settings. Do you want to create new one without uploading?")
                    .setNegativeButton("Yes", (dialogInterface, i) -> {
                        // set scanned number to 0
                        dialogInterface.dismiss();
                    })
                    .setPositiveButton("No", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                    })
                    .show();
        } else {
            uploadFileToSMB(host, sharedFolder, "", username, password);
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && Patterns.WEB_URL.matcher(url).matches();
    }

    public void uploadFileToSMB(String hostname, String shareName, String domain,
                                String username, String password) {
        String fileName = getFileName();
        File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + fileName);
        if (!file.exists()) {
            showInformationDialog("Error", "There is no records to upload.");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // SMB upload logic here
                SMBClient client = new SMBClient();
                try (Connection connection = client.connect(hostname)) {
                    AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), "");
                    com.hierynomus.smbj.session.Session session = connection.authenticate(ac);
                    DiskShare share = (DiskShare) session.connectShare(shareName);
                    if (!share.folderExists("tischPacken/records")) {
                        share.mkdir("tischPacken/records");
                    }

                    String remotePath = "tischPacken/records/" + fileName;

                    FileInputStream fis = new FileInputStream(file);
                    OutputStream os = share.openFile(remotePath,
                            EnumSet.of(AccessMask.GENERIC_WRITE),
                            null,
                            SMB2ShareAccess.ALL,
                            SMB2CreateDisposition.FILE_OVERWRITE_IF,
                            null).getOutputStream();

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }

                    os.close();
                    fis.close();
                    share.close();
                    session.close();
                }

                // Notify success on main thread
                mainHandler.post(() -> {
                    Toast.makeText(this, "Upload successful", Toast.LENGTH_SHORT).show();
                    removeCurrentFile();
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void removeCurrentFile() {

        File dir = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("tischPacken_") && file.getName().endsWith(".xlsx")) {
                        boolean deleted = file.delete();
                        Log.d("FileDelete", file.getName() + (deleted ? " deleted." : " failed to delete."));
                    }
                }
            }
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