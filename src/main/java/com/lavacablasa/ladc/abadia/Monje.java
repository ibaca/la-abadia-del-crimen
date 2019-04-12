package com.lavacablasa.ladc.abadia;

abstract class Monje extends PersonajeConIA {

    // tabla con los distintos fotogramas de animación
    static DatosFotograma[] tablaAnimacion = {
            new DatosFotograma(0x0000, 0x05, 0x22),
            new DatosFotograma(0x0000, 0x05, 0x24),
            new DatosFotograma(0x0000, 0x05, 0x22),
            new DatosFotograma(0x0000, 0x05, 0x22),
            new DatosFotograma(0x0000, 0x05, 0x21),
            new DatosFotograma(0x0000, 0x05, 0x23),
            new DatosFotograma(0x0000, 0x05, 0x21),
            new DatosFotograma(0x0000, 0x05, 0x21)
    };

    int[] datosCara = new int[2];				// dirección de los gráficos de la cara
    SpriteMonje sprMonje;						// sprite del monje

    Monje(Juego juego, SpriteMonje sprite) {
        super(juego, sprite);

        // guarda una referencia al sprite del monje
        sprMonje = sprite;

        // asigna la tabla de animación del personaje
        animacion = tablaAnimacion;
        numFotogramas = 8;

        // inicialmente no tiene datos de cara
        datosCara[0] = datosCara[1] = 0x0000;
    }

    // calcula el fotograma que hay que poner al monje
    DatosFotograma calculaFotograma()
    {
        // obtiene la orientación del personaje según la posición de la cámara
        int oriCamara = juego.motor.ajustaOrientacionSegunCamara(orientacion);

        // actualiza la animación del traje
        sprMonje.animacionTraje = (oriCamara << 2) | contadorAnimacion;

        // selecciona un fotograma dependiendo de la orientación y de si el personaje va hacia la derecha o a la izquierda
        int numAnim = (((oriCamara + 1) & 0x02) << 1) | contadorAnimacion;

        assert(numAnim < numFotogramas);

        // modifica los datos del fotograma con la dirección de la cara del personaje
        animacion[numAnim].dirGfx = datosCara[(numAnim & 0x04) != 0 ? 1 : 0];

        // devuelve los datos del fotograma de la animación del personaje
        return animacion[numAnim];
    }

}
