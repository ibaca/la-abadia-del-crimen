package com.lavacablasa.ladc.core;

public class TimingHandler {
    // constants
    public static final int INTERRUPTS_PER_BASE_TIME_UPDATE = 12;
    public static final int FRAMESKIP_LEVELS = 12;
    public static final int FRAMES_PER_FPS_UPDATE = 12;

    private static final boolean[][] g_skipTable = {
        { false,false,false,false,false,false,false,false,false,false,false,false },
        { false,false,false,false,false,false,false,false,false,false,false, true },
        { false,false,false,false,false, true,false,false,false,false,false, true },
        { false,false,false, true,false,false,false, true,false,false,false, true },
        { false,false, true,false,false, true,false,false, true,false,false, true },
        { false, true,false,false, true,false, true,false,false, true,false, true },
        { false, true,false, true,false, true,false, true,false, true,false, true },
        { false, true,false, true, true,false, true,false, true, true,false, true },
        { false, true, true,false, true, true,false, true, true,false, true, true },
        { false, true, true, true,false, true, true, true,false, true, true, true },
        { false, true, true, true, true, true,false, true, true, true, true, true },
        { false, true, true, true, true, true, true, true, true, true, true, true }
    };

    private Timer timer;                       // timer used to track elapsed time

    private int numInterruptsPerSecond;         // number of interrupts per second
    private int numInterruptsPerVideoUpdate;    // number of interrupts per video update
    private int numInterruptsPerLogicUpdate;    // number of interrupts per logic update

    private int interruptNum;                   // number of interrupts elapsed since the game started

    private boolean throttle;                   // throttle speed

    private long thisFrameBase;                 // base time for the next interrupts
    private long lastFrameBase;                 // last time base for the last interrupts
    private long ticksPerMiliSecond;            // number of ticks per millisecond
    private double timePerInterrupt;            // time elapsed between two consecutive interrupts
    private double ticksPerSleepMiliSec;        // average of time elapsed for the lasts sleep(1) calls
    private double ticksPerSleepMiliSec2;       // average of time elapsed for the lasts sleep(1) calls

    // perfomance information to calculate FPS
    private long lastFpsTime;
    private int framesSinceLastFPS;
    private double currentFPS;

    private int frameSkipCnt;                   // current frameskip count
    private int videoFrameSkip;                 // number of video frames to skip
    private boolean lastVideoFrameSkipped;      // true if we skipped the last video frame

    private int numIntsModLogicInts;
    private int numIntsModVideoInts;
    private int numIntsModeBaseTimeUpdateInts;

    public TimingHandler(Timer timer, int intsPerSecond, int intsPerVideoUpdate, int intsPerLogicUpdate) {
        this.timer = timer;
        this.throttle = true;

        numInterruptsPerSecond = intsPerSecond;
        numInterruptsPerVideoUpdate = intsPerVideoUpdate;
        numInterruptsPerLogicUpdate = intsPerLogicUpdate;

        // init throttling and frame skipping data
        interruptNum = 0;
        frameSkipCnt = 0;
        videoFrameSkip = 0;

        // compute sleep time for 250 miliseconds
        long startTime = this.timer.getTime();
        this.timer.sleep(250);
        long endTime = this.timer.getTime();

        ticksPerSleepMiliSec = (double)(endTime - startTime) / 250.0;
        ticksPerSleepMiliSec2 = (double)(endTime - startTime) / 250.0;

        ticksPerMiliSecond = this.timer.getTicksPerSecond()/1000;
        timePerInterrupt = (double) this.timer.getTicksPerSecond()/(double)intsPerSecond;

        // init perfomance data
        framesSinceLastFPS = 0;
        currentFPS = 0.0;
        lastFpsTime = this.timer.getTime();

        lastFrameBase = lastFpsTime - (int)(INTERRUPTS_PER_BASE_TIME_UPDATE* numInterruptsPerVideoUpdate * timePerInterrupt);
    }

    public boolean processLogicThisInterrupt() {
        return numIntsModLogicInts == 0;
    }

    public boolean processVideoThisInterrupt()
    {
        return numIntsModVideoInts == 0;
    }

    public boolean skipVideoThisInterrupt()
    {
        lastVideoFrameSkipped = g_skipTable[videoFrameSkip][frameSkipCnt];
        return lastVideoFrameSkipped;
    }

    // getters & setters
    public double getCurrenFPS() {
        return currentFPS;
    }

    public boolean isThrottling() {
        return throttle;
    }

    public int getVideoFrameSkip() {
        return videoFrameSkip;
    }

    public void setVideoFrameSkip(int frameSkip) {
        videoFrameSkip = frameSkip;
    }

    public void setSpeedThrottle(boolean mode) {
        throttle = mode;
    }

    public long getTime() {
        return timer.getTime();
    }

    public long getTicksPerSecond() {
        return timer.getTicksPerSecond();
    }

    public void waitThisInterrupt()
    {
        numIntsModLogicInts = interruptNum % numInterruptsPerLogicUpdate;
        numIntsModVideoInts = interruptNum % numInterruptsPerVideoUpdate;

        // if we're throttling, wait until target time
        if (throttle){
            numIntsModeBaseTimeUpdateInts = interruptNum % (INTERRUPTS_PER_BASE_TIME_UPDATE* numInterruptsPerVideoUpdate);
            speedThrottle();
        }

        // if we have to update video in this interrupt, update frameskip count and check if we have to compute FPS
        if (numIntsModVideoInts == 0){
            computeFPS();
            frameSkipCnt = (frameSkipCnt + 1) % FRAMESKIP_LEVELS;
        }
    }

    public void endThisInterrupt()
    {
        interruptNum++;
    }

    // sleeps for some time taking in account the actual frameskip
    public void sleep(int milliSeconds)
    {
        // if we aren't throttling, return immediately
        if (!throttle){
            return;
        }

        // adjust milliseconds based on the frame skip level
        milliSeconds = (int)(milliSeconds*(((double)(FRAMESKIP_LEVELS - videoFrameSkip))/FRAMESKIP_LEVELS));

        long currentTime = timer.getTime();
        long targetTime = currentTime + ticksPerMiliSecond *(long)milliSeconds;

        // check if we have to wait a bit
        if (currentTime - targetTime < 0){
            // if we need to wait and have time to sleep, do it
            while (currentTime - targetTime < 0){
                if ((targetTime - currentTime) > ticksPerSleepMiliSec2 *1.10){
                    timer.sleep(1);
                    long nextTime = timer.getTime();

                    // evolutive adjust sleep time
                    ticksPerSleepMiliSec2 = ticksPerSleepMiliSec2 *0.85 + ((double)(nextTime - currentTime))*0.15;
                    currentTime = nextTime;
                } else {
                    currentTime = timer.getTime();
                }
            }
        }
    }

    private void computeFPS()
    {
        framesSinceLastFPS++;

        // if we didn't skip this video frame, adjust FPS if necessary
        if ((!lastVideoFrameSkipped) && (framesSinceLastFPS >= FRAMES_PER_FPS_UPDATE*(videoFrameSkip + 1))){
            long currentTime = timer.getTime();
            double secsElapsed = (double)(currentTime - lastFpsTime)*(1.0/ timer.getTicksPerSecond());

            // set perfomance data
            currentFPS = (double) framesSinceLastFPS/secsElapsed;

            // reset perfomance helper values
            lastFpsTime = currentTime;
            framesSinceLastFPS = 0;
        }
    }

    private void speedThrottle()
    {
        // recalculate base values each INTERRUPTS_PER_BASE_TIME_UPDATE*numInterruptsPerVideoUpdate frames
        if (numIntsModeBaseTimeUpdateInts == 0){
            thisFrameBase = lastFrameBase + (int)(INTERRUPTS_PER_BASE_TIME_UPDATE* numInterruptsPerVideoUpdate * timePerInterrupt);
        }

        long targetTime = 	thisFrameBase + (int)(numIntsModeBaseTimeUpdateInts * timePerInterrupt);
        long currentTime = timer.getTime();

        // check if we have to wait a bit
        if (currentTime - targetTime < 0){
            // if we need to wait and have time to sleep, do it
            while (currentTime - targetTime < 0){
                if ((targetTime - currentTime) > ticksPerSleepMiliSec *1.20){
                    timer.sleep(1);
                    long nextTime = timer.getTime();

                    // evolutive adjust sleep time
                    ticksPerSleepMiliSec = ticksPerSleepMiliSec *0.90 + ((double)(nextTime - currentTime))*0.10;
                    currentTime = nextTime;
                } else {
                    currentTime = timer.getTime();
                }
            }
        }

        // recalculate base values each INTERRUPTS_PER_BASE_TIME_UPDATE*numInterruptsPerVideoUpdate frames
        if (numIntsModeBaseTimeUpdateInts == 0){
            if ((currentTime - targetTime) > ticksPerSleepMiliSec){
                lastFrameBase = currentTime;
            } else {
                lastFrameBase = targetTime;
            }
        }
    }
}
