package de.grnx.fftlogger.ui.results;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import de.grnx.fftlogger.MainActivity;
import de.grnx.fftlogger.R;
import de.grnx.fftlogger.databinding.FragmentResultsBinding;

public class ResultsFragment extends Fragment {

    private FragmentResultsBinding binding;
    private RecyclerView recyclerView;
    private Button deleteButton;
    private Button copyButton;
    private TextView tvResultBox;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SlideshowViewModel slideshowViewModel =
                new ViewModelProvider(this).get(SlideshowViewModel.class);

        binding = FragmentResultsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        deleteButton = root.findViewById(R.id.button);
        copyButton = root.findViewById(R.id.copy_button);
        tvResultBox = root.findViewById(R.id.tvResultBox);

//        final TextView textView = binding.textSlideshow;
//        slideshowViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        recyclerView = root.findViewById(R.id.recyclerView);

        File path =  ((MainActivity) getActivity()).getFilesDir();
        File subDir = new File(path, "logData");
        ArrayList<File> files = new ArrayList<>(Arrays.asList(subDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".txt");
            }
        })));

        //recyclerView.setAdapter(new ResultsAdapter(files)); set files to the adapter and when clicking on a file, copy the contents into the results view
//        recyclerView.setAdapter(new ResultsAdapter(files, tvResultBox));
//        recyclerView.getAdapter().notifyDataSetChanged();

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new ResultsAdapter(getActivity(), files, tvResultBox, deleteButton, (MainActivity)getActivity()));

        deleteButton.setOnClickListener(this::confirmDialog);
        copyButton.setOnClickListener(v -> {
            setClipboard(getContext(), tvResultBox.getText().toString(), "Copied Text from FFT Logger Resultbox");
            Toast.makeText(getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
        });
        return root;
    }//todo implent deletion

    public void confirmDialog(View v){
        new AlertDialog.Builder(getActivity())
                .setTitle("Title")
                .setMessage("Do you really want delete the selected Log Data?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) { //FIXME highlighted 
//accepted: delete file
                        File file = ((ResultsAdapter) recyclerView.getAdapter()).currentReference;
                        if(file == null){
                            Toast.makeText(getContext(), "No file selected", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(recyclerView.getAdapter().getItemCount()<1){
                            Toast.makeText(getContext(), "No files to delete", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        try{
                        if(file.delete()){
                            ((ResultsAdapter) recyclerView.getAdapter()).results.remove(file);
                            tvResultBox.setText("");
                            recyclerView.getAdapter().notifyDataSetChanged();
                            Toast.makeText(getContext(), "File deleted", Toast.LENGTH_SHORT).show();
                            //remove highlighting TODO
                        }else{
                            Toast.makeText(getContext(), "File not deleted", Toast.LENGTH_SHORT).show();
                        }
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(getContext(), "File not deleted, err: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Method to copy text to clipboard interfacing the android system:
     *
     * @param context
     * @param text text to be copied into clipboard
     * @param label label of the clipboard entry (only used in android 11 and above and not visible for the user?)
     */
    private void setClipboard(Context context, String text, @Nullable String label) {
        if(label == null|| label.isBlank()) label = "Copied Text from FFT Logger Resultbox";
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

    }

}