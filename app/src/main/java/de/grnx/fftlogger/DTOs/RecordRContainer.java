package de.grnx.fftlogger.DTOs;

import androidx.annotation.NonNull;

import java.util.Arrays;

/** container for the record data double arrays planned for raw data, while the doubles are for the values after the filter was applied*/
public record RecordRContainer(double[] frequencies, int[] amplitudes, double[] decibels, double frequency, double amplitude, double volume, long systemTime, long elapsedTime, long operationTime) {
    /**deprecated!!! set frequency, amplitude and decibel not with the first slot but rather with after the frequencyboundsexchange object was checked */
    public RecordRContainer(double[] frequencies, int[] amplitudes, double[] decibels, long systemTime, long elapsedTime, long operationTime) {
        this(frequencies, amplitudes, decibels, frequencies[0], amplitudes[0], decibels[0], systemTime, elapsedTime, operationTime);
    }



    /**
     * @param frequency
     * @param amplitude
     * @param volume
     * @param systemTime
     * @param elapsedTime
     * @param operationTime
     */
    private RecordRContainer(double frequency, double amplitude, double volume, long systemTime, long elapsedTime, long operationTime) {
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
                "; v: " + volume +
                "; fs: " + Arrays.stream(frequencies).limit(30).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(", ")) //how efficient is this?

                 ;
    }


}
