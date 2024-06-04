package de.grnx.fftlogger.ui.charts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikephil.charting.data.Entry;

import java.util.LinkedList;

import de.grnx.fftlogger.DTOs.RecordRContainer;

public class ChartsViewModel extends ViewModel {

    private MutableLiveData<RecordRContainer> latestCalc = new MutableLiveData<>();

    private MutableLiveData<LinkedList<Entry>> volumeData = new MutableLiveData<>(new LinkedList<>());
    private MutableLiveData<LinkedList<Entry>> amplitudeData = new MutableLiveData<>(new LinkedList<>());
    private MutableLiveData<LinkedList<Entry>> frequencyData = new MutableLiveData<>(new LinkedList<>());


    public void setLatestCalc(RecordRContainer value) {
        latestCalc.setValue(value);

        //freq
        double entryFreq = value.frequency();
        this.getFrequencyData().getValue().add(new Entry(value.elapsedTime(), (float) entryFreq));


        //vol
        double entryVol = value.volume();
        this.getVolumeData().getValue().add(new Entry(value.elapsedTime(), (float) entryVol));


        //amp
        double entryAmp = value.amplitude();
        this.getAmplitudeData().getValue().add(new Entry(value.elapsedTime(), (float) entryAmp));
    }

    public LiveData<RecordRContainer> getLatestCalc() {
        return latestCalc;
    }

    public LiveData<LinkedList<Entry>> getAmplitudeData() {
        return amplitudeData;
    }

    public LiveData<LinkedList<Entry>> getFrequencyData() {
        return frequencyData;
    }

    public LiveData<LinkedList<Entry>> getVolumeData() {
        return volumeData;
    }

    public void resetState() {
        /*if(latestCalc.getValue()!=null){
            Log.d("DebugTag","latestCalc.getValue().elapsedTime() = " + latestCalc.getValue().elapsedTime());
            //test for duplicates by comparing original length to length of set of unique hashes
            Log.d("DebugTag","freq: " + frequencyData.getValue().size());
            HashSet<Integer> freqHashes = new HashSet<>();
            frequencyData.getValue().forEach(e->{freqHashes.add(e.hashCode());});
            Log.d("DebugTag","frqHashes.size() = " + freqHashes.size());
        }*/

        this.amplitudeData= new MutableLiveData<>(new LinkedList<>());
        this.latestCalc = new MutableLiveData<>();
        this.volumeData = new MutableLiveData<>(new LinkedList<>());
        this.frequencyData = new MutableLiveData<>(new LinkedList<>());
    }


}
