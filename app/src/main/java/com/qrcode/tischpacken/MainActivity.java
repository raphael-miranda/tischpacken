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
import android.widget.ImageButton;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    private TextView txtTitle;
    private TextInputEditText txtName;
    private AppCompatButton btnNext;

    private TextView txtInspectorName, txtInspectorNumber, txtInspectorDate, txtPlannedCartons;

    private RecyclerView planListView;

    private TextInputEditText txtScan;
    private AppCompatButton btnAdd;
    private ImageButton btnSettings, btnUpdate, btnSave;

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

    ArrayList<HashMap<String, String>> selectedCartons = new ArrayList<>();

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
        DividerItemDecoration divider = new DividerItemDecoration(planListView.getContext(), LinearLayoutManager.VERTICAL);
        planListView.addItemDecoration(divider);

        txtScan = findViewById(R.id.txtScan);
        btnAdd = findViewById(R.id.btnAdd);

        btnSettings = findViewById(R.id.btnSettings);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnSave = findViewById(R.id.btnSave);

        txtTitle.setText(getString(R.string.title, 0, 0));

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

        btnSettings.setOnClickListener(view -> {
            showSettingsDialog();
        });

        btnUpdate.setOnClickListener(view -> {
            downloadPlanFromSMB();
        });

        btnSave.setOnClickListener(view -> {

        });

        initNameInput();
        initContentsInput();
    }

    private void initNameInput() {

        txtName.requestFocus();
//        if (!isManual) {
//            txtName.setShowSoftInputOnFocus(false);
//            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//        }
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
                    btnNext.setEnabled(true);
                } else {
                    checkNameFromExcel(strName);
                }
            }
        });
    }

    private void initContentsInput() {
        txtScan.setText("");
        txtScan.setEnabled(false);
//        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
//        boolean isManual = sharedPreferences.getBoolean(IS_MANUAL, false);

//        if (!isManual) {
//            txtScan.setShowSoftInputOnFocus(false);
//            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//        }
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
                    String strDNr2 = strCtNr.split(";")[2];
                    String strQtty = strCtNr.split(";")[3];
                    String strCartonNr = strCtNr.split(";")[0];

                    int qtty = Integer.parseInt(strQtty);

                    checkPartNumber(partNr, qtty);
                }

                if (txtScan.getText().toString().isEmpty()) {
                    txtScan.setBackgroundTintList(yellowColors);
                }
            }
        });
    }

    private void checkPartNumber(String partNr, int qtty) {

        Set<Integer> selectedPositions = new HashSet<>();
        for (int i = 0; i < selectedCartons.size(); i++) {
            HashMap<String, String> carton = selectedCartons.get(i);
            String partNumber = carton.getOrDefault(Constants.PART_NUMBER, "");
            if (partNumber.equals(partNr)) {
                String strScanCounter = carton.getOrDefault(Constants.SCAN_COUNTER, "0");
                int scanCounter = Integer.parseInt(strScanCounter);
                scanCounter += 1;
                carton.put(Constants.SCAN_COUNTER, String.valueOf(scanCounter));
                selectedCartons.set(i, carton);
                selectedPositions.add(i);
                break;
            }
        }
        if (selectedPositions.isEmpty()) {
            txtScan.setBackgroundTintList(redColors);
        } else {
            txtScan.setBackgroundTintList(normalColors);
        }

        PlanListAdapter planListAdapter = new PlanListAdapter(selectedCartons, selectedPositions);
        planListView.setAdapter(planListAdapter);
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
        selectedCartons = new ArrayList<>();
        int totalNoOfCartons = 0;
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
                                    value = cell.getDateCellValue().toString();
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

                        if (cell.getColumnIndex() == 13) {
                            rowValue.put(Constants.CARTON_NUMBER, value);
                        }
                    }
                    rowValue.put(Constants.SCAN_COUNTER, "0");
                    selectedCartons.add(rowValue);
                }
            }

            wb.close();
            fs.close();
        }catch(Exception exp){
            exp.printStackTrace();
        }

        if (selectedCartons.isEmpty()) {
            txtInspectorName.setText("");
            txtInspectorNumber.setText("");
            txtInspectorDate.setText("");
            txtPlannedCartons.setText("");

            txtName.setEnabled(true);
            txtName.setBackgroundTintList(yellowColors);
        } else {
            HashMap<String, String> rowValue = selectedCartons.get(0);
            txtInspectorName.setText(rowValue.getOrDefault(Constants.INSPECTOR, ""));
            txtInspectorNumber.setText(rowValue.getOrDefault(Constants.CARTON_NUMBER, ""));
            txtInspectorDate.setText(rowValue.getOrDefault(Constants.DATE, ""));
            txtPlannedCartons.setText(String.valueOf(totalNoOfCartons));

            txtName.setBackgroundTintList(normalColors);
            txtName.setEnabled(false);
            txtScan.setEnabled(true);
            txtScan.requestFocus();
        }

        Set<Integer> selectedPositions = new HashSet<>();
        PlanListAdapter planListAdapter = new PlanListAdapter(selectedCartons, selectedPositions);
        planListView.setAdapter(planListAdapter);
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
            });
        });
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