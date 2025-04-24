package de.grnx.fftlogger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import de.grnx.fftlogger.DTOs.DequeWrapper;
import de.grnx.fftlogger.DTOs.FrequencyBoundsExchange;
import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.FileHandling.FileCopyPermissionCallback;
import de.grnx.fftlogger.FileHandling.FileUtils;
import de.grnx.fftlogger.databinding.ActivityMainBinding;
import de.grnx.fftlogger.ui.charts.ChartsViewModel;
import de.grnx.fftlogger.ui.charts.ChartsFragment;
import de.grnx.fftlogger.ui.start.HomeViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements FileCopyPermissionCallback {

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

    public static FrequencyBoundsExchange passFilters = new FrequencyBoundsExchange(0,0);


    public static double initialLowPassFilter; //filters that get read and applied only at the start of the logging process, so that changes afterwards have no effect on the current logging process // maybe also block inputs when logging is active??
    public static double initialHighPassFilter;

    public static int execHandlerCounter = 0;
    public static int execSyncCounter = 0;

    public static final AtomicBoolean concurrentLogAccess = new AtomicBoolean(false); //this is to prevent saving the log file while the scheduler is still writing to it. the scheduler will change this value twice every cycle, so it should be safe to use this as a lock
    public static final AtomicBoolean concurrentRecorderLockWait = new AtomicBoolean(false); //this will be set inside the main Thread, when the concurrentLogAccess is set to true and the Main Activity wants to save the log data. this will be checked in the scheduler to wait for the main thread to finish saving the log data before continuing to write to the log file
//both of these could be volatile only since only one part is writing

    public static final int saveAfterCycles = 500; //this is the amount of cycles the scheduler will go through before saving the log data to the file. this is to prevent the file from being written to too often or not at all until save which would crash the jvm because of filled memory

    public static final float speedOfSound = 340f;

    public static File currentLogFile = null;

    private File fileToCopy;
    private String newFileName;

    private ViewPager2 fragmentMgr;

    public static int colorMode; //0 = light, 1 = dark, (-1 = system default) when undefined, default to dark mode

    public static MainActivity weirdStaticSelfReferenceForResultsFragmentToAccess;
    //in some cases the color mode of the phone is changed during runtime, if the user rejoins the app not through the main fragment which would always reevaluate the color mode, but through the results fragment, the color mode would not be updated. this is a workaround to get the activity reference in the results fragment to call reevaluateColorMode() on it. this is not a good solution a horrible one actually //TODO

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

       /* fragmentMgr = findViewById(R.id.viewpager);
        VPAdapter vpAdapter = new VPAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vpAdapter.addFragment(new HomeFragment());
        vpAdapter.addFragment(new ChartsFragment());
        vpAdapter.addFragment(new ResultsFragment());*/


        weirdStaticSelfReferenceForResultsFragmentToAccess = this;
        this.reevaluateColorMode();

    }

    public static MainActivity getInstance() {
        return weirdStaticSelfReferenceForResultsFragmentToAccess;
    }


    // returned int is unnecessary, since id go down the route of forcing the user to access the color mode through the public static color mode variable
public int reevaluateColorMode(){

    int nightModeFlags = getResources().getConfiguration().uiMode &
            Configuration.UI_MODE_NIGHT_MASK;
    switch (nightModeFlags) {
        case Configuration.UI_MODE_NIGHT_YES:
            colorMode = 1;
            break;

        case Configuration.UI_MODE_NIGHT_NO:
            colorMode = 0;
            break;

        case Configuration.UI_MODE_NIGHT_UNDEFINED:
//                colorMode = -1;
            colorMode=1;
            break;
    }

    return colorMode;

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


        recordScheduler = new RecordScheduler(this::writeLogData ,this::updateUIWithLogData,  this::callBack,this, passFilters);
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

        initialHighPassFilter = sharedViewModel.getHighPassFilter();
        initialLowPassFilter = sharedViewModel.getLowPassFilter();

        //if(initialHighPassFilter>initialLowPassFilter) initialHighPassFilter = initialHighPassFilter^initialLowPassFilter^(initialLowPassFilter = initialHighPassFilter);
        // cant xor swap doubles; nobody wouldve wanted to evaluate this anyways

        if(initialHighPassFilter > initialLowPassFilter) {
            double temp = initialHighPassFilter;
            initialHighPassFilter = initialLowPassFilter;
            initialLowPassFilter = temp;
            Toast.makeText(this, "The lower bound is higher than the maximum, swapping values", Toast.LENGTH_LONG).show();
        }




        String str;
        if(initialHighPassFilter<=0 && initialLowPassFilter <= 0){
            str = "Session started. The loudest frequency will appear here and in the graphs unfiltered. Note that the microphone sampling rate of " + sampleRate + " Hz limits the maximum recordable frequency to " + (sampleRate/2) + " Hz.";
            initialLowPassFilter = (double) sampleRate /2; // moved nyquist frequency limiting here; double cast should be unnecessary
        }else if(initialHighPassFilter<=0){
            str = "Session started. The loudest frequency up to " + initialLowPassFilter + " Hz will appear here and in the graphs";
        }else if(initialLowPassFilter <= 0){
            str = "Session started. The loudest frequency higher than " + initialHighPassFilter + " Hz will appear here and in the graphs";
        }else if(initialHighPassFilter>0&&initialLowPassFilter>0){
            str = "Session started. The loudest frequency between " + initialHighPassFilter+ " and " + initialLowPassFilter + " Hz will appear here and in the graphs";
        }else{
            str = "Session started. Error (the filters are probably set correctly but I messed up the ui logic)";
        }

        passFilters.setHigh(initialHighPassFilter);
        passFilters.setLow(initialLowPassFilter);

        dismissibleSnackbar(str, "Ok", 15000);

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

        //sharedViewModel.setResultBox(dequeToString(currentCollectionRef));
        saveLogSession();
        currentLogFile = null;
//        chartsViewModel.setFrequency(new RecordRContainer(0, 0,0, 0L,0L));
//        chartsViewModel.setVolume(new RecordRContainer(0, 0,0,0L,0L));
//        chartsViewModel.setAmplitude(new RecordRContainer(0, 0,0,0L,0L));
        //chartsViewModel.setLatestCalc(new RecordRContainer(0, 0,0,0L,0L));//is this needed? //TODO
        startTime =-1;
        passFilters.setHigh(-1d);
        passFilters.setLow(-1d);
    }


    int callbackCounter = 0;
        public void callBack(){
            if(!isLogging) return;
            if(callbackCounter++ > 100){
                System.out.println("callbackCounter = " + callbackCounter);
                callbackCounter = 0;
                saveLogPart();
            }
        }
    private void saveLogSession_dpr(){
        File subDir = checkSubDir();
        //get current date--time
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        String[] headers = new String[]{ "Logging session finished at: " + timeStamp.replace("_", " "),
        ("HighPassFilter: " + initialHighPassFilter + "\t LowPassFilter: " + initialLowPassFilter + " (-1/0 will be default and values below 1 ignored)"),
        ("elapsedTime; systemTime; operationTime; frequency; amplitude; decibel")};


        File outFile = new File(subDir, String.format("%s.txt", timeStamp));
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(outFile, true));
            out.print(dequeToString(currentCollectionRef, headers));
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(outFile.exists());
    }

    public void saveLogSession(){
        saveLogPart();
        String name = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime()).concat(".txt");
        currentLogFile.renameTo(new File(checkSubDir(), name)); //TODO check succession of renaming; alternatively choose a new name

    }

    private void saveLogPart() {
        System.out.println("saved: ");
        //System.out.println("currentCollectionRef.size() = " + currentCollectionRef.size());

        String[] headers = null;
        if(currentLogFile == null){
            File subDir = checkSubDir();
            //get current date--time
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
            headers = new String[]{ "Logging session started at: " + timeStamp.replace("_", " "),
                    ("HighPassFilter: " + initialHighPassFilter + "\t LowPassFilter: " + initialLowPassFilter + " (-1/0 will be default and values below 1 ignored)"),
                    ("elapsedTime; systemTime; operationTime; frequency; amplitude; decibel; 30 loudest frequencies (unfiltered)")};
            currentLogFile = new File(subDir, String.format("%s.raw", timeStamp));
        }

        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(currentLogFile, true));
            out.print(dequeToString(currentCollectionRef, headers));
            currentCollectionRef.clear();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private File checkSubDir(){
        File path = this.getFilesDir();
        File subDir = new File(path, "logData");

        if (!subDir.exists()) {
            subDir.mkdirs();
        }
        return subDir;
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
//        sharedViewModel.setFrequency(data);
//        sharedViewModel.setVolume(data);
//        sharedViewModel.setAmplitude(data);
//        sharedViewModel.setPerformance(data);
            sharedViewModel.setCurrentData(data);


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
    private void updateUIGraphWithLogData(RecordRContainer data) {
        if(isLogging){
            chartsViewModel.setLatestCalc(data);
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
    private String dequeToString(ArrayDeque<RecordRContainer> deque, String... headers) {
        StringBuilder sb = new StringBuilder();
        if (headers != null && headers.length >0){//why is this check unnecessary? is an array of length 0 always automatically null?
            for (String header : headers) {
                sb.append(header);
                sb.append("\n");
            }
    }
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

    //clear focus on touch outside of edit text; source: https://stackoverflow.com/questions/4165414/how-to-hide-soft-keyboard-on-android-after-clicking-outside-edittext and https://stackoverflow.com/questions/4828636/edittext-clear-focus-on-touch-outside
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }
public void dismissibleSnackbar(String message, String actionMessage, int duration){
    final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);

    snackBar.setAction(actionMessage, new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Call your action method here
            snackBar.dismiss();
        }
    });
snackBar.setTextMaxLines(10);
snackBar.setDuration(duration);
    snackBar.show();
}
    @Override
    public void requestFileCopyPermission(File file, String newFileName) {
        this.fileToCopy = file;
        this.newFileName = newFileName;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCodes.STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RequestCodes.STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FileUtils.copyFileToDownloads(this, fileToCopy, newFileName);
            } else {
                // Handle permission denial
            }
        }
    }
}