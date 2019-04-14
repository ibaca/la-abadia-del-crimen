package com.lavacablasa.ladc.core;

public class TimingHandler {

    /** timer used to track elapsed time */
    private final Timer timer;

    /** number of interrupts per video update */
    private final int intsPerFrame;
    /** number of interrupts per logic update */
    private final int intsPerLogic;

    /** number of interrupts elapsed since the game started */
    private int ints;

    public TimingHandler(Timer timer,  int intsPerFrame, int intsPerLogic) {
        this.timer = timer;
        this.intsPerFrame = intsPerFrame;
        this.intsPerLogic = intsPerLogic;
        this.ints = 0;
    }

    public void interrupt() { ints++;}
    public boolean processLogicInterrupt() { return ints % intsPerLogic == 0;}
    public boolean processVideoInterrupt() { return ints % intsPerFrame == 0;}

    // sleeps for some time taking in account the actual frame-skip
    public void sleep(int milliSeconds) { timer.sleep(milliSeconds);}
}
