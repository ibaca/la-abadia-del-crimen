package com.lavacablasa.ladc.swing;

import com.lavacablasa.ladc.core.GfxOutput;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;

public class SwingGfxOutput implements GfxOutput {

    private final JFrame frame;
    private final BufferedImage buffer;
    private final int[] colors;

    public SwingGfxOutput(JFrame frame) {
        this.frame = frame;
        this.colors = new int[32];
        this.buffer = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void render() {
        BufferStrategy strategy = frame.getBufferStrategy();

        // Render single frame
        do {
            // The following loop ensures that the contents of the drawing buffer
            // are consistent in case the underlying surface was recreated
            do {
                // Get a new graphics context every time through the loop
                // to make sure the strategy is validated
                Graphics graphics = strategy.getDrawGraphics();

                // Render to graphics
                graphics.drawImage(buffer, 0, 0, 640, 400, null);

                // Dispose the graphics
                graphics.dispose();

                // Repeat the rendering if the drawing buffer contents
                // were restored
            } while (strategy.contentsRestored());

            // Display the buffer
            strategy.show();

            // Repeat the rendering if the drawing buffer was lost
        } while (strategy.contentsLost());
    }

    @Override
    public void setPixel(int x, int y, int color) {
        buffer.setRGB(x, y, colors[color]);
    }

    @Override
    public void setColor(int color, byte r, byte g, byte b) {
        this.colors[color] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }
}
