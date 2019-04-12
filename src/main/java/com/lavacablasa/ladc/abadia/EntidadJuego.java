package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;

class Orientacion {
    public static final int DERECHA     = 0;            // hacia +x
    public static final int ABAJO       = 1;            // hacia -y
    public static final int IZQUIERDA   = 2;            // hacia -x
    public static final int ARRIBA      = 3;            // hacia +y
}

class PosicionJuego {
    public int orientacion;         // orientación de la posición en el mundo
    public int posX;                // posición x en coordenadas de mundo
    public int posY;                // posición y en coordenadas de mundo
    public int altura;              // altura en coordenadas de mundo

    PosicionJuego() {
        this(DERECHA, 0, 0, 0);
    }

    PosicionJuego(int orientacion, int posX, int posY, int altura) {
        this.orientacion = orientacion;
        this.posX = posX;
        this.posY = posY;
        this.altura = altura;
    }

    void copy(PosicionJuego other) {
        this.orientacion = other.orientacion;
        this.posX = other.posX;
        this.posY = other.posY;
        this.altura = other.altura;
    }
}

abstract class EntidadJuego extends PosicionJuego {
    Juego juego;
    Sprite sprite;			// sprite asociado a la entidad

    EntidadJuego(Juego juego, Sprite sprite) {
        this.juego = juego;
        this.sprite = sprite;
    }

    abstract void notificaVisibleEnPantalla(int posXPant, int posYPant, int profundidad);
}
