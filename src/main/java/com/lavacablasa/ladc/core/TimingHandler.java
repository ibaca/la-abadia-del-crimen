package com.lavacablasa.ladc.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimingHandler {
    /** number of interrupts per video update */
    private final int intsPerFrame;
    /** number of interrupts per logic update */
    private final int intsPerLogic;

    /** number of interrupts elapsed since the game started */
    private int ints;

    private final ScheduledExecutorService eventLoop;

    public TimingHandler(int intsPerFrame, int intsPerLogic) {
        this.intsPerFrame = intsPerFrame;
        this.intsPerLogic = intsPerLogic;
        this.ints = 0;
        this.eventLoop = Executors.newSingleThreadScheduledExecutor();
    }

    public void interrupt() { ints++;}
    public boolean processLogicInterrupt() { return ints % intsPerLogic == 0;}
    public boolean processVideoInterrupt() { return ints % intsPerFrame == 0;}

    // sleeps for some time taking in account the actual frame-skip
    public Promise<Void> sleep(int milliSeconds) {
        Promise<Void> out = new Promise<>();
        eventLoop.schedule(() -> out.resolve(null), milliSeconds, TimeUnit.MILLISECONDS);
        return out;
    }
}
