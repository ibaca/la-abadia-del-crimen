package com.lavacablasa.ladc.swing;

import com.lavacablasa.ladc.core.GameContext;
import com.lavacablasa.ladc.core.GfxOutput;
import com.lavacablasa.ladc.core.InputPlugin;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javax.swing.JFrame;

public class SwingGameContext implements GameContext {

    private final GraphicsDevice device;
    private final JFrame frame;

    private SwingGfxOutput gfxOutput;
    private SwingInputPlugin inputPlugin;

    public SwingGameContext() {
        inputPlugin = new SwingInputPlugin();

        frame = new JFrame("La Abadía del Crimen");
        frame.setSize(640, 480);
        frame.setBackground(Color.BLACK);
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIgnoreRepaint(true);
        frame.setLocationRelativeTo(null);
        frame.setCursor(createEmptyCursor());

        frame.addKeyListener(inputPlugin);
        frame.setVisible(true);
        frame.createBufferStrategy(2);

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        device = env.getDefaultScreenDevice();
        device.setFullScreenWindow(frame);
        device.setDisplayMode(new DisplayMode(640, 480, 32, DisplayMode.REFRESH_RATE_UNKNOWN));

        gfxOutput = new SwingGfxOutput(frame);
    }

    private Cursor createEmptyCursor() {
        Point hotSpot = new Point(0, 0);
        Image cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        return toolkit.createCustomCursor(cursorImage, hotSpot, "empty");
    }

    @Override public GfxOutput getGfxOutput() {
        return gfxOutput;
    }

    @Override public InputPlugin getInput() { return inputPlugin;
    }

    @Override public byte[] load(String resource) {
        try (InputStream input = SwingGameContext.class.getResourceAsStream(resource)) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load resource " + resource, e);
        }
    }
}
