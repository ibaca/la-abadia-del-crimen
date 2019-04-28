package com.lavacablasa.ladc.swing;

import com.lavacablasa.ladc.core.GameContext;
import com.lavacablasa.ladc.core.Input;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JFrame;

public class SwingGameContext implements GameContext {
    private final JFrame frame;
    private final BufferedImage buffer;
    private final int[] colors;
    private final Set<Input> pressedInputs = Collections.synchronizedSet(EnumSet.noneOf(Input.class));

    public SwingGameContext() {
        JFrame frame = new JFrame("La Abadía del Crimen");
        frame.setSize(640, 400);
        frame.setBackground(Color.BLACK);
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIgnoreRepaint(true);
        frame.setLocationRelativeTo(null);
        frame.setCursor(createEmptyCursor());

        frame.addKeyListener(new KeyAdapter() {
            @Override public synchronized void keyPressed(KeyEvent e) {
                Input input = asInput(e.getKeyCode());
                if (input != null) pressedInputs.add(input);
            }

            @Override public synchronized void keyReleased(KeyEvent e) {
                Input input = asInput(e.getKeyCode());
                if (input != null) pressedInputs.remove(input);
            }
        });
        frame.setVisible(true);
        frame.createBufferStrategy(2);

        this.frame = frame;
        this.colors = new int[32];
        this.buffer = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
    }

    private Cursor createEmptyCursor() {
        Point hotSpot = new Point(0, 0);
        Image cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        return toolkit.createCustomCursor(cursorImage, hotSpot, "empty");
    }

    @Override public byte[] load(String resource) {
        try (InputStream input = SwingGameContext.class.getResourceAsStream(resource)) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load resource " + resource, e);
        }
    }

    @Override public void render() {
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

    @Override public void setPixel(int x, int y, int color) { buffer.setRGB(x, y, colors[color]);}

    @Override public void setColor(int color, byte r, byte g, byte b) {
        this.colors[color] = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
    }

    @Override public void process(int[] inputs) {
        for (Input input : Input.values()) inputs[input.ordinal()] = pressedInputs.contains(input) ? 1 : 0;
    }

    static Input asInput(int key) {
        switch (key) {
            case KeyEvent.VK_DELETE: return Input.SUPR;
            case KeyEvent.VK_ENTER: return Input.INTRO;
            case KeyEvent.VK_SPACE: return Input.SPACE;
            case KeyEvent.VK_UP: return Input.UP;
            case KeyEvent.VK_DOWN: return Input.DOWN;
            case KeyEvent.VK_LEFT: return Input.LEFT;
            case KeyEvent.VK_RIGHT: return Input.RIGHT;
            case KeyEvent.VK_S: return Input.S;
            case KeyEvent.VK_N: return Input.N;
            case KeyEvent.VK_Q: return Input.Q;
            case KeyEvent.VK_R: return Input.R;
        }
        return null;
    }
}
