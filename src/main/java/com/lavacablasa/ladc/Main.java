package com.lavacablasa.ladc;

import com.lavacablasa.ladc.core.LaAbadiaDelCrimen;
import com.lavacablasa.ladc.swing.SwingGameContext;

public class Main {
    public static void main(String[] args) {
        new LaAbadiaDelCrimen(new SwingGameContext()).run();
    }
}
