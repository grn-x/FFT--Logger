package de.grnx.fftlogger;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import de.grnx.fftlogger.DTOs.FrequencyBoundsExchange;
import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.fftcalc.AudioCalculator;

/**
 * This class is responsible for scheduling the recording process.
 * inside the scheduled executor service a thread is created to record and process the audio
 */
public class RecordScheduler {

    public Consumer<RecordRContainer> uiController;
    public Consumer<RecordRContainer> resWriter;
    public Runnable callback;

    private final Handler uiHandler;
    private ScheduledExecutorService executorService;
    private final AudioCalculator audioCalculator;


    private final int minBufferSize;
    private final AudioRecord recorder;
    private final Runnable executorRunnable;

    private final FrequencyBoundsExchange fbe;

    private Deque<Integer> millisDuration = new ArrayDeque<>();

    /**
     * @param syncConsumer  Synchronous consumer that will be called with the result of the recording, made for non time intensive and critical operations such as pushing results to a collection
     * @param asyncConsumer Asynchronous consumer that will be called with the result of the recording, made for time intensive operations such as updating the ui
     * @param Callback      the callback that will be called after the recording has been processed and the consumers have been called
     * @param context       the activity under which the permission check and request is performed
     * @param fbe           the frequency bounds exchange object that will be used to filter the results
     */
    public RecordScheduler(Consumer<RecordRContainer> syncConsumer, Consumer<RecordRContainer> asyncConsumer, Runnable Callback , Activity context, FrequencyBoundsExchange fbe) {
        this.fbe = fbe;
        this.resWriter = syncConsumer;
        this.uiController = asyncConsumer;
        this.callback = Callback;


        uiHandler = new Handler(Looper.getMainLooper());
        audioCalculator = new AudioCalculator();
        minBufferSize = AudioRecord.getMinBufferSize(MainActivity.sampleRate, MainActivity.channelConfig, MainActivity.audioEncoding);


        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.RECORD_AUDIO}, RequestCodes.AUDIO);
        }
        recorder = new AudioRecord(MainActivity.audioSource, MainActivity.sampleRate, MainActivity.channelConfig, MainActivity.audioEncoding, minBufferSize); //tested in main activity so warning can be ignored
        executorRunnable = new Runnable() {
            @Override
            public void run() {
                /*while (MainActivity.concurrentRecorderLockWait.get()) { // do this to the resWriter instead, so that i have no downtime? and while blocking write to buffer arraylist that will be merged later? //TODO
                    //burn thread cycles until the arraylist gets released
                }
                MainActivity.concurrentLogAccess.set(false);*/
                long startTime = MainActivity.getCurrentTime();

                byte[] buffer = new byte[minBufferSize];

                long sysTimeCall = MainActivity.getCurrentTime();
                if (recorder.read(buffer, 0, minBufferSize) <= 0)
                    return; //in debug mode on my physical device this operation takes aboud 38ms (this is really really expensive i hope im not doing something wrong)
                // edit i believe i am doing something wrong. when testing with a different and much larger interval the whole operation only took between 2 and 6ms
                //nvm theres a bug somewhere it cannot be because of the sampling rate
                //but my initial assumption has proven correct, using fixed delay instead of fixed rate shows a time reduction of 10ms the frame is between 20 and 35ms. i think this is because of the executor service trying to start another thread while the previous one is still running
                //this could lead to race conditions and blocking, i dont know how the executor service handles this but the recorder that both are accessing will certainly not like it

                audioCalculator.setBytes(buffer);                   // suprisingly the fast fourier transform is indeed fast
                int amplitude = audioCalculator.getAmplitude();     // i expected this operation to be one of the most expensive ones
                double decibel = audioCalculator.getDecibel();      // instead all of this buffer copying and calculating only takes about 1.5ms
                double frequency = audioCalculator.getFrequency();  // this is okayish and amazing compared to the hardware recorder reading

                //test
                double[] frequencies = audioCalculator.getFrequencies();
                int[] amplitudes = audioCalculator.getAmplitudes();
                double[] decibels = audioCalculator.getDecibels();

               /*
                //i feel horrible for doing it this way but i currently cant see a better option
                if(fbe.getLow()<0&&fbe.getHigh()<0){
                    //do nothing

                }else if(fbe.getLow()<0){


                }else if(fbe.getHigh()<0){


                }


                if(fbe.getHigh()>MainActivity.sampleRate/2){//this is the nyquist frequency // deal with it? this is a TODO for later
                //for now compiler optimize this out please and thank you
                }*/

                //why dont i just let the lower bound be negative but set the higher bound to the nyquist frequency since this should be the maximum frequency that can be recorded anyway

                int i = 0;
                while((frequencies[i] >fbe.getLow()||frequencies[i] < fbe.getHigh())&&i<frequencies.length-1){
                    if(frequencies[i] >fbe.getLow()){
                        //System.out.println("too high: " + frequencies[i] + " > " + fbe.getLow());
                    }
                    if(frequencies[i] < fbe.getHigh()){
                        //System.out.println("too low: " + frequencies[i] + " < " + fbe.getHigh() + "\t" + fbe.getLow());
                    }
                    i++;

                }
                //System.out.println("new run \n\n");
               /* //low pass filter/if higher increment
                if(frequencies[i] >fbe.getLow()){
                    i++;
                }

                //high pass filter/ if lower increment
                if(frequencies[i] >fbe.getHigh()){
                    fbe.setHigh(-1);
                }


                while(frequencies[i] < fbe.getLow() || frequencies[i] > (fbe.getHigh()<0?MainActivity.sampleRate/2:fbe.getHigh()) && i < frequencies.length-1){//i am so sorry about the ternary operator getting executed each loop iteration and multiple times a second. this is just the beta version so ill call it fine
                    i++;
                }*/


                amplitude = amplitudes[i];
                decibel = decibels[i];
                frequency = frequencies[i];

               /* System.out.println("Arrays.toString(frequencies) = " + Arrays.toString(frequencies));
                System.out.println("Arrays.toString(amplitudes) = " + Arrays.toString(amplitudes));
                System.out.println("Arrays.toString(decibels) = " + Arrays.toString(decibels));

                System.out.println("amplitude = " + amplitude);
                System.out.println("decibel = " + decibel);
                System.out.println("frequency = " + frequency);*/

                //long sysTimeCall = MainActivity.getCurrentTime(); //this should get moved before the reading is done since the reading is the most expensive operation

                RecordRContainer result = new RecordRContainer(frequencies,  amplitudes, decibels,frequency, amplitude, decibel, sysTimeCall, sysTimeCall - MainActivity.getStartTime(), System.currentTimeMillis() - startTime);
                resWriter.accept(result);

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        uiController.accept(result);
                        //Log.d("DebugTag","result.toString() = " + result.toString());
                    }
                });
                //millisDuration.add((int)(System.currentTimeMillis()-startTime));
                //MainActivity.concurrentLogAccess.set(true);
                callback.run(); // just noticed i dont need any locks since the callback will block the executor service cycle anyways :D
            }
        };
    }

    public FrequencyBoundsExchange getFrequencyBoundsExchange(){
        return this.fbe;
    }

    /**
     * this method starts the recording, calculation and ui updating
     */
    public void start() {
        if (executorService == null || executorService.isShutdown())
            executorService = Executors.newSingleThreadScheduledExecutor();
        if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) return;
        if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) return;
        recorder.startRecording();
        if (MainActivity.useFixedDelayScheduler) {
            executorService.scheduleWithFixedDelay(executorRunnable, 0, MainActivity.schedulerIntervalMillis, TimeUnit.MILLISECONDS);
        } else {
            executorService.scheduleAtFixedRate(executorRunnable, 0, MainActivity.schedulerIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }


    /**
     * this method stops the recording, calculation and ui updating
     */
    public void stop() {
        try {
            executorService.shutdown();
            executorService = null;//gc
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) return;
            recorder.stop();
        }catch (Exception e){
            System.out.println("e = " + e);
            //fuck something crashed, id guess its a null pointer exception. i dont wanna have the logged data get wiped because something as stupid as this so here is this:
        }


    }

    /**
     * this method stops the recording, calculation and ui updating and releases the recorder, by doing so the object cannot be restarted now, and is unusable.
     */
    public void terminate() {
        this.stop();
        recorder.release();
//        recorder = null; //is being final a bigger performance benefit than letting it get cleaned up by the gc?
    }

    @Deprecated
/**i commented the timer out to avoid unnecessary overhead, this method is not needed anymore Edit 2; wtf does that even mean? its still in the code, i suggest removing the deprecated tag, though im not sure what any of this means anymore since i took a long break from developing*/
    public void startTimer() {
        millisDuration.clear();
    }

    @Deprecated
/**i commented the timer out to avoid unnecessary overhead, this method is not needed anymore */
    public Deque<Integer> stopTimer() {
        Deque<Integer> millisDuration = this.millisDuration;
        this.millisDuration = new ArrayDeque<>();//remove reference
        return millisDuration;
    }

}
