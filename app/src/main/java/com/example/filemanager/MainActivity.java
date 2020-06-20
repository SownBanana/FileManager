package com.example.filemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    File currentDirectory;

    FileAdapter adapter;
    List<File> files;
    GridView gridFile;
    TextView curPathView;
    TextView emptyView;
    Context activityContext;
    LinearLayout copyLayout;

    //Check if is choosing directory for copy or move file
    boolean isChooseDirectory = false;
    boolean isMoveFile = false;
    File beforeDirectory;

    //Touch back twice to back
    int backCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityContext = this;
        //Check permission
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.v("TAG", "Permission denied! Asking for permission from user");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1235);
            } else {
                Log.v("TAG", "Have Permission!");
            }
        }
        gridFile = findViewById(R.id.grid_file);
        //Event click each items of list File and Folder
        gridFile.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GridView gv = (GridView) parent;
                FileAdapter adapter = (FileAdapter) gv.getAdapter();
                File file = (File) adapter.getItem(position);
                if (file.isDirectory()) {
                    showFileInFolder(file);
                } else {
                    try {
                        if (getFileType(file.getAbsolutePath()).contains("text")) {
                            startViewFileActivity(file.getAbsolutePath(), "text");
                        } else if (getFileType(file.getAbsolutePath()).contains("image")) {
                            startViewFileActivity(file.getAbsolutePath(), "image");
                        } else {
                            Toast.makeText(getApplicationContext(), "This " + getFileType(file.getAbsolutePath()) + " file type is not supported!!!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.v("TAG", e.getMessage());
                        Toast.makeText(getApplicationContext(), "This file is not supported!!!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        //Register context menu for file view
        registerForContextMenu(gridFile);
        curPathView = findViewById(R.id.cur_path);
        emptyView = findViewById(R.id.empty_folder);
        copyLayout = findViewById(R.id.cpy_layout);

        //First show file and folder list (if sdcard is put on)
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sdPath = Environment.getExternalStorageDirectory();
            showFileInFolder(sdPath);
        } else Log.v("TAG", "UNMOUNTED");

        //Button copy or move file
        findViewById(R.id.cpy_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File target = new File(currentDirectory.getAbsolutePath() + "/" + beforeDirectory.getName());
                    target.createNewFile();
                    FileInputStream is = new FileInputStream(beforeDirectory);
                    FileOutputStream os = new FileOutputStream(target);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    //If is being moving file, delete old file
                    if(isMoveFile) beforeDirectory.delete();
                    is.close();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                copyLayout.getLayoutParams().height = 0;
                isChooseDirectory = false;
                isMoveFile = false;
                showFileInFolder(currentDirectory);
            }
        });
        //Cancel copy or move
        findViewById(R.id.cancel_cpy_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyLayout.getLayoutParams().height = 0;
                isChooseDirectory = false;
                showFileInFolder(currentDirectory);
            }
        });
        //The view that show current file/folder path
        findViewById(R.id.path_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
                    showFileInFolder(currentDirectory.getParentFile());
                else {
                    Toast.makeText(getApplicationContext(), "Root Directory", Toast.LENGTH_SHORT).show();
                    Log.v("TAG", "Root Directory");
                }
            }
        });

        //Back button in actionbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    //Context menu of file and folder
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (!isChooseDirectory) {
            if (v.getId() == R.id.grid_file) {
                GridView view = (GridView) v;
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                File file = (File) view.getItemAtPosition(info.position);
                if (file.isDirectory()) {
                    menu.add("Rename Folder");
                    menu.add("Delete Folder");
                } else if (file.isFile()) {
                    menu.add("Rename File");
                    menu.add("Delete File");
                    menu.add("Copy File");
                    menu.add("Move File");
                }
            } else {
                menu.add("New Folder");
                menu.add("New Text File");
            }
        }
    }

    //file and folder context menu handle
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals("Rename Folder")) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final File file = (File) adapter.getItem(info.position);
            final Dialog renameDialog = new Dialog(this);
            renameDialog.setTitle("Rename Folder");
            renameDialog.setContentView(R.layout.rename_dialog);
            ((TextView) renameDialog.findViewById(R.id.tit)).setText("Rename Folder: ");
            ((TextView) renameDialog.findViewById(R.id.new_name)).setText(file.getName());
            final EditText editName = renameDialog.findViewById(R.id.new_name);
            ((Button) renameDialog.findViewById(R.id.cancel_rename_btn)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renameDialog.dismiss();
                }
            });
            ((Button) renameDialog.findViewById(R.id.rename_btn)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String parentPath = file.getParent();
                    Log.v("TAG", "Parent: " + file.getParent());
                    File to = new File(parentPath, editName.getText().toString());
                    file.renameTo(to);
                    renameDialog.dismiss();
                    showFileInFolder(currentDirectory);
                }
            });
            renameDialog.show();
        } else if (item.getTitle().equals("Delete Folder")) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final File file = (File) adapter.getItem(info.position);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete Folder")
                    .setMessage("Are you sure that you want to delete " + file.getName() + " folder?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteFolder(file);
                            showFileInFolder(currentDirectory);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            })
                    .create()
                    .show();
        } else if (item.getTitle().equals("Rename File")) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final File file = (File) adapter.getItem(info.position);
            final Dialog renameDialog = new Dialog(this);
            renameDialog.setTitle("Rename File");
            renameDialog.setContentView(R.layout.rename_dialog);
            ((TextView) renameDialog.findViewById(R.id.tit)).setText("Rename File: ");
            ((TextView) renameDialog.findViewById(R.id.new_name)).setText(file.getName());
            final EditText editName = renameDialog.findViewById(R.id.new_name);
            ((Button) renameDialog.findViewById(R.id.cancel_rename_btn)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    renameDialog.dismiss();
                }
            });
            ((Button) renameDialog.findViewById(R.id.rename_btn)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String parentPath = file.getParent();
                    String newFileName = editName.getText().toString();
                    try {
                        String[] fileInfo = file.getName().split("\\.");
                        if (!newFileName.contains(".")) {
                            newFileName += ".";
                            newFileName += fileInfo[1];
                        }
                    } catch (Exception e) {
                        Log.v("TAG", e.getMessage());
                    }
                    File to = new File(parentPath, newFileName);
                    file.renameTo(to);
                    renameDialog.dismiss();
                    showFileInFolder(currentDirectory);
                }
            });
            renameDialog.show();
        } else if (item.getTitle().equals("Delete File")) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final File file = (File) adapter.getItem(info.position);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Delete File")
                    .setMessage("Are you sure that you want to delete " + file.getName() + " file?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            file.delete();
                            showFileInFolder(currentDirectory);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            })
                    .create()
                    .show();
        } else if (item.getTitle().equals("Copy File")) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final File file = (File) adapter.getItem(info.position);
            copyAndMoveFile(file, false);
        }
        else if (item.getTitle().equals("Move File")) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            final File file = (File) adapter.getItem(info.position);
            copyAndMoveFile(file, true);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1235) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                Log.v("TAG", "Permission granted");
            else
                Log.v("TAG", "Permission denied");
        }
    }

    //Create custom menu in actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manager_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Event click back button in actionbar
    @Override
    public boolean onSupportNavigateUp() {
        if (!currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            showFileInFolder(currentDirectory.getParentFile());
        else {
            Toast.makeText(getApplicationContext(), "Root Directory", Toast.LENGTH_SHORT).show();
            Log.v("TAG", "Root Directory");
        }
        return true;
    }

    //Event press back
    @Override
    public void onBackPressed() {
        if (!currentDirectory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            showFileInFolder(currentDirectory.getParentFile());
        else {
            //press back twice to turn out application
            backCount++;
            if (backCount == 2)
                super.onBackPressed();
            else Toast.makeText(getApplicationContext(), "Root Directory", Toast.LENGTH_LONG).show();
        }
    }

    //Click menu in actionbar handle
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_btn_in_menu) {
            PopupMenu popupMenu = new PopupMenu(activityContext, findViewById(R.id.add_btn_in_menu));
            popupMenu.getMenu().add("New Folder");
            popupMenu.getMenu().add("New Text File");
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getTitle() == "New Folder") {
                        final Dialog newFolderDialog = new Dialog(activityContext);
                        newFolderDialog.setTitle("New Folder");
                        newFolderDialog.setContentView(R.layout.rename_dialog);
                        ((TextView) newFolderDialog.findViewById(R.id.tit)).setText("New Folder: ");
                        final EditText name = newFolderDialog.findViewById(R.id.new_name);
                        Button createBtn = newFolderDialog.findViewById(R.id.rename_btn);
                        createBtn.setText("Create");
                        createBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                File newFolder = new File(currentDirectory.getAbsolutePath(), name.getText().toString());
                                newFolder.mkdirs();
                                newFolderDialog.dismiss();
                                showFileInFolder(currentDirectory);
                                Toast.makeText(activityContext, name.getText() + " created", Toast.LENGTH_SHORT).show();
                            }
                        });
                        ((Button) newFolderDialog.findViewById(R.id.cancel_rename_btn)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                newFolderDialog.dismiss();
                            }
                        });
                        newFolderDialog.show();
                    } else {
                        final Dialog newFileDialog = new Dialog(activityContext);
                        newFileDialog.setTitle("New Text File");
                        newFileDialog.setContentView(R.layout.rename_dialog);
                        ((TextView) newFileDialog.findViewById(R.id.tit)).setText("New Text File: ");
                        final EditText name = newFileDialog.findViewById(R.id.new_name);
                        Button createBtn = newFileDialog.findViewById(R.id.rename_btn);
                        createBtn.setText("Create");
                        createBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String fileName = name.getText().toString();
                                if (!fileName.contains(".txt")) fileName += ".txt";
                                File newFile = new File(currentDirectory.getAbsolutePath(), fileName);
                                try {
                                    newFile.createNewFile();
                                    newFileDialog.dismiss();
                                    Intent intent = new Intent(MainActivity.this, FileViewActivity.class);
                                    Bundle fileBunble = new Bundle();
                                    fileBunble.putString("type", "text");
                                    fileBunble.putString("path", newFile.getAbsolutePath());
                                    intent.putExtras(fileBunble);
                                    startActivity(intent);
                                    showFileInFolder(currentDirectory);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        ((Button) newFileDialog.findViewById(R.id.cancel_rename_btn)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                newFileDialog.dismiss();
                            }
                        });
                        newFileDialog.show();
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
        return super.onOptionsItemSelected(item);
    }

    //Show File and Folder in Directory to screen
    public void showFileInFolder(File directory) {
        currentDirectory = directory;
        curPathView.setText(directory.getAbsolutePath().replace("/storage/emulated/0", "ROOT"));
        files = Arrays.asList(directory.listFiles());
        adapter = new FileAdapter(files);
        gridFile.setAdapter(adapter);
        if (files.size() == 0) {
            emptyView.setText("Folder " + directory.getName() + " is empty");
            emptyView.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else emptyView.getLayoutParams().height = 0;
    }

    public static void getAllFilesOfDir(File directory) {
        Log.v("TAG", "Directory: " + directory.getAbsolutePath() + " Exists: " + Boolean.toString(directory.exists()));
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file != null) {
                    if (file.isDirectory()) {
                        getAllFilesOfDir(file);
                    } else {
                        Log.v("TAG", "File: " + file.getAbsolutePath() + "\n");
                    }
                }
            }
        }
    }

    //Get file type
    public static String getFileType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    //Show Copy/Move file layout
    public void copyAndMoveFile(File file, boolean isMoveFile){
        copyLayout.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        Button move = findViewById(R.id.cpy_btn);
        this.isMoveFile = isMoveFile;
        if (isMoveFile) move.setText("Move Here");
        else move.setText("Copy Here");
        isChooseDirectory = true;
        beforeDirectory = file;
        showFileInFolder(currentDirectory);
    }

    //Delete whole folder having items
    void deleteFolder(File folder) {
        if (folder.isDirectory())
            for (File child : folder.listFiles())
                deleteFolder(child);
        folder.delete();
    }

    //
    void startViewFileActivity(String filePath, String fileType){
        Intent intent = new Intent(MainActivity.this, FileViewActivity.class);
        Bundle fileBunble = new Bundle();
        fileBunble.putString("type", fileType);
        fileBunble.putString("path", filePath);
        intent.putExtras(fileBunble);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        backCount = 0;
        //Refresh lít folder again
        showFileInFolder(currentDirectory);
        super.onResume();
    }
}
