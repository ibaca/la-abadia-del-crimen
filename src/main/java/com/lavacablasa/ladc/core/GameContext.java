package com.lavacablasa.ladc.core;

public interface GameContext {
    // GFX
    void render();
    void setPixel(int x, int y, int color);
    void setColor(int color, byte r, byte g, byte b);
    // Input
    void process(int[] inputs);
    // Resources
    byte[] load(String resource);
    // Timer
    void interrupt();
    boolean processLogicInterrupt();
    boolean processVideoInterrupt();
    Promise<Void> sleep(int milliSeconds);
}
