package com.lavacablasa.ladc;

import com.lavacablasa.ladc.core.GameContext;
import com.lavacablasa.ladc.core.LaAbadiaDelCrimen;
import com.lavacablasa.ladc.swing.SwingGameContext;

public class Main {
    public static void main(String[] args) {
        GameContext context = new SwingGameContext();
        LaAbadiaDelCrimen abadia = new LaAbadiaDelCrimen(context);
        abadia.run();
    }
}
