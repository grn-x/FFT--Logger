package de.grnx.fftlogger.DTOs;

import androidx.annotation.NonNull;

public record RecordRContainer(double[] frequencies, double[] amplitudes, double[] decibels, double frequency, double amplitude, double volume, long systemTime, long elapsedTime, long operationTime) {
    public RecordRContainer(double[] frequencies, double[] amplitudes, double[] decibels) {
        this(frequencies, amplitudes, decibels, 0.0D, 0, 0.0D, 0L, 0L, -1L);
    }

    /**
     * @param frequency
     * @param amplitude
     * @param volume
     * @param systemTime
     * @param elapsedTime
     * @param operationTime
     */
    public RecordRContainer(double frequency, double amplitude, double volume, long systemTime, long elapsedTime, long operationTime) {
        this(null, null, null, frequency, amplitude, volume, systemTime, elapsedTime, operationTime);
    }

    /**
     * @param frequency
     * @param amplitude
     * @param volume
     * @param systemTime
     * @param elapsedTime
     */
    public RecordRContainer(double frequency, double amplitude, double volume, long systemTime, long elapsedTime) {
        this(null, null, null, frequency, amplitude, volume, systemTime, elapsedTime, -1L);
    }

    public RecordRContainer() {
        this(null, null, null, 0.0D, 0, 0.0D, 0L, 0L, -1L);
    }

    @NonNull
    @Override
    public String toString(){
        return "et: " + elapsedTime +
                "; ot: " + operationTime +
                "; st: " + systemTime +
                "; f: " + frequency +
                "; a: " + amplitude +
                "; v: " + volume

                 ;
    }
}
