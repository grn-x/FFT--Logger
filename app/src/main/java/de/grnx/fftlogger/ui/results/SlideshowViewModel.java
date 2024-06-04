package de.grnx.fftlogger.ui.results;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SlideshowViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public SlideshowViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Results of previous calculations will be displayed here.(when i implement it)");
    }

    public LiveData<String> getText() {
        return mText;
    }
}