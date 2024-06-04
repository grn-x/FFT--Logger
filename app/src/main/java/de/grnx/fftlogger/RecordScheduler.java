package de.grnx.fftlogger;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.fftcalc.AudioCalculator;

/**
 * This class is responsible for scheduling the recording process.
 * inside the scheduled executor service a thread is created to record and process the audio
 */
public class RecordScheduler {

    public Consumer<RecordRContainer> uiController;
    public Consumer<RecordRContainer> resWriter;

    private final Handler uiHandler;
    private ScheduledExecutorService executorService;
    private final AudioCalculator audioCalculator;


    private final int minBufferSize;
    private final AudioRecord recorder;
    private final Runnable executorRunnable;


    private Deque<Integer> millisDuration = new ArrayDeque<>();

    /**
     * @param syncConsumer  Synchronous consumer that will be called with the result of the recording, made for non time intensive and critical operations such as pushing results to a collection
     * @param asyncConsumer Asynchronous consumer that will be called with the result of the recording, made for time intensive operations such as updating the ui
     * @param context       the activity under which the permission check and request is performed
     */
    public RecordScheduler(Consumer<RecordRContainer> syncConsumer, Consumer<RecordRContainer> asyncConsumer, Activity context) {
        this.resWriter = syncConsumer;
        this.uiController = asyncConsumer;


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

                //long sysTimeCall = MainActivity.getCurrentTime(); //this should get moved before the reading is done since the reading is the most expensive operation

                RecordRContainer result = new RecordRContainer(frequency, amplitude, decibel, sysTimeCall, sysTimeCall - MainActivity.getStartTime(), System.currentTimeMillis() - startTime);
                resWriter.accept(result);

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        uiController.accept(result);
                        //Log.d("DebugTag","result.toString() = " + result.toString());
                    }
                });
                //millisDuration.add((int)(System.currentTimeMillis()-startTime));
            }
        };
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
        executorService.shutdown();
        executorService = null;//gc

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) return;
        recorder.stop();
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
/**i commented the timer out to avoid unnecessary overhead, this method is not needed anymore */
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
