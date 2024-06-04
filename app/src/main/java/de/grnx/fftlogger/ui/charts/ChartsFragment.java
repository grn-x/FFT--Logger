package de.grnx.fftlogger.ui.charts;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.MainActivity;
import de.grnx.fftlogger.R;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.List;
import java.util.function.Consumer;


public class ChartsFragment extends Fragment {
    private ChartsViewModel sharedViewModel;
    private LineChart chartVol;
    private LineChart chartFreq;


//TODO when starting (and stopping) the logging without opening the graph once, the graph extends to the right on the x-axis without data
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_charts, container, false);
        MainActivity.chartsFragmentRef = this;
//        tvFrequency = root.findViewById(R.id.tv_frequency);
//        tvVolume = root.findViewById(R.id.tv_volume);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(ChartsViewModel.class);
        /*sharedViewModel.getFrequency().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double d) {
//                tvFrequency.setText(s);
                addEntry(chartFreq, d);
            }
        });*/
        /*sharedViewModel.getVolume().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double d) {
//                tvVolume.setText(s);
                addEntry2(chartVol, d, true);

            }
        });
        sharedViewModel.getAmplitude().observe(getViewLifecycleOwner(), new Observer<Double>() {
            @Override
            public void onChanged(Double d) {
//                tvVolume.setText(s);
                addEntry2(chartVol, d, false);
            }
        });*/

        sharedViewModel.getLatestCalc().removeObservers(getViewLifecycleOwner());

        Consumer<RecordRContainer> cOnsumer = new Consumer<RecordRContainer>() {
            @Override
            public void accept(RecordRContainer entries) {
                //this is only possible since the method that triggers this observer has already also updated the data linked lists.
                //its a weird solution but from my understanding the most efficient one since i cannot pass a universal entry and also instantiating the entries elsewhere is cleaner
                addEntry(chartFreq, sharedViewModel.getFrequencyData().getValue().getLast());
                addEntry2(chartVol, sharedViewModel.getVolumeData().getValue().getLast(), true);
                addEntry2(chartVol, sharedViewModel.getAmplitudeData().getValue().getLast(), false);
            }
        };
        Log.d("DebugTag", new String(sharedViewModel.getLatestCalc().hasObservers()+"bool"));
        Log.d("DebugTag","amps in create: " + sharedViewModel.getAmplitudeData().getValue().size());
        sharedViewModel.getLatestCalc().hasObservers();

        sharedViewModel.getLatestCalc().observe(getViewLifecycleOwner(), cOnsumer::accept);

        Log.d("DebugTag","cOnsumer.hashCode() = " + cOnsumer.hashCode());
        Log.d("DebugTag","getViewLifecycleOwner().hashCode() on create= " + getViewLifecycleOwner().hashCode());

        chartVol = root.findViewById(R.id.chart1);
        chartFreq = root.findViewById(R.id.chart2);

        setupChart2(chartVol);
        setupChart(chartFreq);

        return root;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save or serialize maybe urgent? //TODO FIXME
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //MainActivity.chartsFragmentRef = null; //todo im only saving an instance of the fragment in the main activity to be able to call the resetGraph method from the main activity but this may not even work at all since the fragment and the graphs might not even exist then anymore
        sharedViewModel.getLatestCalc().removeObservers(getViewLifecycleOwner());
        Log.d("DebugTag","getViewLifecycleOwner() onDestroy = " + getViewLifecycleOwner().hashCode());
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



//        LineDataSet dataSet = new LineDataSet(sharedViewModel.getFrequencyData().getValue(), "Frequency in Hz");
        LineDataSet dataSet = new LineDataSet((List<Entry>) sharedViewModel.getFrequencyData().getValue().clone(), "Frequency in Hz");
        LineData lineData = new LineData(dataSet);


        chart.setData(lineData);
        chart.invalidate(); // refresh
    }

    private void setupChart2(LineChart chart) {
        Description description = new Description();
        description.setText("Volume and Amplitude over time");
        chart.setDescription(description);
        chart.getXAxis().setTextColor(Color.WHITE);
        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setTextColor(Color.WHITE);
        chart.getLegend().setTextColor(Color.WHITE);
        chart.getDescription().setTextColor(Color.WHITE);

        boolean restore = true;

        LineDataSet volumeDataSet = new LineDataSet((List<Entry>) sharedViewModel.getVolumeData().getValue().clone(), "Volume in dB");
        volumeDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        volumeDataSet.setColor(Color.BLUE);
        volumeDataSet.setCircleColor(Color.BLUE);
        volumeDataSet.setValueTextColor(Color.BLUE);

//        LineDataSet amplitudeDataSet = new LineDataSet(sharedViewModel.getAmplitudeData().getValue(), "Amplitude");
        LineDataSet amplitudeDataSet = new LineDataSet(((List<Entry>) sharedViewModel.getAmplitudeData().getValue().clone()), "Amplitude");//thanks for fucking not specifying that this is using the reference to update its internal line
        amplitudeDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
        amplitudeDataSet.setColor(Color.RED);
        amplitudeDataSet.setCircleColor(Color.RED);

        LineData lineData = new LineData(volumeDataSet, amplitudeDataSet);


        chart.setData(lineData);


        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setTextColor(Color.BLUE);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        rightAxis.setTextColor(Color.RED);


        //chart.getXAxis().setGranularity(MainActivity.graphXgranularity);
        chart.invalidate();
    }

    private void addEntry_sync(LineChart chart, Entry entry) {
        LineData data = chart.getData();
        if (data != null) {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            /*if (set == null) {
                set = new LineDataSet(new ArrayList<>(), "Frequency in Hz");
                data.addDataSet(set);
            }*/

//            data.addEntry(new Entry(set.getEntryCount(), (float) value), 0);


//            data.addEntry(new Entry(elapsedTime, (float) value), data.getIndexOfDataSet(set));
            data.addEntry(entry, data.getIndexOfDataSet(set));//are you perhaps adding the data to your own dataset for the graph  AND to the dataset passed in the constructor? that is fucking ass. it doesnt observe the value changing and when the underlying set changes it also does work calling chart.notify dataset changed
            // so you need to add the values manually through chart.addEntry which also adds them to the reference from the constructor and fucks everything up. finding this shit out cost me hours and will also have a negative impact on performance since i need to clone the list every time im opening this framgnet
            //using a hashset is also not an option and converting from deque is probably slower than cloning
            //im so mad rn


            //sharedViewModel.getFrequencyData().getValue().add(new Entry(set.getEntryCount(), (float) value));
            //int size = sharedViewModel.getFrequencyData().getValue().size();
            //data.addEntry(sharedViewModel.getFrequencyData().getValue().get(size-1), data.getIndexOfDataSet(set));
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.getXAxis().setAxisMaximum(entry.getX());
            chart.setVisibleXRangeMaximum(MainActivity.graphXvisibleRange);
            chart.moveViewToX(entry.getX());//elapsedTime

        }
    }
private void addEntry(LineChart chart, Entry entry) {
        //chart.post(() -> addEntry_sync(chart, value));
        addEntry_sync(chart, entry);
    }
    private void addEntry2_sync(LineChart chart, Entry entry, boolean isVolume){
        LineData data = chart.getData();
        if (data != null) {
            // Get the data sets
            LineDataSet volumeDataSet = (LineDataSet) data.getDataSetByLabel("Volume in dB", true);//not using index because im a dumbass
            LineDataSet amplitudeDataSet = (LineDataSet) data.getDataSetByLabel("Amplitude", true);

            if(isVolume) {
                //data.addEntry(new Entry(volumeDataSet.getEntryCount(), (float) entry), data.getIndexOfDataSet(volumeDataSet)); // for more accurate and machine/power independent plotting the x axis value is now the elapsed time
//                sharedViewModel.getVolumeData().getValue().add(new Entry(volumeDataSet.getEntryCount(), (float) entry));
                data.addEntry(entry, data.getIndexOfDataSet(volumeDataSet));
//                sharedViewModel.getVolumeData().getValue().add(new Entry(elapsedTime, (float) entry));//isnt this already done by the view model add method? ill have to look into that when im not as tired //TODO
            } else {
//                data.addEntry(new Entry(amplitudeDataSet.getEntryCount(), (float) entry), data.getIndexOfDataSet(amplitudeDataSet));
//                sharedViewModel.getAmplitudeData().getValue().add(new Entry(amplitudeDataSet.getEntryCount(), (float) entry));
                data.addEntry(entry, data.getIndexOfDataSet(amplitudeDataSet));
                //sharedViewModel.getAmplitudeData().getValue().add(new Entry(elapsedTime, (float) entry));
            }
            data.notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.getXAxis().setAxisMaximum(entry.getX());
            chart.setVisibleXRangeMaximum(MainActivity.graphXvisibleRange);
            chart.moveViewToX(entry.getX());
        }
    }

    private void addEntry2(LineChart chart, Entry entry, boolean isVolume) {
        //chart.post(() -> addEntry2_sync(chart, entry, isVolume, elapsedTime));
        addEntry2_sync(chart, entry, isVolume);
    }

    /** this method resets the graph supposed to be called by {@link MainActivity#startLogging()}
     * this method also calls {@link ChartsViewModel#resetState()} to clear the datasets*/
    public void resetGraph() {
        //log points on graph
        Log.d("DebugTag","log points on graph");
        Log.d("DebugTag", String.valueOf(this.chartVol.getData().getDataSetByIndex(0).getEntryCount()));
        Log.d("DebugTag", String.valueOf(this.chartVol.getData().getDataSetByIndex(1).getEntryCount()));
        Log.d("DebugTag", String.valueOf(this.chartFreq.getData().getDataSetByIndex(0).getEntryCount()));

        sharedViewModel.resetState();

        this.chartFreq.clear();
        this.chartFreq.invalidate();

        this.chartVol.clear();
        this.chartVol.invalidate();
    }
}