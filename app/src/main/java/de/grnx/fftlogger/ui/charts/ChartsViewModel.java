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

        point1.setValue(null);
        point2.setValue(null);
        origFreq.setValue(null);
        vApproach.setValue(null);
        vLeave.setValue(null);
        xPointPassing.setValue(null);
        //yEstimatedFreq.setValue(null);

    }

    private MutableLiveData<Entry> point1 = new MutableLiveData<>();
    private MutableLiveData<Entry> point2 = new MutableLiveData<>();

    private MutableLiveData<Double> origFreq = new MutableLiveData<>();

    private MutableLiveData<Double> vApproach = new MutableLiveData<>(); //should be float

    private MutableLiveData<Double> vLeave = new MutableLiveData<>(); //should be float

    private MutableLiveData<Double> xPointPassing = new MutableLiveData<>(); //should be float

    private MutableLiveData<Double> yEstimatedFreq = new MutableLiveData<>(); //should be float //if this is null. and origFreq isnt null, then the user entered the frequency manually -> use origFreq

    private MutableLiveData<Boolean> detectionMode = new MutableLiveData<>(true); //true = auto, false = manual

    public LiveData<Double> getVApproach() {
        return vApproach;
    }

    public LiveData<Double> getVLeave() {
        return vLeave;
    }

    public LiveData<Double> getXPointPassing() {
        return xPointPassing;
    }

    @Deprecated //manage everything with origFreq, because of new manual mode
    public LiveData<Double> getYEstimatedFreq() {
        return yEstimatedFreq;
    }

    public void setVApproach(Double value) {
        vApproach.setValue(value);
    }

    public void setVLeave(Double value) {
        vLeave.setValue(value);
    }

    public void setXPointPassing(Double value) {
        xPointPassing.setValue(value);
    }

    @Deprecated //manage everything with origFreq, because of new manual mode
    public void setYEstimatedFreq(Double value) {
        yEstimatedFreq.setValue(value);
    }



    public void setPoint1(Entry value) {
        point1.setValue(value);
    }

    public void setPoint2(Entry value) {
        point2.setValue(value);
    }

    public void setOrigFreq(Double value) {
        origFreq.setValue(value);
    }

    public LiveData<Entry> getPoint1() {
        return point1;
    }

    public LiveData<Entry> getPoint2() {
        return point2;
    }

    public LiveData<Double> getOrigFreq() {
        return origFreq;
    }


    public LiveData<Boolean> getDetectionMode() {
        return detectionMode;
    }

    public void setDetectionMode(Boolean value) {
        detectionMode.setValue(value);
    }


}
