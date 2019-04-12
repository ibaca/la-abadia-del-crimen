package com.lavacablasa.ladc.abadia;

import com.lavacablasa.ladc.core.Input;
import com.lavacablasa.ladc.core.InputPlugin;

class Controles {
    public static final int NUM_INPUTS = Input.values().length;

    private final InputPlugin input;
    private final int[] inputs = new int[NUM_INPUTS];
    private final int[] inputsHistory = new int[NUM_INPUTS];

    Controles(InputPlugin input) {
        this.input = input;
    }

    void actualizaEstado() {
        input.process(inputs);

        // combina el estado actual de los controles con el anterior para poder detectar pulsaciones
        for (int i = 0; i < NUM_INPUTS; i++){
            inputsHistory[i] = (inputsHistory[i] << 1) | inputs[i];
        }
    }

    boolean estaSiendoPulsado(Input input) {
        return inputs[input.ordinal()] != 0;
    }

    boolean seHaPulsado(Input input) {
        return (inputsHistory[input.ordinal()] & 0b11) == 0b01;
    }

    boolean seHaSoltado(Input input) {
        return (inputsHistory[input.ordinal()] & 0b11) == 0b10;
    }
}
