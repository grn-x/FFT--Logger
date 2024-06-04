package de.grnx.fftlogger.ui.start;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import de.grnx.fftlogger.DTOs.RecordRContainer;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> frequency = new MutableLiveData<>();
    private final MutableLiveData<String> volume = new MutableLiveData<>();
    private final MutableLiveData<String> amplitude = new MutableLiveData<>();
    private final MutableLiveData<String> performance = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLogging = new MutableLiveData<>();

    private final MutableLiveData<String> resultBox = new MutableLiveData<>();

    public void setLogging(boolean value) {
        isLogging.setValue(value);
    }

    public LiveData<Boolean> getLogging() {
        return isLogging;
    }

    public void setFrequency(RecordRContainer value) {
        frequency.setValue(value.frequency()+" Hz");
    }

    public LiveData<String> getFrequency() {
        return frequency;
    }

    public void setVolume(RecordRContainer value) {
        volume.setValue(value.volume()+ " dB");
    }

    public LiveData<String> getVolume() {
        return volume;
    }

    public void setAmplitude(RecordRContainer value) {
        amplitude.setValue(value.amplitude()+ "");
    }

    public LiveData<String> getAmplitude() {
        return amplitude;
    }

    public void setPerformance(RecordRContainer value) {
        //performance.setValue((1 / ((value.operationTime() + 1)*0.001))+ ""); //+1 to avoid division by zero
        double d = (1 / ((value.operationTime() + 1)*0.001));
//        int i =(int)(d*1000);
//        float f = (float)i/1000;//round to 3 decimal places, casting is more efficient than using Math.round or decimal format because its hotspot optimized
        performance.setValue((int)d + "");
    }

    public LiveData<String> getPerformance() {
        return performance;
    }

    public void setResultBox(String value) {
        resultBox.setValue(value);
    }
    public LiveData<String> getResultBox() {
        return resultBox;
    }
}