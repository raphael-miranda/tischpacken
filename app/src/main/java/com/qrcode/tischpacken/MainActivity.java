package com.qrcode.tischpacken;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    TextView txtTitle;

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
        txtTitle.setText(getString(R.string.title, 0, 0));


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

        File loggerFile = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

        if(!loggerFile.exists()){
            boolean isLoggerCreated = loggerFile.mkdir();
        }else{
            File exlFile2 = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName);

            File loginExcel = new File(loggerFile, "/plan.xls");
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
}