package com.lavacablasa.ladc.abadia;

class Paleta {

    // paletas gráficas
    private static final int[] INTRO_PALETTE = {
            07, 20, 11, 03, 06, 12, 04, 21, 13, 05, 14, 29, 00, 28, 31, 27
    };

    private static final int[][] GAME_PALETTES = {
            { 20, 20, 20, 20 },		// paleta negra
            { 07, 28, 20, 12 },		// paleta del pergamino
            { 06, 14, 03, 20 },		// paleta de día durante el juego
            { 04, 29, 00, 20 }		// paleta de noche durante el juego
    };

    // campos
    private final CPC6128 cpc6128;		// objeto que presta ayuda para realizar operaciones gráficas del cpc6128

    Paleta(Juego juego) {
        this.cpc6128 = juego.cpc6128;
    }

    // métodos

    void setIntroPalette() {
        setPalette(INTRO_PALETTE);
    }

    void setGamePalette(int pal) {
        setPalette(GAME_PALETTES[pal]);
    }

    private void setPalette(int[] palette) {
        for (int i = 0; i < palette.length; i++) {
            cpc6128.setHardwareColor(i, palette[i]);
        }
    }
}
