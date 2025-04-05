package de.grnx.fftlogger.ui.results;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import de.grnx.fftlogger.R;

class ResultsViewHolder extends RecyclerView.ViewHolder {
    private final TextView textView;
    private File f;

    public ResultsViewHolder(@NonNull View itemView) {
        super(itemView);
        textView = itemView.findViewById(R.id.textView);
        itemView.setOnClickListener(v -> {
            ResultsAdapter.onClick.accept(v,this.f);
        });
        itemView.setOnLongClickListener(v -> {
            ResultsAdapter.onLongClick.accept(v,this.f);
            return true;
        });
        }



    public void setValue(File f){
        this.f=f;
        this.textView.setText(f.getName());
    }

    public File getFile(){
        return this.f;
    }

}