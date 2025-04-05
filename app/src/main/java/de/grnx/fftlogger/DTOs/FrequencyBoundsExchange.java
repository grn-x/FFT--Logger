package de.grnx.fftlogger.DTOs;

import de.grnx.fftlogger.MainActivity;

/**
 * object to set the high pass filter and low pass filter values, wrapped inside this class to keep the reference when passing to scheduler
 */
public class FrequencyBoundsExchange {
    private double low;
    private double high;

    public FrequencyBoundsExchange(double low, double high) {
        setLow(low);
        setHigh(high);
    }
    //if low is negative it will be set to the nyquist frequency

    public double getLow() {
        return low;
    }

    public double getHigh() {
        return high;
    }

    public void setLow(double low) {
        System.out.println("low: " + low);
        if (low <= 0) {
            this.low = MainActivity.sampleRate / 2;
        } else {
            this.low = low;
        }
        System.out.println("low after: " + this.low);
    }

    public void setHigh(double high) {
        this.high = high;
    }
}
