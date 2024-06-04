package de.grnx.fftlogger.ui.start;
import android.content.Context;
import android.os.Bundle;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import de.grnx.fftlogger.MainActivity;
import de.grnx.fftlogger.R;

//        /import de.grnx.fftlogger.ui.MainActivity;
//        import de.grnx.fftlogger.ui.R;

public class HomeFragment extends Fragment {
    private TextView tvFrequency;
    private TextView tvVolume;
    private TextView tvAmplitude;
    private TextView tvPerformance;

    private TextView tvResultBox;
    private Button copyButton;
    private HomeViewModel sharedViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_start, container, false);
        Button startButton = root.findViewById(R.id.start_button);
        Button stopButton = root.findViewById(R.id.stop_button);
        tvFrequency = root.findViewById(R.id.tv_frequency);
        tvVolume = root.findViewById(R.id.tv_volume);
        tvAmplitude = root.findViewById(R.id.tv_amplitude);
        tvPerformance = root.findViewById(R.id.tv_performance);
        tvResultBox = root.findViewById(R.id.tvResultBox);
        copyButton = root.findViewById(R.id.copy_button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).startLogging();
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).stopLogging();
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);

            }
        });

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setClipboard(getContext(), tvResultBox.getText().toString(), null);
            }
        });


        sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        sharedViewModel.getFrequency().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvFrequency.setText(s);
            }
        });
        sharedViewModel.getVolume().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvVolume.setText(s);
            }
        });
        sharedViewModel.getAmplitude().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvAmplitude.setText(s);
            }
        });

        sharedViewModel.getPerformance().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvPerformance.setText(s);
            }
        });

        sharedViewModel.getResultBox().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                tvResultBox.setText(s);
            }
        });

        if(sharedViewModel.getLogging().getValue() != null && sharedViewModel.getLogging().getValue()) {
            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
        } else {
//            stopButton.setVisibility(View.GONE);
//            startButton.setVisibility(View.VISIBLE);
            //not needed since this is the default xml state
        }
        return root;
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