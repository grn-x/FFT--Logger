package de.grnx.fftlogger.ui.results;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.grnx.fftlogger.FileHandling.FileCopyPermissionCallback;
import de.grnx.fftlogger.FileHandling.FileUtils;
import de.grnx.fftlogger.MainActivity;
import android.Manifest;
import de.grnx.fftlogger.R;
import de.grnx.fftlogger.RequestCodes;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsViewHolder> {

    final ArrayList<File> results;
    private final TextView tvResultBox;

   private final Context context;

   private final Button deleteButton;

   public File currentReference;
    public static BiConsumer<View, File> onClick;

    public static BiConsumer<View, File> onLongClick;

    private FileCopyPermissionCallback callback;
    public static View currentView;


    public ResultsAdapter(Activity context, ArrayList<File> results, TextView tvResultBox, Button deleteButton, FileCopyPermissionCallback callback) {
        this.results = results;
        this.tvResultBox = tvResultBox;
        this.context = context;
        this.deleteButton = deleteButton;
        this.callback = callback;

        onClick = new BiConsumer<View, File>() {
            @Override
            public void accept(View v, File f) {
                System.out.println(f.getAbsoluteFile());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {


                    try {
                        String contents = new String(Files.readAllBytes(f.toPath()), Charset.defaultCharset());
                        tvResultBox.setText(contents);
                        tvResultBox.setTextColor(Color.WHITE);
                        if(currentView !=null)currentView.setBackgroundColor(Color.WHITE);
                        currentView = v;
                        v.setBackgroundColor(Color.GRAY);
                        currentReference=f;//fix this. somehow get the current file after click

                    } catch (Exception e) {
                        e.printStackTrace();
                        tvResultBox.setText(e.getMessage());
                    }
                    }
                });
            }};

        onLongClick = new BiConsumer<View, File>() {
            @Override
            public void accept(View v, File f) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        // Create a confirmation dialog
                        new AlertDialog.Builder(context)
                                .setTitle("Copy File")
                                .setMessage("Do you want to copy the file to the downloads directory?")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        if (FileUtils.hasPermissions(context)) {
                                            FileUtils.copyFileToDownloads(context, f, null);
                                        } else {
                                            callback.requestFileCopyPermission(f, null);
                                            FileUtils.copyFileToDownloads(context, f, null);

                                        }
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .show();
                    }
                });
            }
        };


    }

    @NonNull
    @Override
    public ResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recview_result_list, parent, false);
        return new ResultsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultsViewHolder holder, int position) {
        holder.setValue(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }




}

