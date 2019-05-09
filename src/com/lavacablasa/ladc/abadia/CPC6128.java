package com.lavacablasa.ladc.abadia;

import com.lavacablasa.ladc.core.GameContext;

/**
 * Clase de ayuda para realizar operaciones gráficas de amstrad.
 */
public class CPC6128 {

    /////////////////////////////////////////////////////////////////////////////
    // CPC 6128 palette
    /////////////////////////////////////////////////////////////////////////////

    private static final byte[] MODE_0_DECODE;
    private static final byte[] MODE_1_DECODE;

    static {
        MODE_0_DECODE = new byte[256];
        MODE_1_DECODE = new byte[256];
        for (int i = 0; i < 256; i++) {
            MODE_0_DECODE[i] = (byte) (unpackPixelMode0(i, 0) | unpackPixelMode0(i, 1) << 4);
            MODE_1_DECODE[i] = (byte) (unpackPixelMode1(i, 0) | unpackPixelMode1(i, 1) << 2
                    | unpackPixelMode1(i, 2) << 4 | unpackPixelMode1(i, 3) << 6);
        }
    }

    // hardware colors
    private static final int[][] HARDWARE_PALETTE = {
            { 0x080, 0x080, 0x080 },    // 00 white
            { 0x080, 0x080, 0x080 },    // 01 white
            { 0x000, 0x0ff, 0x080 },    // 02 sea green
            { 0x0ff, 0x0ff, 0x080 },    // 03 pastel yellow
            { 0x000, 0x000, 0x080 },    // 04 blue
            { 0x0ff, 0x000, 0x080 },    // 05 purple
            { 0x000, 0x080, 0x080 },    // 06 cyan
            { 0x0ff, 0x080, 0x080 },    // 07 pink
            { 0x0ff, 0x000, 0x080 },    // 08 purple
            { 0x0ff, 0x0ff, 0x080 },    // 09 pastel yellow
            { 0x0ff, 0x0ff, 0x000 },    // 10 bright yellow
            { 0x0ff, 0x0ff, 0x0ff },    // 11 bright white
            { 0x0ff, 0x000, 0x000 },    // 12 bright red
            { 0x0ff, 0x000, 0x0ff },    // 13 bright magenta
            { 0x0ff, 0x080, 0x000 },    // 14 orange
            { 0x0ff, 0x080, 0x0ff },    // 15 pastel magenta
            { 0x000, 0x000, 0x080 },    // 16 blue
            { 0x000, 0x0ff, 0x080 },    // 17 sea green
            { 0x000, 0x0ff, 0x000 },    // 18 bright green
            { 0x000, 0x0ff, 0x0ff },    // 19 bright cyan
            { 0x000, 0x000, 0x000 },    // 20 black
            { 0x000, 0x000, 0x0ff },    // 21 bright blue
            { 0x000, 0x080, 0x000 },    // 22 green
            { 0x000, 0x080, 0x0ff },    // 23 sky blue
            { 0x080, 0x000, 0x080 },    // 24 magenta
            { 0x080, 0x0ff, 0x080 },    // 25 pastel green
            { 0x080, 0x0ff, 0x080 },    // 26 lime
            { 0x080, 0x0ff, 0x0ff },    // 27 pastel cyan
            { 0x080, 0x000, 0x000 },    // 28 red
            { 0x080, 0x000, 0x0ff },    // 29 mauve
            { 0x080, 0x080, 0x000 },    // 30 yellow
            { 0x080, 0x080, 0x0ff }     // 31 pastel blue
    };

    private final GameContext gfx;
    private final byte[] screenBuffer = new byte[16000];
    private int mode = 0;

    public CPC6128(GameContext gfx) {
        this.gfx = gfx;
    }

    public void setMode(int mode) {
        assert (mode == 0 || mode == 1);
        this.mode = mode;
    }

    public void setHardwareColor(int numInk, int color) {
        assert ((numInk >= 0) && (numInk < 16));

        int[] rgb = HARDWARE_PALETTE[color];
        gfx.setColor(numInk, (byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
    }

    // sets a pixel in mode 1 (320x200, x pixels = 2 width, y pixels = 2 height, 4 colors)
    public void setMode1Pixel(int x, int y, int color) {
        assert ((x >= 0) && (x < 320)) && ((y >= 0) && (y < 200)) && ((color >= 0) && (color < 4));

        int pos = (80 * y) + (x >>> 2);
        screenBuffer[pos] = (byte) packPixelMode1(screenBuffer[pos] & 0xff, (x & 0x03), color);
    }

    public void scrollLine(int xHalfChar, int yLine, int width) {
        for (int y = 0; y < 8; y++) {
            int pos = (yLine + y) * 80 + xHalfChar;
            for (int x = 0; x < (2 * width) - 2; x += 2) {
                screenBuffer[pos + x] = screenBuffer[pos + x + 2];
                screenBuffer[pos + x + 1] = screenBuffer[pos + x + 3];
            }
        }
    }

    // gets a pixel in mode 1 (320x200, 4 colors)
    public int getMode1Pixel(int x, int y) {
        assert ((x >= 0) && (x < 320)) && ((y >= 0) && (y < 200));

        int pos = (80 * y) + (x >>> 2);
        return unpackPixelMode1(screenBuffer[pos], (x & 0x03));
    }

    public void showMode0Screen(byte[] data) {
        for (int y = 0; y < 200; y++) {
            int sourceLinePos = (y & 0x07) * 0x800 + (y >> 3) * 80;
            int targetLinePos = y * 80;
            System.arraycopy(data, sourceLinePos, screenBuffer, targetLinePos, 80);
        }
    }

    // rectangle filling

    // fills a rectangle in mode 1 (320x200, 4 colors)
    public void fillMode1Rect(int x0, int y0, int width, int height, int color) {
        assert ((x0 >= 0) && (x0 < 320)) && ((y0 >= 0) && (y0 < 200)) && ((color >= 0) && (color < 4));
        assert (((x0 + width) <= 320) && ((y0 + height) <= 200));

        int x1 = x0 + width;
        int y1 = y0 + height;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int pos = y * 80 + (x >>> 2);
                screenBuffer[pos] = (byte) packPixelMode1(screenBuffer[pos], x & 0x03, color);
            }
        }
    }

    // pixel unpacking
    public static int unpackPixelMode0(int data, int pixel) {
        return (((data >> (1 - pixel)) & 0x01) << 3) |
                (((data >> (5 - pixel)) & 0x01) << 2) |
                (((data >> (3 - pixel)) & 0x01) << 1) |
                (((data >> (7 - pixel)) & 0x01) << 0);
    }

    public static int unpackPixelMode1(int data, int pixel) {
        return (((data >> (3 - pixel)) & 0x01) << 1) | ((data >> (7 - pixel)) & 0x01);
    }

    // array with the four colors
    private static final int[] MODE_1_BYTE_COLORS = { 0b00000000, 0b11110000, 0b00001111, 0b11111111 };

    // pixel packing
    public int packPixelMode1(int oldByte, int pixel, int color) {
        assert ((pixel >= 0) && (pixel < 4));
        assert ((color >= 0) && (color < 4));

        // find out the 2 bits of the new pixel
        int mask = 0b10001000;
        mask = mask >> pixel;

        // save the other pixels
        oldByte = (oldByte & (~mask)) & 0xff;

        // combines the other pixels with the new pixel
        return oldByte | (MODE_1_BYTE_COLORS[color] & mask);
    }

    public synchronized void render() {
        switch (mode) {
            case 0: renderMode0(); break;
            case 1: renderMode1(); break;
        }
        gfx.render();
    }

    private void renderMode0() {
        int pos = 0;
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 80; x++) {
                int value = MODE_0_DECODE[screenBuffer[pos++] & 0xff];
                int col0 = (value >>> 0) & 0x0f;
                int col1 = (value >>> 4) & 0x0f;
                gfx.setPixel(4 * x, y, col0);
                gfx.setPixel(4 * x + 1, y, col0);
                gfx.setPixel(4 * x + 2, y, col1);
                gfx.setPixel(4 * x + 3, y, col1);
            }
        }
    }

    private void renderMode1() {
        int pos = 0;
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 80; x++) {
                int value = MODE_1_DECODE[screenBuffer[pos++] & 0xff];
                gfx.setPixel(4 * x + 0, y, (value >>> 0) & 0x03);
                gfx.setPixel(4 * x + 1, y, (value >>> 2) & 0x03);
                gfx.setPixel(4 * x + 2, y, (value >>> 4) & 0x03);
                gfx.setPixel(4 * x + 3, y, (value >>> 6) & 0x03);
            }
        }
    }
}
