package com.lavacablasa.ladc.core;

public interface GfxOutput {
    // drawing methods
    void takeScreenShot();
    void render();
    void setPixel(int x, int y, int color);
    void setColor(int color, byte r, byte g, byte b);
}
