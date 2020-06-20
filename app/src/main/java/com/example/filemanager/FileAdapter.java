package com.example.filemanager;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class FileAdapter extends BaseAdapter {
    List<File> items;

    public FileAdapter(List<File> items) {
        this.items = items;
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public Object getItem(int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.itemLayout = convertView.findViewById(R.id.a_file);
            viewHolder.fileIcon = convertView.findViewById(R.id.f_icon);
            viewHolder.fileName = convertView.findViewById(R.id.f_name);
            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();
        final File file = items.get(position);
        viewHolder.fileName.setText(file.getName());
        if (file.isDirectory()) {
//            if(file.list().length != 0)
                viewHolder.fileIcon.setImageResource(R.drawable.ic_folder_amber_80dp);
//            else
//                viewHolder.fileIcon.setImageResource(R.drawable.ic_folder_empty_ember_80dp);
        } else {
            if (MainActivity.getFileType(file.getAbsolutePath()) != null)
                if (MainActivity.getFileType(file.getAbsolutePath()).contains("image")) {
                    viewHolder.fileIcon.setImageDrawable(Drawable.createFromPath(file.getAbsolutePath()));
                } else if (MainActivity.getFileType(file.getAbsolutePath()).contains("text"))
                    viewHolder.fileIcon.setImageResource(R.drawable.ic_file_blue_80dp);
                else viewHolder.fileIcon.setImageResource(R.drawable.ic_file_red_80dp);
            else viewHolder.fileIcon.setImageResource(R.drawable.ic_broken_file_black_80dp);
        }
        return convertView;
    }


    class ViewHolder {
        RelativeLayout itemLayout;
        ImageView fileIcon;
        TextView fileName;
    }
}
