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

    public double getLow() {
        return low;
    }

    public double getHigh() {
        return high;
    }

    // remove the application of the nyquist frequency here, this should be done by the caller
    public void setLow(double low) {
        /*System.out.println("low: " + low);
        if (low <= 0) {
            this.low = MainActivity.sampleRate / 2;
        } else {
            this.low = low;
        }
        System.out.println("low after: " + this.low);*/

        this.low = low;
    }

    public void setHigh(double high) {
        this.high = high;
    }
}
