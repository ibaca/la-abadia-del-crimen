package com.lavacablasa.ladc.abadia;

import com.lavacablasa.ladc.core.GameContext;
import com.lavacablasa.ladc.core.Input;

class Controles {
    public static final int NUM_INPUTS = Input.values().length;

    private final GameContext context;
    private final int[] inputs = new int[NUM_INPUTS];
    private final int[] inputsHistory = new int[NUM_INPUTS];

    Controles(GameContext context) {
        this.context = context;
    }

    void actualizaEstado() {
        context.process(inputs);

        // combina el estado actual de los controles con el anterior para poder detectar pulsaciones
        for (int i = 0; i < NUM_INPUTS; i++) inputsHistory[i] = (inputsHistory[i] << 1) | inputs[i];
    }

    boolean estaSiendoPulsado(Input input) {
        return inputs[input.ordinal()] != 0;
    }

    boolean seHaPulsado(Input input) {
        return (inputsHistory[input.ordinal()] & 0b11) == 0b01;
    }
}
