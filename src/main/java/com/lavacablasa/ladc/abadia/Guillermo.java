package com.lavacablasa.ladc.abadia;

import com.lavacablasa.ladc.core.Input;

class Guillermo extends Personaje {

    /////////////////////////////////////////////////////////////////////////////
    // tabla de la animación del personaje
    /////////////////////////////////////////////////////////////////////////////

    private static final DatosFotograma[] tablaAnimacion = {
        new DatosFotograma(0xa3b4, 0x05, 0x22 ),
        new DatosFotograma(0xa300, 0x05, 0x24 ),
        new DatosFotograma(0xa3b4, 0x05, 0x22 ),
        new DatosFotograma(0xa45e, 0x05, 0x22 ),
        new DatosFotograma(0xa666, 0x04, 0x21 ),
        new DatosFotograma(0xa508, 0x05, 0x23 ),
        new DatosFotograma(0xa666, 0x04, 0x21 ),
        new DatosFotograma(0xa5b7, 0x05, 0x21 )
    };

    int incrPosY; // incremento de la posición y si el estado no es 0

    Guillermo(Juego juego, Sprite sprite) {
        super(juego,sprite);

        // asigna la tabla de animación del personaje
        animacion = tablaAnimacion;
        numFotogramas = 8;

        incrPosY = 2;
    }

    /////////////////////////////////////////////////////////////////////////////
    // movimiento
    /////////////////////////////////////////////////////////////////////////////

    // método llamado desde el bucle principal para que el personaje interactue con el mundo virtual
    void run()
    {
        mueve();
    }

    // mueve el personaje según el estado en el que se encuentra
    void ejecutaMovimiento()
    {
        // si está vivo, responde a la pulsación de los cursores
        if (estado == 0){
            // si la cámara no sigue a guillermo, sale
            if (juego.logica.numPersonajeCamara != 0) return;

            // dependiendo de la tecla que se pulse, actúa en consecuencia
            if (juego.controles.estaSiendoPulsado(Input.LEFT)){
                gira(1);
            } else if (juego.controles.estaSiendoPulsado(Input.RIGHT)){
                gira(-1);
            } else if (juego.controles.estaSiendoPulsado(Input.UP)){
                int[] difAltura = new int[2];
                int[] avance = new int[2];

                // obtiene la altura de las posiciones hacia las que se va a mover
                juego.motor.rejilla.obtenerAlturaPosicionesAvance(this, difAltura, avance);
                trataDeAvanzar(difAltura[0], difAltura[1], avance[0], avance[1]);
            }
        } else {
            // si ha llegado al último estado cuando está muerto, sale
            if (estado == 1) return;

            estado = estado - 1;

            // si ha caido en la trampa del espejo, lo mete en el agujero
            if (estado == 0x13){
                if (incrPosY == 2){
                    posX = posX - 1;
                    actualizaSprite();
                    return;
                }
            }

            if (estado != 1){
                // modifica la posición y del sprite en pantalla
                sprite.posYPant += incrPosY;
                sprite.haCambiado = true;

                juego.logica.hayMovimiento = true;
            } else {
                // en el estado 1 desaparece el sprite de guillermo
                sprite.esVisible = false;
            }
        }
    }
}
