package com.lavacablasa.ladc.abadia;

class Paleta {

    // paletas gráficas
    private static final int[] INTRO_PALETTE = {
            7, 20, 11, 3, 6, 12, 4, 21, 13, 5, 14, 29, 0, 28, 31, 27
    };

    private static final int[][] GAME_PALETTES = {
            { 20, 20, 20, 20 },        // paleta negra
            { 7, 28, 20, 12 },        // paleta del pergamino
            { 6, 14, 3, 20 },        // paleta de día durante el juego
            { 4, 29, 0, 20 }        // paleta de noche durante el juego
    };

    // campos
    private final CPC6128 cpc6128;        // objeto que presta ayuda para realizar operaciones gráficas del cpc6128

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
