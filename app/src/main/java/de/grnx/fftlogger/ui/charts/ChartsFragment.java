package de.grnx.fftlogger.ui.charts;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.MainActivity;
import de.grnx.fftlogger.R;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

//TODO: fix leaving and rejoining fragment while selecting points
public class ChartsFragment extends Fragment {
    private ChartsViewModel sharedViewModel;
    private LineChart chartVol;
    private LineChart chartFreq;

    private Button point1;
    private Button point2;

    private TextView tvPoint1Freq;
    private TextView tvPoint2Freq;

    private TextView tvPoint1Time;
    private TextView tvPoint2Time;

    private TextInputEditText tvFreqInput;

    private static boolean choosingP1 = false;
    private static boolean choosingP2 = false;

    private TextView tvVApproach;
    private TextView tvVLeave;

    public static int graphLabelColor;



//TODO when starting (and stopping) the logging without opening the graph once, the graph extends to the right on the x-axis without data
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_charts, container, false);
        MainActivity.chartsFragmentRef = this;
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ChartsViewModel.class);

        graphLabelColor = Color.WHITE;
        try{
            if(MainActivity.getInstance().reevaluateColorMode() == 0) graphLabelColor = Color.BLACK;
        }catch (Exception e){ // I can definitely see this failing because of androids lifecycle management
            try{
                if(MainActivity.colorMode == 0) graphLabelColor = Color.BLACK;
            }catch (Exception e2){ // if the upper one fails, this should most definitely fail as well, but it worked before so idk
                Log.d("DebugTag", "onCreateView: " + e2.getMessage());
                e2.printStackTrace();
            }
        }


        chartVol = root.findViewById(R.id.chart1);
        chartFreq = root.findViewById(R.id.chart2);

        point1 = root.findViewById(R.id.button);
        point2 = root.findViewById(R.id.button2);

        tvPoint1Freq = root.findViewById(R.id.tv_freq1);
        tvPoint2Freq = root.findViewById(R.id.tv_freq2);

        tvPoint1Time = root.findViewById(R.id.tv_time1);
        tvPoint2Time = root.findViewById(R.id.tv_time2);

        tvFreqInput = root.findViewById(R.id.tv_original_frequency);

        tvVApproach = root.findViewById(R.id.label5);
        tvVLeave = root.findViewById(R.id.label6);

        choosingP1 = choosingP2 = false;

       tvFreqInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    if(s.toString().isEmpty()){
                        sharedViewModel.setOrigFreq(0d);

                        return;
                    }
                    sharedViewModel.setOrigFreq(Double.parseDouble(s.toString()));
                    if(tvFreqInput.hasFocus()) {
                        sharedViewModel.setYEstimatedFreq(null);
                        chartFreq.getAxis(YAxis.AxisDependency.LEFT).removeAllLimitLines();
                        var ll = new LimitLine(sharedViewModel.getOrigFreq().getValue().floatValue(), "Emitted Frequency");
                        ll.setTextColor(graphLabelColor);
                        chartFreq.getAxis(YAxis.AxisDependency.LEFT).addLimitLine(ll);
                        chartFreq.invalidate();
                    }
                } catch (Exception e) {

                    e.printStackTrace();
                    Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
                    Toast.makeText(getContext(), ""+e.getMessage() + " " + e.getCause(), Toast.LENGTH_SHORT).show();
                    tvFreqInput.setText("");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                return;

                /*//this is also triggered when the text is set programmatically (so I need to check if the text was empty or if the textfield had focus)
                try {
                    if(s.toString().isEmpty()){
                        sharedViewModel.setOrigFreq(0d);
                        return;
                    }
                    sharedViewModel.setOrigFreq(Double.parseDouble(s.toString()));
                    if(freqInput.hasFocus()) {
                        chartFreq.getAxis(YAxis.AxisDependency.LEFT).removeAllLimitLines();
                        chartFreq.getAxis(YAxis.AxisDependency.LEFT).addLimitLine(new LimitLine(sharedViewModel.getOrigFreq().getValue().floatValue(), "Emitted Frequency"));
                        //chartFreq.invalidate();
                    }
                } catch (Exception e) {

                e.printStackTrace();
                Toast.makeText(getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
                Toast.makeText(getContext(), ""+e.getMessage() + " " + e.getCause(), Toast.LENGTH_SHORT).show();
                freqInput.setText("");
                }*/
            }});

        tvFreqInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                        (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    tvFreqInput.clearFocus();
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });


        //observe shared view model in here and only change the mutable live data in the handler method and let the rest of the updating be done by the observer?
        if(sharedViewModel.getPoint1().getValue()!=null){
            tvPoint1Freq.setText(""+sharedViewModel.getPoint1().getValue().getY());
            tvPoint1Time.setText(""+sharedViewModel.getPoint1().getValue().getX());
        }
        if(sharedViewModel.getPoint2().getValue()!=null){
            tvPoint2Freq.setText(""+sharedViewModel.getPoint2().getValue().getY());
            tvPoint2Time.setText(""+sharedViewModel.getPoint2().getValue().getX());
        }

        if(sharedViewModel.getVApproach().getValue()!=null){
            tvVApproach.setText(""+sharedViewModel.getVApproach().getValue());
        }
        if(sharedViewModel.getVLeave().getValue()!=null){
            tvVLeave.setText(""+sharedViewModel.getVLeave().getValue());
        }



        boolean xPointPassingAllowed = false;

        //TODO orig frequency persists after fragment restart; also estimated frequency label changes to frequency label

        if(sharedViewModel.getYEstimatedFreq().getValue()!=null){
            chartFreq.getAxis(YAxis.AxisDependency.LEFT).removeAllLimitLines();
            var limitLine = new LimitLine(sharedViewModel.getYEstimatedFreq().getValue().floatValue(), "Estimated Emitted Frequency");
            limitLine.setTextColor(graphLabelColor);
            chartFreq.getAxis(YAxis.AxisDependency.LEFT).addLimitLine(limitLine);
            xPointPassingAllowed = true;
        }

        if(sharedViewModel.getOrigFreq().getValue()!=null){
            tvFreqInput.setText(""+sharedViewModel.getOrigFreq().getValue());
            chartFreq.getAxis(YAxis.AxisDependency.LEFT).removeAllLimitLines();
            var limitLine = new LimitLine(sharedViewModel.getOrigFreq().getValue().floatValue(), "Emitted Frequency");
            limitLine.setTextColor(graphLabelColor);
            chartFreq.getAxis(YAxis.AxisDependency.LEFT).addLimitLine(limitLine);
            xPointPassingAllowed = true;
        }else{
            tvFreqInput.setText("");

        }
        if(sharedViewModel.getXPointPassing().getValue()!=null&&xPointPassingAllowed){
            chartFreq.getXAxis().removeAllLimitLines();
            var horizontalLimitLine = new LimitLine(sharedViewModel.getXPointPassing().getValue().floatValue(), "Estimated Passing Point");
            horizontalLimitLine.setTextColor(graphLabelColor);
            chartFreq.getXAxis().addLimitLine(horizontalLimitLine);

        }


        point1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonHandler(v, point1);
            }
        });

        point2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonHandler(v, point2);
            }
        });

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



        chart.getXAxis().setTextColor(graphLabelColor);
        chart.getAxisLeft().setTextColor(graphLabelColor);
        chart.getAxisRight().setTextColor(graphLabelColor);
        chart.getLegend().setTextColor(graphLabelColor);
        chart.getDescription().setTextColor(graphLabelColor);



//        LineDataSet dataSet = new LineDataSet(sharedViewModel.getFrequencyData().getValue(), "Frequency in Hz");
        LineDataSet dataSet = new LineDataSet((List<Entry>) sharedViewModel.getFrequencyData().getValue().clone(), "Frequency in Hz");
        LineData lineData = new LineData(dataSet);

        chart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                chartPressHandler(e, h); //can i override this method with a lambda expression directly?
            }

            @Override
            public void onNothingSelected() {

            }
        });

        
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }

    private void setupChart2(LineChart chart) {
        Description description = new Description();
        description.setText("Volume and Amplitude over time");
        chart.setDescription(description);



        chart.getXAxis().setTextColor(graphLabelColor);
        chart.getAxisLeft().setTextColor(graphLabelColor);
        chart.getAxisRight().setTextColor(graphLabelColor);
        chart.getLegend().setTextColor(graphLabelColor);
        chart.getDescription().setTextColor(graphLabelColor);


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

    private void buttonHandler(View root, Button b) {
        if(b==point1){
            if(choosingP1){
                b.setText("Point 1");
                choosingP1 = false;
            }else{
                b.setText("Select");
                choosingP1 = true;
                if(choosingP2){
                    point2.setText("Point 2");
                    choosingP2 = false;
                }
            }
        } else if (b==point2){
            if(choosingP2){
                b.setText("Point 2");
                choosingP2 = false;
            }else{
                b.setText("Select");
                choosingP2 = true;
                if(choosingP1){
                    point1.setText("Point 1");
                    choosingP1 = false;
                }
            }
        }else{
            Log.d("DebugTag","buttonHandler: unknown button");
        }

    }

    private void chartPressHandler(Entry e, com.github.mikephil.charting.highlight.Highlight h){
        if(choosingP1){
            sharedViewModel.setPoint1(e);
            tvPoint1Freq.setText(""+ e.getY());
            int res = Math.round(e.getX()/100);
            float val;
            /*int res = Math.round(e.getX()/100);
            val = (float)res/10; */
            val = (float)e.getX()/1000f;
            System.out.println("val = " + val);
            tvPoint1Time.setText(String.format("%.2f", val));
        }else if(choosingP2){
            sharedViewModel.setPoint2(e);
            tvPoint2Freq.setText(""+ e.getY());
            float val;
            /*int res = Math.round(e.getX()/100);
            val = (float)res/10; */
            val = (float)e.getX()/1000f;
            System.out.println("val = " + val);
            tvPoint2Time.setText(String.format("%.2f", val));
        }else{
            return;
        }

        if(!tvPoint1Freq.getText().toString().isEmpty() && !tvPoint2Freq.getText().toString().isEmpty()){ //dont parse, do with sharedview model data instead //TODO
            /*if(Double.parseDouble(tvPoint1Freq.getText().toString())>Double.parseDouble(tvPoint2Freq.getText().toString())){
                Toast.makeText(getContext(), "Point 1 must be smaller than Point 2", Toast.LENGTH_LONG).show();//this cannot work as the observed audio source would first need to leave just to then approach
                return;
            }*/
            if(sharedViewModel.getPoint1().getValue().getY()<sharedViewModel.getPoint2().getValue().getY()){
                Toast.makeText(getContext(), "Point 1 must be of higher frequency than Point 2", Toast.LENGTH_LONG).show();//this cannot work as the observed audio source would first need to leave just to then approach
                return;
            }


            Entry p1 = sharedViewModel.getPoint1().getValue();
            Entry p2 = sharedViewModel.getPoint2().getValue();


            //if(!tvFreqInput.getText().toString().isEmpty() ){
            if((sharedViewModel.getOrigFreq().getValue()!=null)&&(sharedViewModel.getOrigFreq().getValue()>0d)){
                //double val = Double.parseDouble(tvFreqInput.getText().toString());

                if(Double.parseDouble(tvPoint1Freq.getText().toString())>Double.parseDouble(tvFreqInput.getText().toString()) && Double.parseDouble(tvFreqInput.getText().toString())>Double.parseDouble(tvPoint2Freq.getText().toString())){ // p1 greater than freq greater than p2 //perfect case

                   /* Why did i even do this, what was the point of all this garbage?
                    // create map of all values between p1 and p2
                    // filter out the single entry where the value is closest to the input frequency

                    List<Entry> filteredSubList = sharedViewModel.getFrequencyData().getValue().stream()
                            .filter(entry -> entry.getX() >= p1.getX() && entry.getX() <= p2.getX())
                            .collect(Collectors.toList());

                    System.out.println("filteredSubList.toString() = " + filteredSubList.toString());

                    Entry closest = filteredSubList.stream().reduce((a, b) -> Math.abs(a.getY() - val) < Math.abs(b.getY() - val) ? a : b).orElse(returnOptional());
                    System.out.println(closest);*/

                    float vApproach;// = (float) (MainActivity.speedOfSound * (1 - val / closest.getY()));
                    //vApproach = MainActivity.speedOfSound +(-MainActivity.speedOfSound *sharedViewModel.getOrigFreq().getValue())/sharedViewModel.getPoint1().getValue();
                    vApproach = (float)(MainActivity.speedOfSound + ((-MainActivity.speedOfSound * sharedViewModel.getOrigFreq().getValue()) / p1.getY()));


                    float vLeave;// = (float) (MainActivity.speedOfSound * (val / closest.getY() - 1));
                    vLeave = (float)(MainActivity.speedOfSound - ((MainActivity.speedOfSound * sharedViewModel.getOrigFreq().getValue()) / p2.getY()));

                    tvVApproach.setText(vApproach+" m/s");
                    tvVLeave.setText(vLeave+" m/s");
                    sharedViewModel.setVApproach((double) vApproach);
                    sharedViewModel.setVLeave((double) vLeave);

                    System.out.println("vApproach = " + vApproach);
                    System.out.println("vLeave = " + vLeave);

                    List<Entry> filteredSubList = sharedViewModel.getFrequencyData().getValue().stream()
                            .filter(entry -> entry.getX() >= p1.getX() && entry.getX() <= p2.getX())
                            .collect(Collectors.toList());


                    Entry closestEntry = filteredSubList.stream()
                            .min((a, b) -> Float.compare(Math.abs(a.getY() - sharedViewModel.getOrigFreq().getValue().floatValue()), Math.abs(b.getY() - sharedViewModel.getOrigFreq().getValue().floatValue())))
                            .orElse(new Entry(0, sharedViewModel.getOrigFreq().getValue().floatValue()));

                    System.out.println("filteredSubList.toString() = " + filteredSubList.toString());
                    System.out.println("middle = " + sharedViewModel.getOrigFreq().getValue());

                    System.out.println("closest = " + closestEntry.getY());




                    chartFreq.getXAxis().removeAllLimitLines();
                    var horizontalLimitLine = new LimitLine(closestEntry.getX(), "Estimated Passing Point");
                    horizontalLimitLine.setTextColor(graphLabelColor);
                    chartFreq.getXAxis().addLimitLine(horizontalLimitLine);

                    chartFreq.getAxis(YAxis.AxisDependency.LEFT).removeAllLimitLines();
                    var limitLine = new LimitLine(sharedViewModel.getOrigFreq().getValue().floatValue(), "Emitted Frequency");
                    limitLine.setTextColor(graphLabelColor);
                    chartFreq.getAxis(YAxis.AxisDependency.LEFT).addLimitLine(limitLine);
                    chartFreq.invalidate();

                    sharedViewModel.setOrigFreq(sharedViewModel.getOrigFreq().getValue());
                    sharedViewModel.setXPointPassing((double) closestEntry.getX());



                }else{ //case where one frequency is greater or lower than the emitted frequency, but there is no second one, so the vehicle was recorded leaving or approaching but not both
                    //for this to happen something must be wrong with the input, it is highly unlikely, but if the user still manages to correctly place the (there can only be one anymore) point, calculations might still succeed
                    Toast.makeText(getContext(), "Your range doesn't contain a turning point, calculations may be off", Toast.LENGTH_SHORT).show();

                    //double tDiff = p2.getX()- p1.getX();
                    double fDiff;


                    // if approaching: v = c * (1-val/freq)
                    if(p1.getY()>p2.getY()){
                        //moving towards
                        //fDiff = p1.getY()-p2.getY();
                        //float velocity = MainActivity.speedOfSound *(1-)
                    }else if(p2.getY()>p1.getY()){                    // if leaving: v = c * (val/freq -1)

                        //moving away
                        fDiff = p2.getY()-p1.getY();

                    }



                }
                return;
            }else{
                //Emitted Frequency is empty, find turning point? //TODO

                    // create map of all values between p1 and p2
                    // filter out the single entry where the value is closest to the input frequency
                    float middle = (p1.getY()+p2.getY())/2;

                    /*Map<Float, Float> filteredSubList = sharedViewModel.getFrequencyData().getValue().stream()
                            .filter(entry -> entry.getX() >= p1.getX() && entry.getX() <= p2.getX())
                            .collect(Collectors.toMap(Entry::getX, Entry::getY));

//                    Float[] entrySet = (Float[]) filteredSubList.entrySet().toArray(new Float[filteredSubList.size()]);
                    Float[] entrySet = (Float[]) filteredSubList.values().toArray(new Float[filteredSubList.size()]);

                    //damn dumb as hell, the list should be sorted by size since we are observing the passing vehicle, BUT IT NEEDNT! so all of this binary searching is useless because the list is unordered :)
                */
                /*float closest = filteredSubList.stream().map(Entry::getY)
                        .min((a, b) -> Float.compare(Math.abs(a - middle), Math.abs(b - middle)))
                        .orElse(middle);

                Map<Float, Float> filteredSubMap = sharedViewModel.getFrequencyData().getValue().stream()
                        .filter(entry -> entry.getX() >= p1.getX() && entry.getX() <= p2.getX())
                        .collect(Collectors.toMap(Entry::getX, Entry::getY));*/

                List<Entry> filteredSubList = sharedViewModel.getFrequencyData().getValue().stream()
                        .filter(entry -> entry.getX() >= p1.getX() && entry.getX() <= p2.getX())
                        .collect(Collectors.toList());


                Entry closestEntry = filteredSubList.stream()
                        .min((a, b) -> Float.compare(Math.abs(a.getY() - middle), Math.abs(b.getY() - middle)))
                        .orElse(new Entry(0, middle));

                    System.out.println("filteredSubList.toString() = " + filteredSubList.toString());
                System.out.println("middle = " + middle);

                System.out.println("closest = " + closestEntry.getY());




                chartFreq.getXAxis().removeAllLimitLines();
                var horizontalLimitLine = new LimitLine(closestEntry.getX(), "Estimated Passing Point");
                horizontalLimitLine.setTextColor(graphLabelColor);
                chartFreq.getXAxis().addLimitLine(horizontalLimitLine);

                chartFreq.getAxis(YAxis.AxisDependency.LEFT).removeAllLimitLines();
                var limitLine = new LimitLine(closestEntry.getY(), "Estimated Emitted Frequency");
                limitLine.setTextColor(graphLabelColor);
                chartFreq.getAxis(YAxis.AxisDependency.LEFT).addLimitLine(limitLine);
                chartFreq.invalidate();







                sharedViewModel.setOrigFreq(null);
                sharedViewModel.setYEstimatedFreq((double) closestEntry.getY());
                sharedViewModel.setXPointPassing((double) closestEntry.getX());
                tvFreqInput.setText(""+closestEntry.getY());

                float vApproach;// = (float) (MainActivity.speedOfSound * (1 - val / closest.getY()));
                //vApproach = MainActivity.speedOfSound +(-MainActivity.speedOfSound *sharedViewModel.getOrigFreq().getValue())/sharedViewModel.getPoint1().getValue();
                vApproach = (float)(MainActivity.speedOfSound + ((-MainActivity.speedOfSound * closestEntry.getY()) / p1.getY()));


                float vLeave;// = (float) (MainActivity.speedOfSound * (val / closest.getY() - 1));
                vLeave = (float)(MainActivity.speedOfSound - ((MainActivity.speedOfSound * closestEntry.getY()) / p2.getY()));

                sharedViewModel.setVApproach((double) vApproach);
                sharedViewModel.setVLeave((double) vLeave);

                tvVApproach.setText(vApproach+" m/s");
                tvVLeave.setText(vLeave+" m/s");


                //Entry closest = filteredSubList.stream().reduce((a, b) -> Math.abs(a.getY() - val) < Math.abs(b.getY() - val) ? a : b).orElse(returnOptional());
                    //System.out.println(closest);
            }
        }else{
//one or both points are not selected yet -> clear the calculated values
            tvVApproach.setText("");
            tvVLeave.setText("");
            return;}


    }




    /**@deprecated */
    public static float search(float value, Float[] a) {

        if(value < a[0]) {
            return a[0];
        }
        if(value > a[a.length-1]) {
            return a[a.length-1];
        }

        int lo = 0;
        int hi = a.length - 1;

        while (lo <= hi) {
            int mid = (hi + lo) / 2;

            if (value < a[mid]) {
                hi = mid - 1;
            } else if (value > a[mid]) {
                lo = mid + 1;
            } else {
                return a[mid];
            }
        }
        // lo == hi + 1
        return (a[lo] - value) < (value - a[hi]) ? a[lo] : a[hi];
    }

}