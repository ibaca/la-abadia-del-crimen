package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.Orientacion.ABAJO;
import static com.lavacablasa.ladc.abadia.Orientacion.IZQUIERDA;

class DatosFotograma {
    int dirGfx;             // dirección de los gráficos de este fotograma de la animación
    int ancho;              // ancho de este fotograma de la animación (en múltiplos de 4 pixels)
    int alto;               // alto de este fotograma de la animación (en pixels)

    public DatosFotograma(int dirGfx, int ancho, int alto) {
        this.dirGfx = dirGfx;
        this.ancho = ancho;
        this.alto = alto;
    }
}

abstract class Personaje extends EntidadJuego {

    // tabla para el cálculo del desplazamiento, según la animación y la cámara de un personaje
    private static final int[][][] difPosAnimCam = {
        {
            { 0, 0,  -1, -2,  -1, +2,  -2,  0,  +1, +2,   0,  0,   0, -2 },
            { 0, 0,  -1, +2,  +1, +2,   0, +4,  -1, +2,  -2, +6,  -2,  0 },
            { 0, 0,  +1, +2,  -1, +2,   0, +4,  +1, +2,  +2, +6,  +2,  0 },
            { 0, 0,  +1, -2,  +1, +2,  +2,  0,  -1, +2,   0,  0,   0, -2 }
        },
        {
            { 0, 0,  -1, -2,  -1, +2,  -2,  0,  -1, -2,  -2, -4,  -2, -6 },
            { 0, 0,  -1, +2,  -1, -2,  -2,  0,  -1, +2,  -2, +6,  -2,  0 },
            { 0, 0,  +1, +2,  -1, +2,   0, +4,  -1, -2,   0, +2,   0, -4 },
            { 0, 0,  +1, -2,  -1, -2,   0, -4,  -1, +2,   0,  0,   0, -2 }
        },
        {
            { 0, 0,  -1, -2,  +1, -2,   0, -4,  -1, -2,  -2, -4,  -2, -6 },
            { 0, 0,  -1, +2,  -1, -2,  -2,  0,  +1, -2,   0, +2,   0, -3 },
            { 0, 0,  +1, +2,  +1, -2,  +2,  0,  -1, -2,   0, +2,   0, -4 },
            { 0, 0,  +1, -2,  -1, -2,   0, -4,  +1, -2,  +2, -4,  +2, -6 }
        },
        {
            { 0, 0,  -1, -2,  +1, -2,   0, -4,  +1, +2,   0,  0,   0, -2 },
            { 0, 0,  -1, +2,  +1, +2,   0, +4,  +1, -2,   0, +2,   0, -4 },
            { 0, 0,  +1, +2,  +1, -2,  +2,  0,  +1, +2,  +2, +6,  +2,  0 },
            { 0, 0,  +1, -2,  +1, +2,  +2,  0,  +1, -2,  +2, -4,  +2, -6 }
        }
    };

    private static final int FLIP_OFFSET = 0xc000;

    int estado;                     // estado del personaje
    int contadorAnimacion;          // contador para animar el personaje
    boolean bajando;                // indica si el personaje está bajando en altura
    boolean enDesnivel;             // indica que el personaje está en un desnivel
    boolean giradoEnDesnivel;       // indica que el personaje no está avanzando en el sentido del desnivel
    boolean flipX;                  // indica si los gráficos están girados en x
    int despX;                      // desplazamiento en x para dibujar el personaje (en múltiplos de 4 pixels)
    int despY;                      // desplazamiento en y para dibujar el personaje (en pixels)
    int valorPosicion;              // valor a grabar en las posiciones de la rejilla en las que está el personaje

    boolean puedeQuitarObjetos;     // indica si el personaje puede quitar objetos o a otro personaje
    int objetos;                    // objetos que tiene el personaje
    int mascaraObjetos;             // máscara de los objetos que puede coger el personaje
    int contadorObjetos;            // contador para no coger/dejar los objetos varias veces

    int permisosPuertas;            // puertas que puede abrir el personaje

    DatosFotograma[] animacion;     // tabla con los datos para las animaciones
    int numFotogramas;              // número de fotogramas de la tabla de animaciones


    Personaje(Juego juego, Sprite sprite) {
        super(juego, sprite);

        estado = 0;
        puedeQuitarObjetos = false;
        objetos = 0;
        mascaraObjetos = 0;
        contadorObjetos = 0;

        contadorAnimacion = 0;
        bajando = false;
        giradoEnDesnivel = false;
        enDesnivel = false;
        flipX = false;
        despX = despY = 0;
        valorPosicion = 0x10;

        permisosPuertas = 0;

        animacion = null;
        numFotogramas = 0;
    }

    abstract void run();
    abstract void ejecutaMovimiento();

    /////////////////////////////////////////////////////////////////////////////
    // actualización del entorno cuando un personaje es visible en la pantalla actual
    /////////////////////////////////////////////////////////////////////////////

    void actualizaPosPantSprite(int posXPant, int posYPant, int profundidad)
    {
        int entrada = 0;

        // si el personaje ocupa sólo una posición porque está en un desnivel, hay que tener en cuenta varios casos
        if (enDesnivel){
            entrada += 2;

            if (!giradoEnDesnivel){
                entrada += 2;

                if ((contadorAnimacion & 0x01) == 0x01){
                    entrada += 1;
                    if (bajando){
                        entrada += 1;
                    }
                }
            } else {
                entrada += contadorAnimacion & 0x01;
            }
        } else {
            // si el personaje ocupa cuatro posiciones, la posición solo depende del contador de la animación
            entrada += contadorAnimacion & 0x01;
        }

        // actualiza la posición en pantalla del sprite asociado al personaje dependiendo de la cámara
        int oriAjustada = juego.motor.ajustaOrientacionSegunCamara(orientacion);
        sprite.posXPant = posXPant + despX + difPosAnimCam[juego.motor.oriCamara][oriAjustada][2*entrada];
        sprite.posYPant = posYPant + despY + difPosAnimCam[juego.motor.oriCamara][oriAjustada][2*entrada + 1];

        // si el sprite no es visible, fija también la posición anterior
        if (!sprite.esVisible){
            sprite.oldPosXPant = sprite.posXPant;
            sprite.oldPosYPant = sprite.posYPant;
        }
    }

    // actualiza la posición y la animación del sprite dependiendo de su posición con respecto a la cámara
    void notificaVisibleEnPantalla(int posXPant, int posYPant, int profundidad)
    {
        // actualiza la posición en pantalla del sprite
        actualizaPosPantSprite(posXPant, posYPant, profundidad);

        // actualiza la animación del personaje
        DatosFotograma df = calculaFotograma();
        actualizaAnimacion(df, profundidad);
    }

    /////////////////////////////////////////////////////////////////////////////
    // animación del personaje
    /////////////////////////////////////////////////////////////////////////////

    // calcula el fotograma que hay que poner al personaje
    DatosFotograma calculaFotograma()
    {
        // obtiene la orientación del personaje según la posición de la cámara
        int oriCamara = juego.motor.ajustaOrientacionSegunCamara(orientacion);

        // selecciona un fotograma dependiendo de la orientación y de si el personaje va hacia la derecha o a la izquierda
        int numAnim = (((oriCamara + 1) & 0x02) << 1) | contadorAnimacion;

        assert(numAnim < numFotogramas);

        // devuelve los datos del fotograma de la animación del personaje
        return animacion[numAnim];
    }

    // actualiza la animación del sprite con el fotograma que se le pasa
    void actualizaAnimacion(DatosFotograma df, int profundidad)
    {
        sprite.esVisible = true;
        sprite.haCambiado = true;
        sprite.profundidad = profundidad;

        // obtiene la orientación del personaje según la posición de la cámara
        int oriCamara = juego.motor.ajustaOrientacionSegunCamara(orientacion);

        // comprueba si hay que girar los gráficos del personaje por el cambio de la orientación del personaje
        if ((((oriCamara >> 1) & 0x01) ^ ((flipX) ? 1 : 0)) != 0){
            flipX = !flipX;
        }

        sprite.despGfx = df.dirGfx;
        sprite.ancho = df.ancho;
        sprite.alto = df.alto;

        if (flipX){
            sprite.despGfx += FLIP_OFFSET;
        }

        juego.logica.hayMovimiento = true;
    }

    // avanza la animación del sprite y ajusta según la cámara
    void avanzaAnimacion()
    {
        // avanza el contador de la animación
        contadorAnimacion = (contadorAnimacion + 1) & 0x03;

        // actualiza la posición y dimensiones del sprite del personaje según la animación y la cámara
        actualizaSprite();
    }

    // actualiza la posición y dimensiones del sprite del personaje según la animación y la cámara
    void actualizaSprite()
    {
        // comprueba si el sprite es visible en la pantalla actual
        if (!juego.motor.actualizaCoordCamara(this)) {
            // si el sprite era visible, lo hace desaparecer
            if (sprite.esVisible){
                sprite.desaparece = true;
                sprite.haCambiado = true;
                sprite.profundidad = 0;
            }
        }

        juego.logica.hayMovimiento = true;
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con el movimiento del personaje
    /////////////////////////////////////////////////////////////////////////////

    void mueve()
    {
        // pone la posición y dimensiones actuales como posición y dimensiones antiguas
        sprite.preparaParaCambio();

        // si el personaje está en las posiciones centrales de la pantalla actual, limpia las posiciones que ocupa
        marcaPosicion(juego.motor.rejilla, 0);

        // avanza la animación del personaje o se mueve
        avanzaAnimacionOMueve();

        // si el personaje está en las posiciones centrales de la pantalla actual, marca las posiciones que ocupa
        marcaPosicion(juego.motor.rejilla, valorPosicion);
    }

    // dependiendo de como esté la animación del personaje, avanza la animación o realiza un movimiento
    void avanzaAnimacionOMueve()
    {
        if ((contadorAnimacion & 0x01) == 0x01){
            avanzaAnimacion();
        } else {
            ejecutaMovimiento();
        }
    }

    // si el personaje puede avanzar en la orientación actual, avanza
    void trataDeAvanzar(int difAltura1, int difAltura2, int avanceX, int avanceY)
    {
        bajando = false;

        // si el personaje ocupa 4 posiciones
        if (!enDesnivel){
            // si se quiere subir o bajar, cambia la altura e indica que el personaje está en desnivel
            if (difAltura1 == 1){
                altura++;
                enDesnivel = true;
            } else if (difAltura1 == -1){
                altura--;
                enDesnivel = true;
                bajando = true;
            } else if (difAltura1 != 0){
                // si se quiere pasar a un desnivel de más de una posición, sale
                return;
            }

            // si se anda por una zona sin desnivel, actualiza la posición y la animación del personaje según hacia donde se avanza
            if (difAltura1 == 0){
                incrementaPos(avanceX, avanceY);
                avanzaAnimacion();
            } else {
                // si se va a subir o a bajar
                incrementaPos(avanceX, avanceY);

                if (cambioCentro()){
                    incrementaPos(avanceX, avanceY);
                }
                avanzaAnimacion();
            }
        } else {
            // si el personaje ocupa 1 posición

            // si se quiere avanzar a una posición donde hay un personaje, sale
            if ((difAltura2 == 0x10) || (difAltura2 == 0x20)) return;

            if (!giradoEnDesnivel){
                // si se quiere subir o bajar, cambia la altura
                if (difAltura1 == 1){
                    altura++;
                } else if (difAltura1 == -1){
                    altura--;
                    bajando = true;
                } else {
                    // si el desnivel es muy grande, sale
                    return;
                }

                // si las 2 posiciones que hay avanzando tienen la misma altura, indica que ya no está en desnivel
                if (difAltura1 == difAltura2){
                    enDesnivel = false;

                    incrementaPos(avanceX, avanceY);

                    if (!cambioCentro()){
                        incrementaPos(avanceX, avanceY);
                    }
                    avanzaAnimacion();
                } else {
                    incrementaPos(avanceX, avanceY);
                    avanzaAnimacion();
                }
            } else {
                // si el personaje está girado en un desnivel
                int difAltura = cambioCentro() ? difAltura2 : difAltura1;

                if (difAltura != 0) return;

                incrementaPos(avanceX, avanceY);
                avanzaAnimacion();
            }
        }
    }

    // actualiza la posición del personaje según el avance que se le pasa
    void incrementaPos(int avanceX, int avanceY)
    {
        posX += avanceX;
        posY += avanceY;
    }

    // gira el personaje a la derecha o a la izquierda
    void gira(int difOrientacion)
    {
        contadorAnimacion = 0;

        // si está en desnivel, obtiene si avanza en el sentido del desnivel o no
        if (enDesnivel){
            giradoEnDesnivel = !giradoEnDesnivel;
        }

        // actualiza la orientación del personaje
        orientacion = (orientacion + difOrientacion) & 0x03;

        // actualiza la posición y dimensiones del sprite del personaje según la animación y la cámara
        actualizaSprite();
    }

    // si la orientación del personaje es DERECHA o ARRIBA devuelve false, en otro caso devuelve true
    boolean cambioCentro()
    {
        return (orientacion == ABAJO) || (orientacion == IZQUIERDA);
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con los objetos
    /////////////////////////////////////////////////////////////////////////////

    // indica si se puede dejar un objeto. Si no se puede, devuelve -1. En otro caso devuelve el número de objeto que puede dejar
    int puedeDejarObjeto(int[] posicion)
    {
        // inicia la máscara para el primer objeto
        int mascara = 1 << Juego.numObjetos;

        // si el personaje está cogiendo o dejando algún objeto, sale
        contadorObjetos--;
        if (contadorObjetos != -1) return -1;
        contadorObjetos++;

        // recorre los objetos del juego
        for (int i = 0; i < Juego.numObjetos; i++){
            mascara = mascara >> 1;

            // si no se tiene el objeto que actual, pasa al siguiente
            if ((objetos & mascara) == 0) continue;

            // obtiene la posición en la que se dejará el objeto
            posicion[0] = posX + 2* MotorGrafico.tablaDespOri[orientacion][0];
            posicion[1] = posY + 2* MotorGrafico.tablaDespOri[orientacion][1];
            posicion[2] = altura;
            int alturaBasePlantaObj = juego.motor.obtenerAlturaBasePlanta(altura);
            int alturaRelativa = altura - alturaBasePlantaObj;

            boolean estaEnPantallaActual = false;

            // si el objeto está en la misma planta que la que se muestra en pantalla
            if (alturaBasePlantaObj == juego.motor.rejilla.minAltura){
                RejillaPantalla rejilla = juego.motor.rejilla;

                int[] posObjRejilla = new int[2];

                // comprueba si la posición en la que se deja el objeto está en la rejilla de pantalla que se muestra
                if (rejilla.ajustaAPosRejilla(posicion[0], posicion[1], posObjRejilla)){
                    int altPos = rejilla.bufAlturas[posObjRejilla[1]][posObjRejilla[0]] & 0xff;

                    // si hay algún personaje en la posición en la que se quiere dejar el objeto, sale
                    if ((altPos & 0xf0) != 0) return -1;

                    // se queda con la altura de esa posición
                    altPos = altPos & 0x0f;

                    // si se va a dejar a una altura muy alta de la planta, sale
                    if (altPos >= 0x0d) return -1;

                    // si hay mucha diferencia de altura del personaje a donde se va dejar, sale
                    if ((altPos - alturaRelativa) >= 5) return -1;

                    // si la altura de la posición en donde se va a dejar el objeto es distinta de la de sus vecinos, sale
                    if (altPos != (rejilla.bufAlturas[posObjRejilla[1]][posObjRejilla[0] - 1] & 0xff)) return -1;
                    if (altPos != (rejilla.bufAlturas[posObjRejilla[1] - 1][posObjRejilla[0]] & 0xff)) return -1;
                    if (altPos != (rejilla.bufAlturas[posObjRejilla[1] - 1][posObjRejilla[0] - 1] & 0xff)) return -1;

                    // calcula la altura final del objeto
                    posicion[2] = rejilla.minAltura + altPos;

                    estaEnPantallaActual = true;
                }
            }

            // si el objeto no se va a dejar en la pantalla que se muestra actualmente, se deja en la posición del personaje
            if (!estaEnPantallaActual){
                posicion[0] = posX;
                posicion[1] = posY;
                posicion[2] = altura;
            }

            // devuelve el objeto que se puede dejar
            return i;
        }

        // si llega aquí es que no se pudo dejar ningún objeto
        return -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    // posición en el buffer de alturas
    /////////////////////////////////////////////////////////////////////////////

    // marca la posición ocupada por el personaje en el buffer de alturas
    void marcaPosicion(RejillaPantalla rejilla, int valor)
    {
        int[] posRejilla = new int[2];

        // si el personaje está en las 20x20 posiciones centrales de la rejilla, marca las posiciones que ocupa
        if (rejilla.estaEnRejillaCentral(this, posRejilla)){
            // marca la posición (x, y) en el buffer de alturas
            rejilla.bufAlturas[posRejilla[1]][posRejilla[0]] = (byte) ((rejilla.bufAlturas[posRejilla[1]][posRejilla[0]] & 0x0f) | valor);

            // si el personaje no está en un desnivel, ocupa 4 posiciones ((x, y)(x-1, y)(x, y-1)(x-1, y-1))
            if (!enDesnivel){
                rejilla.bufAlturas[posRejilla[1]][posRejilla[0] - 1] = (byte) ((rejilla.bufAlturas[posRejilla[1]][posRejilla[0] - 1] & 0x0f) | valor);
                rejilla.bufAlturas[posRejilla[1] - 1][posRejilla[0]] = (byte) ((rejilla.bufAlturas[posRejilla[1] - 1][posRejilla[0]] & 0x0f) | valor);
                rejilla.bufAlturas[posRejilla[1] - 1][posRejilla[0] - 1] = (byte) ((rejilla.bufAlturas[posRejilla[1] - 1][posRejilla[0] - 1] & 0x0f) | valor);
            }
        }
    }

}
