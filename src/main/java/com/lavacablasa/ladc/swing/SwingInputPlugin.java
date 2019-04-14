package com.lavacablasa.ladc.swing;

import com.lavacablasa.ladc.core.Input;
import com.lavacablasa.ladc.core.InputPlugin;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SwingInputPlugin extends KeyAdapter implements InputPlugin {

    private static final Map<Integer, Input> INPUTS;

    static {
        Map<Integer, Input> inputs = new HashMap<>();
        inputs.put(KeyEvent.VK_DELETE, Input.SUPR);
        inputs.put(KeyEvent.VK_ENTER, Input.INTRO);
        inputs.put(KeyEvent.VK_SPACE, Input.SPACE);
        inputs.put(KeyEvent.VK_UP, Input.UP);
        inputs.put(KeyEvent.VK_DOWN, Input.DOWN);
        inputs.put(KeyEvent.VK_LEFT, Input.LEFT);
        inputs.put(KeyEvent.VK_RIGHT, Input.RIGHT);
        inputs.put(KeyEvent.VK_S, Input.S);
        inputs.put(KeyEvent.VK_N, Input.N);
        inputs.put(KeyEvent.VK_Q, Input.Q);
        inputs.put(KeyEvent.VK_R, Input.R);
        INPUTS = Collections.unmodifiableMap(inputs);
    }

    private Set<Input> pressedInputs = EnumSet.noneOf(Input.class);

    @Override
    public synchronized void process(int[] inputs) {
        for (Input input : Input.values()) {
            inputs[input.ordinal()] = pressedInputs.contains(input) ? 1 : 0;
        }
    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        Input input = INPUTS.get(e.getKeyCode());
        if (input != null) {
            pressedInputs.add(input);
        }
    }

    @Override
    public synchronized void keyReleased(KeyEvent e) {
        Input input = INPUTS.get(e.getKeyCode());
        if (input != null) {
            pressedInputs.remove(input);
        }
    }
}
