package com.lavacablasa.ladc.core;

import com.lavacablasa.ladc.abadia.DskReader;
import java.util.function.BiConsumer;

public abstract class GameContext {
    // GFX
    public abstract void render();
    public abstract void setPixel(int x, int y, int color);
    public abstract void setColor(int color, byte r, byte g, byte b);
    // Input
    public abstract void process(int[] inputs);
    // Timer
    protected abstract BiConsumer<Runnable, Integer> getTimer();
    public Promise<Void> sleep(int milliSeconds) { return Promise.sleep(getTimer(), milliSeconds);}
    // Disk utils
    protected static byte[] readDiskImageToMemory(byte[] diskImageData) {
        byte[] auxBuffer = new byte[0xff00];
        byte[] memoryData = new byte[0x24000];
        DskReader dsk = new DskReader(diskImageData);

        for (int i = 0; i <= 16; i++) dsk.getTrackData(i + 0x01, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x00000, 0x4000);    // abadia0.bin
        reOrderAndCopy(auxBuffer, 0x4000, memoryData, 0x0c000, 0x4000);    // abadia3.bin
        reOrderAndCopy(auxBuffer, 0x8000, memoryData, 0x20000, 0x4000);    // abadia8.bin
        reOrderAndCopy(auxBuffer, 0xc000, memoryData, 0x04100, 0x3f00);    // abadia1.bin
        for (int i = 0; i <= 4; i++) dsk.getTrackData(i + 0x12, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x1c000, 0x4000);    // abadia7.bin
        for (int i = 0; i <= 4; i++) dsk.getTrackData(i + 0x17, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x18000, 0x4000);    // abadia6.bin
        for (int i = 0; i <= 5; i++) dsk.getTrackData(i + 0x1c, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x14000, 0x4000);    // abadia5.bin
        for (int i = 0; i <= 4; i++) dsk.getTrackData(i + 0x21, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x08000, 0x4000);    // abadia2.bin

        return memoryData;
    }
    static void reOrderAndCopy(byte[] src, int srcPos, byte[] dst, int dstPos, int size) {
        for (int i = 0; i < size; i++) dst[dstPos + size - i - 1] = src[srcPos + i];
    }
}
