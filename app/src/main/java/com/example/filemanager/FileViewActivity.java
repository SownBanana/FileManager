package com.example.filemanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class FileViewActivity extends AppCompatActivity {
    EditText textView;
    ImageView imageView;

    String type;
    String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_view_layout);
        textView = findViewById(R.id.text_view);
        imageView = findViewById(R.id.image_view);


        Intent intent = getIntent();
        Bundle fileBundle = intent.getExtras();
        type = (String) fileBundle.get("type");
        path = (String) fileBundle.get("path");


        if (type.equals("text")) {
            textView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            imageView.getLayoutParams().height = 0;
            textView.setText("");
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
                String line = "";
                String rs = "";
                while ((line = reader.readLine()) != null) {
                    rs += line + '\n';
                }
                reader.close();
                textView.append(rs);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (type.equals("image")) {
            textView.getLayoutParams().height = 0;
            imageView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            imageView.setImageDrawable(Drawable.createFromPath(path));
        }
    }

    //Save text file when close activity
    @Override
    protected void onDestroy() {
        if (type.equals("text"))
            try {
                PrintWriter printWriter = new PrintWriter(path);
                printWriter.print(textView.getText());
                printWriter.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        super.onDestroy();
    }
}
