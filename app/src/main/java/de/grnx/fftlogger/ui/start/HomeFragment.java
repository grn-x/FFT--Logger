package de.grnx.fftlogger.ui.start;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.MainActivity;
import de.grnx.fftlogger.R;

//        /import de.grnx.fftlogger.ui.MainActivity;
//        import de.grnx.fftlogger.ui.R;
public class HomeFragment extends Fragment {
    public static final boolean DontShowChart = true;
    private TextView tvFrequency;
    private TextView tvVolume;
    private TextView tvAmplitude;
    private TextView tvPerformance;

    private EditText highPassFilter;
    private EditText lowPassFilter;


    private HomeViewModel sharedViewModel;
    private LineChart chartFreqDomain;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_start, container, false);
        Button startButton = root.findViewById(R.id.start_button);
        Button stopButton = root.findViewById(R.id.stop_button);
        tvFrequency = root.findViewById(R.id.tv_frequency);
        tvVolume = root.findViewById(R.id.tv_volume);
        tvAmplitude = root.findViewById(R.id.tv_amplitude);
        tvPerformance = root.findViewById(R.id.tv_performance);
//        tvResultBox = root.findViewById(R.id.tvResultBox);
//        copyButton = root.findViewById(R.id.copy_button);
        chartFreqDomain = root.findViewById(R.id.chart_domain);

        highPassFilter = root.findViewById(R.id.editTextHighPassFilter);
        lowPassFilter = root.findViewById(R.id.editTextLowPassFilter);

        highPassFilter.setText(sharedViewModel != null ? String.valueOf(sharedViewModel.getHighPassFilter()) : "-1");
        lowPassFilter.setText(sharedViewModel != null ? String.valueOf(sharedViewModel.getLowPassFilter()) : "-1");
        setupChart(chartFreqDomain);


        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).startLogging();
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);

                highPassFilter.setEnabled(false);
                lowPassFilter.setEnabled(false);
                sharedViewModel.disableTV();

            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).stopLogging();
                stopButton.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);

                highPassFilter.setEnabled(true);
                lowPassFilter.setEnabled(true);
                sharedViewModel.enableTV();

            }
        });

        /*highPassFilter.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                sharedViewModel.setHighPassFilter(Double.parseDouble(s.toString()));
            }
        });*/

        highPassFilter.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    sharedViewModel.setHighPassFilter(highPassFilter.getText().toString().isBlank()?0d:Double.parseDouble(highPassFilter.getText().toString()));;
                    System.out.println("highPassFilter.getText().toString() = " + highPassFilter.getText().toString());
                }
            }
        });

        /*lowPassFilter.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                sharedViewModel.setLowPassFilter(Double.parseDouble(s.toString()));
            }
        });*/

        lowPassFilter.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {//todo implemen this for the other filter aswell? also the focus change really is a dumb solution since the graph kind of crashes when clicking elswhere (really buggy)
                if (!hasFocus) {
                    sharedViewModel.setLowPassFilter(lowPassFilter.getText().toString().isBlank()?0d:Double.parseDouble(lowPassFilter.getText().toString()));
                    System.out.println("lowPassFilter.getText().toString() = " + lowPassFilter.getText().toString());
                }
            }
        });
        //todo set view model variables and then go to main and grab those and then have them saved in the deque to string and then continue about writing the log file and then add the list view


        sharedViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        sharedViewModel.getCurrentDataRef().observe(getViewLifecycleOwner(), new Observer<RecordRContainer>() {
            @Override
            public void onChanged(RecordRContainer recordRContainer) {
                tvFrequency.setText(sharedViewModel.getFrequency());
                tvVolume.setText(sharedViewModel.getVolume());
                tvAmplitude.setText(sharedViewModel.getAmplitude());
                tvPerformance.setText(sharedViewModel.getPerformance());
                updateChart((LineChart) chartFreqDomain, recordRContainer);
            }
        });


        if (sharedViewModel.getLogging().getValue() != null && sharedViewModel.getLogging().getValue()) {
            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
        } else {
//            stopButton.setVisibility(View.GONE);
//            startButton.setVisibility(View.VISIBLE);
            //not needed since this is the default xml state
        }


        lowPassFilter.setEnabled(sharedViewModel.isTVEnabled());
        highPassFilter.setEnabled(sharedViewModel.isTVEnabled());
        return root;
    }

    public double getHighPassFilter() {
        return Double.parseDouble(highPassFilter.getText().toString());
    }

    public double getLowPassFilter() {
        return Double.parseDouble(lowPassFilter.getText().toString());
    }



    /** @deprecated
     * holy fuck this is causing a memory leak
     *  */
    private void updateChart_depr(LineChart chart, RecordRContainer dataset) {//all of this is fucking buggy. im gonna abandon this until i find the motivation to make it better
        chart.clear();
        chart.setData(null);
        List<Entry> entries = new ArrayList<>();

        double smallestElement = sharedViewModel.getHighPassFilter();
        double biggestElement = sharedViewModel.getLowPassFilter();

        for (int i = 0; i < dataset.frequencies().length; i++) {
            if (dataset.frequencies()[i] > smallestElement && dataset.frequencies()[i] < biggestElement && dataset.amplitudes()[i] > 0.1) {//why the fuck is amplitude negative anyways?
                entries.add(new Entry((float) dataset.frequencies()[i], (float) dataset.amplitudes()[i]));
            }
        }


        int iterator = 0;
        int counter = 0;
        double maxDiff = (biggestElement-smallestElement)/20;

        while(biggestElement - counter >= maxDiff){
            entries.add(new Entry(counter+=maxDiff, 0));
        }
        entries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));
        /*while (false&&counter < (int) biggestElement) { //digga scheiss drauf hier hab ich irgendwas gefickt
            if (iterator + 1 <= entries.size()-1 && entries.get(iterator + 1).getX() - entries.get(iterator).getX() <= maxDiff) {//test if neighbouring entries are within range
                counter+=maxDiff;
                iterator++;
                continue;
            } else {
                entries.add(iterator, new Entry(counter, 0));
                iterator++;
                if (counter - 1 >= entries.size()) {

                    while (biggestElement - counter > maxDiff) {
                        entries.add(iterator, new Entry(counter, 0));
                        counter+=maxDiff;
                        iterator++;
                    }
                    break; //does this break the loop completely?
                }
            }

        //if(list entry x is in range of next x entry range if not insert, if so advance 2 pioitners)
    }*/






    //System.out.println("Arrays.stream(dataset.frequencies()).map(f ->String.valueOf(f)).collect(Collectors.joining(\", \")) = " + Arrays.stream(dataset.frequencies()).mapToObj(f -> String.valueOf(f)).collect(Collectors.joining(", ")));
    // System.out.println("Arrays.stream(dataset.amplitudes()).map(f ->String.valueOf(f)).collect(Collectors.joining(\", \")) = " + Arrays.stream(dataset.amplitudes()).mapToObj(f -> String.valueOf(f)).collect(Collectors.joining(", ")));
    //System.out.println("Arrays.stream(dataset.decibels()).map(f ->String.valueOf(f)).collect(Collectors.joining(\", \")) = " + Arrays.stream(dataset.decibels()).mapToObj(f -> String.valueOf(f)).collect(Collectors.joining(", ")));

    LineData lineData = new LineData(new LineDataSet(new ArrayList<>(entries), "Frequency Domain"));


        chart.setData(lineData);
        chart.invalidate(); // refresh
}



    private void updateChart_fin(LineChart chart, RecordRContainer dataset) {//all of this is fucking buggy. im gonna abandon this until i find the motivation to make it better
        //if(true)return;
        chart.clear();
        chart.setData(null);
        List<Entry> entries = new ArrayList<>();

        double smallestElement = sharedViewModel.getHighPassFilter();
        double biggestElement = sharedViewModel.getLowPassFilter();

        for (int i = 0; i < dataset.frequencies().length; i++) {
            if (dataset.frequencies()[i] > smallestElement && dataset.frequencies()[i] < biggestElement && dataset.amplitudes()[i] > 0.1) {//why the fuck is amplitude negative anyways?
                entries.add(new Entry((float) dataset.frequencies()[i], (float) dataset.amplitudes()[i]));
            }
        }


        int iterator = 0;
        int counter = 0;
        double maxDiff = (biggestElement-smallestElement)/20;

        while(biggestElement - counter >= maxDiff){
            entries.add(new Entry(counter+=maxDiff, 0));
        }
        entries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));
        LineData lineData = new LineData(new LineDataSet(new ArrayList<>(entries), "Frequency Domain"));

        entries.clear();
        entries=null;
        //gc
        chart.setData(lineData); //this is causing a memeory leak, thanks

        chart.invalidate(); // refresh
    }

    private void updateChart(LineChart chart, RecordRContainer dataset) {//all of this is fucking buggy. im gonna abandon this until i find the motivation to make it better
        if(DontShowChart)return;
       // chart.clear();
       // chart.setData(null);
        List<Entry> entries = new ArrayList<>();

        double smallestElement = sharedViewModel.getHighPassFilter();
        double biggestElement = sharedViewModel.getLowPassFilter();

        for (int i = 0; i < dataset.frequencies().length; i++) {
            if (dataset.frequencies()[i] > smallestElement && dataset.frequencies()[i] < biggestElement && dataset.amplitudes()[i] > 0.1) {//why the fuck is amplitude negative anyways?
                entries.add(new Entry((float) dataset.frequencies()[i], (float) dataset.amplitudes()[i]));
            }
        }


        int iterator = 0;
        int counter = 0;
        double maxDiff = (biggestElement - smallestElement) / 20;

        while (biggestElement - counter >= maxDiff) {
            entries.add(new Entry(counter += maxDiff, 0));
        }
        entries.sort((e1, e2) -> Float.compare(e1.getX(), e2.getX()));
        LineData lineData = new LineData(new LineDataSet(new ArrayList<>(entries), "Frequency Domain"));

        System.out.println("chart.getLineData().getDataSetCount() = " + chart.getLineData().getDataSetCount());
        chart.getLineData().removeDataSet(0);
        chart.getLineData().addDataSet(new LineDataSet(new ArrayList<>(entries), "Frequency Domain"));
        chart.notifyDataSetChanged();
        chart.invalidate(); // refresh
        entries.clear();
        entries=null;
        //theres still a memory leak somewhere in here, its been hours of debugging and i cant find it, so the chart will remain off for now
    }

private void setupChart(LineChart chart) {
    Description description = new Description();
    description.setText("Frequency over time");
    chart.setDescription(description);
    int graphLabelColor = Color.BLACK;
    chart.getXAxis().setTextColor(graphLabelColor);
    chart.getAxisLeft().setTextColor(graphLabelColor);
    chart.getAxisRight().setTextColor(graphLabelColor);
    chart.getLegend().setTextColor(graphLabelColor);
    chart.getDescription().setTextColor(graphLabelColor);
    if(DontShowChart){
        var desc = new Description();
        desc.setText("This chart is disabled due to unresolved memory leaks =)");

        var desc2 = new Description();
        desc2.setText("The chart library is not releasing allocated chart datasets properly, read more here: https://github.com/grn-x");


        chart.setDescription(desc);
        //chart.setNoDataText(desc.getText() + "\n" + desc2.getText());
        chart.setNoDataText("Disabled due to memory leaks in the chart library, read GH issues");
        chart.invalidate();
    }else {

        chart.setData(new LineData());
    }

}


}