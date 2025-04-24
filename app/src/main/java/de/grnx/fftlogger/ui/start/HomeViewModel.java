package de.grnx.fftlogger.ui.start;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import de.grnx.fftlogger.DTOs.RecordRContainer;
import de.grnx.fftlogger.MainActivity;

public class HomeViewModel extends ViewModel {


    private final MutableLiveData<Boolean> isLogging = new MutableLiveData<>();

    private final MutableLiveData<String> resultBox = new MutableLiveData<>();
    private final MutableLiveData<RecordRContainer> currentData = new MutableLiveData<>();

    private final MutableLiveData<Double> highPassFilter = new MutableLiveData<>();
    private final MutableLiveData<Double> lowPassFilter = new MutableLiveData<>();
    private final MutableLiveData<Boolean> tvEnabled = new MutableLiveData<>(); //essentially the same as isLogging, but for the textview and not managed in the main activity but in the fragment via button clicks



    public void setCurrentData(RecordRContainer data){
        currentData.setValue(data);
    }

    public RecordRContainer getCurrentData(){
        return currentData.getValue();
    }

    public MutableLiveData<RecordRContainer> getCurrentDataRef(){
        return currentData;
    }


    public double getHighPassFilter() {
        return highPassFilter != null && highPassFilter.getValue() != null ? highPassFilter.getValue() : 0;
    }

    public void setHighPassFilter(double value) {
        highPassFilter.setValue(value);
    }

    public double getLowPassFilter() {
        return lowPassFilter != null && lowPassFilter.getValue() != null ? lowPassFilter.getValue() : 0; //MainActivity.sampleRate / 2; //nyquist frequency if not initialized //!! leave nyquist frequency limiting to the caller
    }
    public void setLowPassFilter(double value) {
        lowPassFilter.setValue(value);
    }


    public double[] getFrequencyDomain() {
        return currentData.getValue().frequencies();
    }

    public void setLogging(boolean value) {
        isLogging.setValue(value);
    }

    public LiveData<Boolean> getLogging() {
        return isLogging;
    }


    public String getFrequency() {
        //round using casts because this should be hotspot optimized and faster than other methods
        int i = (int)(currentData.getValue().frequency()*10);
        return ((double)i)/10+" Hz";
        //return currentData.getValue().frequency()+" Hz";
    }


    public String getVolume() {
        return currentData.getValue().volume()+ " dB";
    }


    public String getAmplitude() {
        return currentData.getValue().amplitude()+"";
    }



    public String getPerformance() {
        double d = (1 / ((currentData.getValue().operationTime() + 1)*0.001));
        return (int)d + "";
    }

    public void setResultBox(String value) {
        resultBox.setValue(value);
    }
    public LiveData<String> getResultBox() {
        return resultBox;
    }

    public void enableTV() {
        tvEnabled.setValue(true);
    }
    public void disableTV() {
        tvEnabled.setValue(false);
    }
    public LiveData<Boolean> getTVEnabled() {
        return tvEnabled;
    }
    public boolean isTVEnabled() {
        return tvEnabled.getValue() != null ? tvEnabled.getValue():true;
    }
}