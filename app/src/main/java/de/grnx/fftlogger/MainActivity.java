package de.grnx.fftlogger;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import de.grnx.fftlogger.DTOs.DequeWrapper;
import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.databinding.ActivityMainBinding;
import de.grnx.fftlogger.ui.charts.ChartsViewModel;
import de.grnx.fftlogger.ui.charts.ChartsFragment;
import de.grnx.fftlogger.ui.start.HomeViewModel;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final ArrayList<ArrayDeque<RecordRContainer>> resultList = new ArrayList<>();
    private ArrayDeque<RecordRContainer> currentCollectionRef;//initialized and put in resultList in start logging, ref overwritten in next startlogging method execution(though still accessible through resultList)
    //done a few tests and it seems deque is faster than arraylist(ofc), linked list and even stack and vector (because of thread safety?) queue is not as nice to work with as deque
    //

    private DequeWrapper refHolder = new DequeWrapper();

    private boolean isLogging = false;


    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private HomeViewModel sharedViewModel;
    private ChartsViewModel chartsViewModel;
    public static ChartsFragment chartsFragmentRef;//managed by gallerview fragment itself, reference is destroyed when fragment is destroyed(hopefully) to avoid memory leaks and let the gc do its thing
    private RecordScheduler recordScheduler;


    private static long startTime;

    //config
    public static final int audioSource = MediaRecorder.AudioSource.DEFAULT;
    public static final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    public static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    public static final int sampleRate= 44100;
    public static final int schedulerIntervalMillis =1;
    public static final int graphXvisibleRange = 5000;
    public static final float graphXgranularity = 100f;
    public static final int dequeSize = 10000;
    /**
     * if true the scheduler will use a fixed delay scheduler, if false it will use a fixed rate scheduler
     * the latter is not recommended but was what i used originally for trying to get consistent results,
     * it turns out the calculation is pretty consistent itself and im using sys time to plot the data so this it not a concern anymore
     * changes affect code here: {@link RecordScheduler#start()}
     */
    public static final boolean useFixedDelayScheduler = false;



    public static int execHandlerCounter = 0;
    public static int execSyncCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        /*binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        sharedViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        chartsViewModel = new ViewModelProvider(this).get(ChartsViewModel.class);


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    protected void onResume() {
        super.onResume();


        recordScheduler = new RecordScheduler(this::writeLogData ,this::updateUIWithLogData, this);
        recordScheduler.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recordScheduler.stop();
    }

    public void startLogging() {
        recordScheduler.startTimer();
        currentCollectionRef = new ArrayDeque<>(dequeSize);
        resultList.add(currentCollectionRef);
        refHolder.setDeque(currentCollectionRef);
        isLogging = true;//can i combine these and let the mutable live data be a field of the owner activity? //TODO
        sharedViewModel.setLogging(true);
        try {
                chartsFragmentRef.resetGraph();
        } catch (Exception e) {
            e.printStackTrace(); //this is to be expected atleast once. the fragment neednt be created when the button is pressed because the viewholder handles the data, thus this nullpointer exception.
            // ill leave this universal catch statement here anyways because this is a dumb fucking way to solve this issue of non cleared graphs, also it might not be memory safe so if something happens look here //TODO
        }finally {
            chartsViewModel.resetState(); //calling this just to make sure. this should be handled inside the resetGraph method already. actually this is not needed since the critical data is logged inside the synchronous callback and not dependent on the ui or the graph
        }

        startTime = System.currentTimeMillis();
    }

    public void stopLogging() {
        Log.d("DebugTag","currentCollectionRef.getLast().elapsedTime() = " + currentCollectionRef.getLast().elapsedTime());
        Log.d("DebugTag","sync: " +currentCollectionRef.size());
        Log.d("DebugTag","handler " + execHandlerCounter);
        Log.d("DebugTag","sync " + execSyncCounter);
        Log.d("DebugTag","amp: " + chartsViewModel.getAmplitudeData().getValue().size());
        Log.d("DebugTag","vol: " + chartsViewModel.getVolumeData().getValue().size());
        Log.d("DebugTag","freq: " + chartsViewModel.getFrequencyData().getValue().size());
        ArrayDeque<Integer> durations = (ArrayDeque<Integer>) recordScheduler.stopTimer();
        long sum = durations.stream().mapToLong(Integer::longValue).sum();
        Log.d("DebugTag","num: " + durations.size());
        Log.d("DebugTag","avgDuration in millis: " + (float)sum/durations.size());


        execHandlerCounter = execSyncCounter = 0;
        isLogging = false;
        sharedViewModel.setLogging(false);
        sharedViewModel.setResultBox(dequeToString(currentCollectionRef));
//        chartsViewModel.setFrequency(new RecordRContainer(0, 0,0, 0L,0L));
//        chartsViewModel.setVolume(new RecordRContainer(0, 0,0,0L,0L));
//        chartsViewModel.setAmplitude(new RecordRContainer(0, 0,0,0L,0L));
        //chartsViewModel.setLatestCalc(new RecordRContainer(0, 0,0,0L,0L));//is this needed? //TODO
        startTime =-1;
    }

    public static long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    public static long getStartTime() {
        //if(startTime == -1) throw new IllegalStateException("start time not set");
        // this sanity check somehow fucks up the whole calculation without any signs of error, its just that nothing works anymore
        // i wasnt able to trace the error since im too tired and it literally throws non
        //id probalby need to use thousands of debug statements but thats too much work right now so ill rather trust that nobody tries to retrieve the start time before the timer has started and remove this statement
        return startTime;
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    private void updateUIWithLogData(RecordRContainer data) {
        sharedViewModel.setFrequency(data);
        sharedViewModel.setVolume(data);
        sharedViewModel.setAmplitude(data);
        sharedViewModel.setPerformance(data);

        if(isLogging){
            chartsViewModel.setLatestCalc(data);
            execHandlerCounter++;
//            chartsViewModel.setFrequency(data);
//            chartsViewModel.setVolume(data);
//            chartsViewModel.setAmplitude(data);

//            chartsViewModel.getAmplitudeData().getValue().add(new Entry(chartsViewModel.getAmplitudeData().getValue().size(), (float) entry));
//            chartsViewModel.getVolumeData().getValue().add(new Entry(chartsViewModel.getVolumeData().getValue().size(), (float) entry));
//            chartsViewModel.getFrequencyData().getValue().add(new Entry(chartsViewModel.getFrequencyData().getValue().size(), (float) value));

        }
    }

    private void writeLogData(RecordRContainer data) {
        if(isLogging) {
            this.refHolder.getDeque().add(data);
            execSyncCounter ++;
        }else{
            return;
        }
    }
    private String dequeToString(ArrayDeque<RecordRContainer> deque) {
        StringBuilder sb = new StringBuilder();
        sb.append("elapsedTime; systemTime; operationTime; frequency; amplitude; decibel\n");
        for(RecordRContainer r : deque) {
            sb.append(r.toString());
            sb.append("\n");
        }
        return sb.toString();
    }


    public boolean isLogging() {
        return isLogging;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordScheduler.terminate();
        recordScheduler = null;//gc
    }



}