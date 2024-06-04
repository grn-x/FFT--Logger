package de.grnx.fftlogger.DTOs;

import java.util.ArrayDeque;

/** class that holds a dynamic reference to the deque used for data logging. this is important since the lamda inside the mainactivity that is passed to the recorder changes the deque object, to keep the reference it will be exchanged using this wrapper //though probably not even needed*/
public class DequeWrapper {
    private ArrayDeque<RecordRContainer> deque;

    public DequeWrapper() {
    }

    public DequeWrapper(ArrayDeque<RecordRContainer> deque) {
        this.deque = deque;
    }

    public ArrayDeque<RecordRContainer> getDeque() {
        return deque;
    }

    public void setDeque(ArrayDeque<RecordRContainer> deque) {
        this.deque = deque;
    }
}
