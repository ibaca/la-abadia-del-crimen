package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.ObjetosJuego.LAMPARA;

class MotorGrafico {
    static final int[][] tablaDespOri = {
            { +1, 0 },
            { 0, -1 },
            { -1, 0 },
            { 0, +1 }
    };

    private static final int SCREEN_DATA_BASE_ADDRESS = 0x1c000;

    /////////////////////////////////////////////////////////////////////////////
    // mapa de las plantas de la abadía
    /////////////////////////////////////////////////////////////////////////////

    private static byte[][] plantas = { //@formatter:off
        {
    // planta baja
    //
    // X	00   01   02   03   04   05   06   07   08   09   0a   0b   0c   0d   0e  0f        Y
    //		===============================================================================     ==
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 00
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0x27,0   ,0x3e,0   ,0   ,0   ,0   ,0   , // 01
            0   ,0x0a,0x09,0   ,0x07,0x08,0x2a,0x28,0x26,0x29,0x37,0x38,0x39,0   ,0   ,0   , // 02
            0   ,0   ,0x02,0x01,0x00,0x0d,0x0e,0x24,0x23,0x25,0x2b,0x2c,0x2d,0   ,0   ,0   , // 03
            0   ,0   ,0x03,0   ,0x1f,0   ,0   ,0   ,0x22,0   ,0x2e,0x2f,0x30,0   ,0   ,0   , // 04
            0   ,0   ,0x04,0x1d,0x1e,0x3e,0x3d,0   ,0x21,0   ,0x31,0x32,0x33,0   ,0   ,0   , // 05
            0   ,0x0c,0x0b,0x1c,0x05,0x06,0x3c,0   ,0x20,0   ,0x34,0x35,0x36,0   ,0   ,0   , // 06
            0   ,0   ,0   ,0x0f,0x10,0x11,0x12,0   ,0x1b,0   ,0x1a,0x3a,0x3b,0   ,0   ,0   , // 07
            0   ,0   ,0   ,0   ,0   ,0   ,0x13,0x14,0x15,0x18,0x19,0   ,0   ,0   ,0   ,0   , // 08
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0x16,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 09
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0x17,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0a
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0b
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0c
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0d
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0e
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0     // 0f
        },
        {
    // primera planta
    //
    // X	00   01   02   03   04   05   06   07   08   09   0a   0b   0c   0d   0e  0f        Y
    //		===============================================================================     ==
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 00
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 01
            0   ,0x45,0x44,0   ,0x48,0x49,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 02
            0   ,0   ,0x43,0x47,0x4a,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 03
            0   ,0   ,0x42,0   ,0x4b,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 04
            0   ,0   ,0x41,0x40,0x4c,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 05
            0   ,0x3f,0x46,0   ,0x4d,0x4e,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 06
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 07
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 08
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 09
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0a
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0b
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0c
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0d
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0e
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0     // 0f
        },
        {
    // segunda planta
    //
    // X	00   01   02   03   04   05   06   07   08   09   0a   0b   0c   0d   0e  0f        Y
    //		===============================================================================     ==
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 00
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 01
            0   ,0x67,0x66,0   ,0x65,0x64,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 02
            0   ,0   ,0x6a,0x69,0x68,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 03
            0   ,0   ,0x6c,0   ,0x6b,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 04
            0   ,0   ,0x6f,0x6e,0x6d,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 05
            0   ,0x73,0x72,0   ,0x71,0x70,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 06
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 07
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 08
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 09
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0a
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0b
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0c
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0d
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   , // 0e
            0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0   ,0     // 0f
        }
    }; //@formatter:on

    Juego juego;
    RejillaPantalla rejilla;        // objeto para realizar operaciones relacionadas con la rejilla de pantalla
    GeneradorPantallas genPant;     // generador de pantallas interpretando los bloques de construcción
    MezcladorSprites mezclador;     // mezclador de los sprites con el resto de la pantalla

    int posXPantalla;               // posición x de la pantalla actual en coordenadas de mundo
    int posYPantalla;               // posición y de la pantalla actual en coordenadas de mundo
    int alturaBasePantalla;         // altura base de la planta que se muestra en la pantalla actual
    boolean pantallaIluminada;      // indica si la pantalla está iluminada o no
    boolean hayQueRedibujar;        // indica que hay que redibujar la pantalla
    int numPantalla;                // número de la pantalla que muestra la cámara
    int oriCamara;                  // orientación de la cámara para ver la pantalla actual

    Personaje personaje;            // personaje al que sigue la cámara

    public MotorGrafico(Juego juego, byte[] buffer) {
        this.juego = juego;

        posXPantalla = posYPantalla = 0;
        alturaBasePantalla = 0;
        hayQueRedibujar = true;
        pantallaIluminada = true;

        personaje = null;

        genPant = new GeneradorPantallas(juego);
        rejilla = new RejillaPantalla(juego);
        mezclador = new MezcladorSprites(juego, buffer);
    }

    /////////////////////////////////////////////////////////////////////////////
    // comprobación del cambio de pantalla y actualización de entidades del juego
    /////////////////////////////////////////////////////////////////////////////

    // comprueba si el personaje al que sigue la cámara ha cambiado de pantalla y si es así, actualiza las variables del motor
    // obtiene los datos de altura de la nueva pantalla y ajusta las posiciones de las entidades del juego
    // según la orientación con la que se ve la pantalla actual
    void compruebaCambioPantalla() {
        // inicialmente no hay cambio de pantalla
        boolean cambioPantalla = false;

        // si el personaje al que sigue la cámara cambia de pantalla en x
        if ((personaje.posX & 0xf0) != posXPantalla) {
            cambioPantalla = true;

            // actualiza la posición x del motor
            posXPantalla = personaje.posX & 0xf0;
        }

        // si el personaje al que sigue la cámara cambia de pantalla en y
        if ((personaje.posY & 0xf0) != posYPantalla) {
            cambioPantalla = true;

            // actualiza la posición y del motor
            posYPantalla = personaje.posY & 0xf0;
        }

        // si el personaje ha cambiado de planta
        if (obtenerAlturaBasePlanta(personaje.altura) != alturaBasePantalla) {
            cambioPantalla = true;

            // actualiza la altura del motor
            alturaBasePantalla = obtenerAlturaBasePlanta(personaje.altura);
        }

        // si no se ha cambiado la pantalla que se muestra, sale
        if (!cambioPantalla) return;

        hayQueRedibujar = true;
        pantallaIluminada = true;

        // si está en la segunda planta, comprueba si es una de las pantallas iluminadas
        if (obtenerPlanta(alturaBasePantalla) == 2) {
            // si no está detrás del espejo o en la habitación iluminada del laberinto
            if (posXPantalla >= 0x20) {
                if (posXPantalla != 0x20) {
                    pantallaIluminada = false;
                } else {
                    // si está en la pantalla del espejo
                    pantallaIluminada = posYPantalla == 0x60;
                }
            }
        }

        // marca el sprite de la luz como no visible
        juego.sprites[Juego.spriteLuz].esVisible = false;

        // obtiene el número de pantalla que se va a mostrar
        numPantalla = plantas[obtenerPlanta(alturaBasePantalla)][posYPantalla | ((posXPantalla >> 4) & 0x0f)];

        // rellena el buffer de alturas con los datos de altura de la pantalla actual
        rejilla.rellenaAlturasPantalla(personaje);

        // calcula la orientación de la cámara para la pantalla que se va a mostrar
        oriCamara = (((posXPantalla >> 4) & 0x01) << 1) | (((posXPantalla >> 4) & 0x01) ^ ((posYPantalla >> 4) & 0x01));

        // recorre las puertas, y para las visibles, actualiza su posición y marca la altura que ocupan
        actualizaPuertas();

        // recorre los objetos, y para los visibles, actualiza su posición
        actualizaObjetos();

        // recorre los personajes, y para los visibles, actualiza su posición y animación y marca la altura que ocupan
        actualizaPersonajes();
    }

    /////////////////////////////////////////////////////////////////////////////
    // actualización de las entidades del juego según la cámara
    /////////////////////////////////////////////////////////////////////////////

    void actualizaPuertas() {
        // recorre las puertas, y para las visibles, actualiza su posición y marca la altura que ocupan
        for (int i = 0; i < Juego.numPuertas; i++) {
            Puerta puerta = juego.puertas[i];

            // actualiza la posición del sprite según la cámara
            if (!actualizaCoordCamara(puerta)) {
                puerta.sprite.esVisible = false;
            }

            puerta.sprite.oldPosXPant = puerta.sprite.posXPant;
            puerta.sprite.oldPosYPant = puerta.sprite.posYPant;
        }
    }

    void actualizaObjetos() {
        // recorre los objetos, y para los visibles, actualiza su posición
        for (int i = 0; i < Juego.numObjetos; i++) {
            Objeto objeto = juego.objetos[i];

            // actualiza la posición del sprite según la cámara
            if (!actualizaCoordCamara(objeto)) {
                objeto.sprite.esVisible = false;
            }

            objeto.sprite.oldPosXPant = objeto.sprite.posXPant;
            objeto.sprite.oldPosYPant = objeto.sprite.posYPant;
        }
    }

    void actualizaPersonajes() {
        // recorre los personajes, y para los visibles, actualiza su posición y animación y marca la altura que ocupan
        for (int i = 0; i < Juego.numPersonajes; i++) {
            Personaje pers = juego.personajes[i];

            // actualiza la posición del sprite según la cámara
            if (!actualizaCoordCamara(pers)) {
                pers.sprite.esVisible = false;
            }

            // si el personaje está en las posiciones centrales de la pantalla actual, marca las posiciones que ocupa
            pers.marcaPosicion(rejilla, pers.valorPosicion);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // dibujo de la escena
    /////////////////////////////////////////////////////////////////////////////

    void dibujaSprites() {
        // si la habitación no está iluminada, evita dibujar los sprites visibles
        if (!pantallaIluminada) {
            for (int i = 0; i < Juego.numSprites; i++) {
                if (juego.sprites[i].esVisible) {
                    juego.sprites[i].haCambiado = false;
                }
            }

            // si adso es visible en la pantalla actual y tiene la lámpara, activa el sprite de la luz
            if ((juego.personajes[1].sprite.esVisible) && ((juego.personajes[1].objetos & LAMPARA) != 0)) {
                juego.sprites[Juego.spriteLuz].esVisible = true;
                juego.sprites[Juego.spriteLuz].haCambiado = true;

                // fija una profundidad muy alta para que sea el último sprite que se dibuje
                juego.sprites[Juego.spriteLuz].profundidad = 0x3c;
            }
        }

        // dibuja los sprites visibles que han cambiado
        mezclador.mezclaSprites(juego.sprites, Juego.numSprites);
    }

    void dibujaPantalla() {
        if (hayQueRedibujar) {
            // elige un color de fondo según el tipo de pantalla
            int colorFondo = (pantallaIluminada) ? 0 : 3;

            // prepara el buffer de tiles y limpia la pantalla
            genPant.limpiaPantalla(colorFondo);

            // obtiene el desplazamiento de los datos a los bloques que forman la pantalla actual
            int data = obtenerDirPantalla(numPantalla);

            // rellena el buffer de tiles interpretando los bloques que forman la pantalla
            genPant.genera(data);

            // si es una pantalla iluminada, dibuja el contenido del buffer de tiles
            if (pantallaIluminada) {
                genPant.dibujaBufferTiles();
            }

            hayQueRedibujar = false;
        }
    }

    boolean actualizaCoordCamara(EntidadJuego entidad) {

        int posXPant, posYPant, sprPosY;

        // transforma las coordenadas de mundo en coordenadas locales
        int posXLocal = entidad.posX - (posXPantalla - 12);
        int posYLocal = entidad.posY - (posYPantalla - 12);
        int alturaLocal = entidad.altura - alturaBasePantalla;

        // si la entidad no está en la zona visible, devuelve false
        if ((posXLocal < 0) || (posXLocal >= 40)) return false;
        if ((posYLocal < 0) || (posYLocal >= 40)) return false;
        if (obtenerAlturaBasePlanta(entidad.altura) != alturaBasePantalla) return false;

        // transforma las coordenadas locales a coordenadas de cámara
        int[] posLocal = new int[] { posXLocal, posYLocal };
        transCoordLocalesACoordCamara(posLocal);
        posXLocal = posLocal[0];
        posYLocal = posLocal[1];

        entidad.sprite.posXLocal = posXLocal;
        entidad.sprite.posYLocal = posYLocal;

        // convierte las coordenadas de cámara en coordenadas de pantalla
        posYPant = posXLocal + posYLocal - alturaLocal;

        if (posYPant < 0) return false;
        posYPant = posYPant - 6;

        if ((posYPant < 8) || (posYPant >= 58)) return false;

        posYPant = 4 * (posYPant + 1);
        posXPant = 2 * (posXLocal - posYLocal) + 80 - 40;
        if ((posXPant < 0) || (posXPant >= 80)) return false;

        // calcula la profundidad usada para ordenar el dibujado de sprites
        sprPosY = posXLocal + posYLocal - 16;

        if (sprPosY < 0) {
            sprPosY = 0;
        }

        entidad.notificaVisibleEnPantalla(posXPant, posYPant, sprPosY);
        return true;
    }

    // transforma las coordenadas locales según la orientación de la cámara
    void transCoordLocalesACoordCamara(int[] pos) {
        int temp;
        switch (oriCamara) {
            case 0:
                break;
            case 1:
                temp = pos[1];
                pos[1] = pos[0];
                pos[0] = 40 - temp;
                break;
            case 2:
                pos[0] = 40 - pos[0];
                pos[1] = 40 - pos[1];
                break;
            case 3:
                temp = pos[0];
                pos[0] = pos[1];
                pos[1] = 40 - temp;
                break;
        }
    }

    // ajusta la orientación que se le pasa según la orientación de la cámara
    int ajustaOrientacionSegunCamara(int orientacion) {
        return (orientacion - oriCamara) & 0x03;
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con al altura
    /////////////////////////////////////////////////////////////////////////////

    // devuelve la altura base de la planta a la que corresponde la altura que se le pasa
    int obtenerAlturaBasePlanta(int altura) {
        if (altura < 0x0d) return 0x00;            // planta baja (0x00-0x0c)
        if (altura >= 0x18) return 0x16;        // segunda baja (0x18-0xff)
        return 0x0b;                            // primera planta (0x0d-0x17)
    }

    // devuelve la planta a la que corresponde la altura base que se le pasa
    int obtenerPlanta(int alturaBase) {
        if (alturaBase == 0x00) return 0;        // planta baja
        if (alturaBase == 0x0b) return 1;        // primera planta
        if (alturaBase == 0x16) return 2;        // segunda planta

        assert (false);

        return 0;
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda
    /////////////////////////////////////////////////////////////////////////////

    // dada una pantalla, obtiene la dirección donde empieza
    int obtenerDirPantalla(int numPant) {
        int desp = SCREEN_DATA_BASE_ADDRESS;

        // recorre las pantallas hasta llegar a la que buscamos
        for (int i = 0; i < numPant; i++) {
            desp += juego.gameData(desp);
        }

        return desp + 1;
    }

}
