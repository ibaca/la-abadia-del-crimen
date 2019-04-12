package com.lavacablasa.ladc.core;

import java.util.concurrent.TimeUnit;

// ITimer.h
//
//	Abstract class that defines the interface for a high resolution timer
//
//	A timer should implement 3 methods:
//		* getTime, that returns the actual number of ticks.
//		* getTicksPerSecond, that returns the number of ticks in a second.
//		* sleep, that stops execution for a specific time.
//
/////////////////////////////////////////////////////////////////////////////
public class Timer {
    public long getTime() {
        return System.nanoTime();
    }

    public long getTicksPerSecond() {
        return TimeUnit.SECONDS.toNanos(1);
    }

    public void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
