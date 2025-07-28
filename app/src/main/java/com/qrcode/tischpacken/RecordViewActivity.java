package com.qrcode.tischpacken;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RecordViewActivity extends AppCompatActivity {

    private RecyclerView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_record_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        readExcelFile();
    }

    private void readExcelFile() {
        ArrayList<ArrayList<String>> cellList = new ArrayList<>();
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {

                File file = new File(Utils.getMainFilePath(getApplicationContext()) + "/" + Constants.FolderName + "/" + getFileName());

                FileInputStream fis = new FileInputStream(file);

                Workbook workbook = new XSSFWorkbook(fis);

                Sheet sheet = workbook.getSheetAt(0);

                for (Row row: sheet) {
                    if (row.getRowNum() == 0) continue;

                    ArrayList<String> rowValue= new ArrayList<>();
                    for (Cell cell: row) {
                        switch (cell.getCellType()) {
                            case STRING:
                                rowValue.add(cell.getStringCellValue());
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    rowValue.add(cell.getDateCellValue().toString());
                                } else {
                                    rowValue.add(String.valueOf(cell.getNumericCellValue()));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    cellList.add(rowValue);
                }

                workbook.close();
                fis.close();
            }
        }catch(Exception exp){
            exp.printStackTrace();
            Toast.makeText(this, "Sorry User don't have view report", Toast.LENGTH_SHORT).show();
        }


        listView = findViewById(R.id.listView);
        listView.setLayoutManager(new LinearLayoutManager(this));

        RecordViewAdapter reportAdapter = new RecordViewAdapter(cellList);

        listView.setAdapter(reportAdapter);

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
}
