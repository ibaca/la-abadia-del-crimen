package com.lavacablasa.ladc.abadia;

class PosicionesPredefinidas {
    public static final int POS_GUILLERMO = -1;
    public static final int POS_ABAD = -2;
    public static final int POS_LIBRO = -3;
    public static final int POS_PERGAMINO = -4;
}

abstract class PersonajeConIA extends Personaje {

    private static final int[] bufferDatosPersonaje = new int[10];

    /////////////////////////////////////////////////////////////////////////////
    // tabla con los comandos para cambiar de orientación y avanzar
    /////////////////////////////////////////////////////////////////////////////

    private static final int[][] comandosGirar = {
            { 0x8000, 1 },    // avanzar una posición hacia delante (no debería llamarse nunca)
            { 0x4000, 3 },    // girar a la derecha
            { 0x6c00, 6 },    // girar 2 veces a la izquierda
            { 0x6000, 3 }    // girar a la izquierda
    };

    private static final int[][] comandosAvanzar = {
            { 0x8000, 2 },
            // si está girado, avanza, en otro caso sube (y sigue ocupando una posición) (sólo usado si el personaje ocupa una posición)
            { 0x2000, 4 },    // sube (y si ocupaba una posición pasa a ocupar 4 posiciones)
            { 0xc000, 2 },    // baja (y sigue ocupando una posición)
            { 0x3000, 4 },    // baja (y si ocupaba una posición pasa a ocupar 4 posiciones)
            { 0x8000, 1 }    // avanza (sólo usado si el personaje ocupa 4 posiciones)
    };

    /////////////////////////////////////////////////////////////////////////////
    // tabla con las distancias permisibles según la orientación
    /////////////////////////////////////////////////////////////////////////////

    private static final int[][] distanciasOri = {
            { 6, 24, 6, 12 },
            { 6, 12, 12, 24 },
            { 12, 24, 6, 12 },
            { 6, 12, 6, 24 }
    };

    int numBitAcciones;                 // indica el número de bits procesados del byte de acciones actual
    boolean pensarNuevoMovimiento;      // indica si hay que pensar un nuevo movimiento
    int accionActual;                   // indica las acciones que el personaje está procesando actualmente
    int posAcciones;                    // posición de la acción actual en el buffer de acciones del personaje
    byte[] bufAcciones = new byte[0x30];  // buffer de acciones del personaje actual

    int mascarasPuertasBusqueda;        // máscara de las puertas que se comprobarán en la búsqueda
    int aDondeVa;                       // indica al lugar que trata de ir actualmente
    int aDondeHaLlegado;                // indica a donde ha llegado el personaje

    PosicionJuego[] posiciones;         // posiciones a las que puede ir el personaje

    PersonajeConIA(Juego juego, Sprite sprite) {
        super(juego, sprite);
        numBitAcciones = 0;
        pensarNuevoMovimiento = false;
        accionActual = 0;
        posAcciones = 0;

        mascarasPuertasBusqueda = 0;
        aDondeVa = 0;
        estado = 0;
        aDondeHaLlegado = -6;
    }

    abstract void piensa();

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con el comportamiento del personaje
    /////////////////////////////////////////////////////////////////////////////

    // método llamado desde el bucle principal para que el personaje interactue con el mundo virtual
    void run() {
        // inicialmente el personaje va a tratar de moverse
        juego.logica.buscRutas.seBuscaRuta = true;

        // ejecuta la lógica del personaje
        piensa();

        // modifica la tabla de conexiones de las habitaciones dependiendo de las puertas a las que se tenga acceso
        juego.logica.buscRutas.modificaPuertasRuta(mascarasPuertasBusqueda);

        // mueve el personaje
        mueve();

        // si se han terminado los comandos de movimiento, genera los comandos para ir a donde se quiere
        juego.logica.buscRutas.generaAccionesMovimiento(this);
    }

    // interpreta los comandos almacenados en el buffer de acciones para moverse a donde quiere
    void ejecutaMovimiento() {
        // si tiene que pensar a donde moverse, sale
        if (pensarNuevoMovimiento) {
            return;
        }

        // graba algunos datos del personaje por si hay que restaurarlos
        grabaEstado();

        // lee el siguiente comando almacenado en el buffer de acciones
        int comando = leeComando();

        // si hay que pensar un nuevo movimiento sale
        if (comando == -1) return;

        // si hay que girar, gira y sale
        if (comando == 3) {
            gira(1);
            return;
        } else if (comando == 2) {
            gira(-1);
            return;
        }

        // indica que de momento no hay movimiento
        juego.logica.hayMovimiento = false;

        int[] difAltura = new int[2];
        int[] avance = new int[2];

        // en otro caso hay que avanzar. Como sólo tenemos calculada la rejilla de la pantalla que muestra el
        // motor, si el personaje que quiere avanzar no está en la pantalla que se muestra, hacemos trampa
        // e indicamos que el movimiento va a poder realizarse con éxito. Si el personaje está en la pantalla
        // que se está mostrando actualmente, el movimiento se realiza de forma precisa y sin hacer trampas

        // obtiene la altura de las posiciones hacia las que se va a mover
        if (!juego.motor.rejilla.obtenerAlturaPosicionesAvance(this, difAltura, avance)) {
            // si llega aquí es porque el personaje no está en la pantalla que muestra la cámara

            // si el personaje ocupa 4 posiciones
            if (!enDesnivel) {
                difAltura[1] = 0;    // no se utilizará en trataDeAvanzar

                if (comando == 1) {
                    difAltura[0] = 0;
                } else {
                    if (comando == 5) {
                        difAltura[0] = -1;
                    } else { // comando == 4
                        difAltura[0] = 1;
                    }
                }
            } else {
                // si el personaje ocupa una posición
                if (comando == 0) {
                    if (giradoEnDesnivel) {
                        difAltura[0] = 0;
                        difAltura[1] = 0;
                    } else {
                        difAltura[0] = 1;
                        difAltura[1] = 2;
                    }
                } else {
                    if (comando == 1) {
                        difAltura[0] = -1;
                        difAltura[1] = -2;
                    } else if (comando == 4) {
                        difAltura[0] = 1;
                        difAltura[1] = 1;
                    } else { // comando == 5
                        difAltura[0] = -1;
                        difAltura[1] = -1;
                    }
                }
            }

            // guarda el avance en cada coordenada según la orientación en la que se quiere avanzar
            avance[0] = MotorGrafico.tablaDespOri[orientacion][0];
            avance[1] = MotorGrafico.tablaDespOri[orientacion][1];
        }
        trataDeAvanzar(difAltura[0], difAltura[1], avance[0], avance[1]);

        // si el personaje no ha podido moverse a donde quería, deshace los cambios
        if (!juego.logica.hayMovimiento) {
            cargaEstado();
        }
    }

    // devuelve el siguiente bit de los comandos almacenados en el buffer
    int leeBit() {
        // si se han terminado los bits de la accion actual, obtiene un nuevo byte de comandos
        if (numBitAcciones == 0) {
            assert (posAcciones < 0x30);

            accionActual = bufAcciones[posAcciones] & 0xff;
            posAcciones++;
        }

        // devuelve el bit correspondiente de la acción
        boolean bitAccion = (accionActual & (1 << (7 - numBitAcciones))) != 0;

        // incrementa los bits procesados
        numBitAcciones = (numBitAcciones + 1) & 0x07;

        return bitAccion ? 1 : 0;
    }

    int leeComando() {
        // comprueba si el personaje tiene que avanzar y si es así, devuelve 0 o 1
        if (enDesnivel) {
            if (leeBit() == 1) {
                return leeBit();
            }
        } else {
            if (leeBit() == 1) {
                return 1;
            }
        }

        // si es un comando de giro, devuelve 2 o 3
        if (leeBit() == 1) {
            return 2 + leeBit();
        }

        // si es un comando para subir o bajar, devuelve 4 o 5
        if (leeBit() == 1) {
            return 4 + leeBit();
        }

        // si se leyó 0000
        if (leeBit() == 0) {
            if (enDesnivel) {
                return 0;
            } else {
                // reinicia los contadores
                reiniciaPosicionBuffer();

                // sigue procesando comandos
                return leeComando();
            }
        } else {
            // si se leyó 0001, indica que hay que pensar un nuevo movimiento
            pensarNuevoMovimiento = true;

            return -1;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con la generación de comandos de movimiento
    /////////////////////////////////////////////////////////////////////////////

    // escribe los comandos necesarios para obtener la orientación desesada
    void modificaOrientacion(int oriDeseada) {
        // calcula cuanto hay que girar
        int difOri = orientacion - oriDeseada;

        if (difOri < 0) {
            difOri = (-difOri) ^ 2;

            if (difOri == 0) {
                difOri = 2;
            }
        }

        // escribe los comandos necesarios para girar
        escribeComandos(comandosGirar[difOri][0], comandosGirar[difOri][1]);
    }

    // escribe unos comandos dependiendo de si el personaje sube, baja o se mantiene
    void avanzaPosicion(int difAltura1, int difAltura2, int avanceX, int avanceY) {
        int numEntrada;
        bajando = false;

        // si el personaje ocupa 4 posiciones
        if (!enDesnivel) {
            // si se quiere subir o bajar, cambia la altura e indica que el personaje está en desnivel
            if (difAltura1 == 1) {
                numEntrada = 1;
                altura++;
                enDesnivel = true;
            } else if (difAltura1 == -1) {
                numEntrada = 3;
                altura--;
                enDesnivel = true;
                bajando = true;
            } else {
                numEntrada = 4;
                incrementaPos(avanceX, avanceY);
            }

            // si se pasa a una zona con desnivel
            if (enDesnivel) {
                incrementaPos(avanceX, avanceY);

                if (cambioCentro()) {
                    incrementaPos(avanceX, avanceY);
                }
            }
        } else {
            // si el personaje ocupa 1 posición
            numEntrada = 0;

            // si está girado en desnivel, avanza
            if (giradoEnDesnivel) {
                incrementaPos(avanceX, avanceY);
            } else {
                // en otro caso, si quiere subir o bajar, cambia la altura
                if (difAltura1 == 1) {
                    altura++;
                } else {
                    numEntrada = 2;
                    altura--;
                    bajando = true;
                }

                // si las 2 posiciones que hay avanzando tienen la misma altura, indica que ya no está en desnivel
                if (difAltura1 == difAltura2) {
                    numEntrada++;
                    enDesnivel = false;

                    incrementaPos(avanceX, avanceY);

                    if (!cambioCentro()) {
                        incrementaPos(avanceX, avanceY);
                    }
                } else {
                    incrementaPos(avanceX, avanceY);
                }
            }
        }

        // escribe los comandos necesarios para avanzar según la situación
        escribeComandos(comandosAvanzar[numEntrada][0], comandosAvanzar[numEntrada][1]);
    }

    // fija la primera posición para coger comandos del buffer
    void reiniciaPosicionBuffer() {
        posAcciones = 0;
        numBitAcciones = 0;
        pensarNuevoMovimiento = false;
    }

    // descarta los movimientos pensados e indica que hay que pensar un nuevo movimiento
    void descartarMovimientosPensados() {
        // descarta los movimientos anteriores
        reiniciaPosicionBuffer();

        // escribe el comando para pensar un nuevo movimiento
        bufAcciones[0] = 0x10;
    }

    // escribe los bits de los comandos que se le pasa en el buffer de acciones del personaje
    void escribeComandos(int comandos, int bits) {
        // para todos los bits del comando
        for (int i = 0; i < bits; i++) {
            // si se ha completado el byte actual
            if (numBitAcciones == 8) {
                numBitAcciones = 0;

                // graba el comando actual
                bufAcciones[posAcciones] = (byte) accionActual;

                // avanza la posición del buffer
                posAcciones++;
            }

            // escribe el bit actual
            accionActual = (accionActual << 1) | (((comandos & 0x8000) >> 15) & 0x01);
            comandos = (comandos << 1) & 0xffff;
            numBitAcciones++;
        }
    }

    // graba los datos principales del personaje por si hay que restaurarlos
    void grabaEstado() {
        bufferDatosPersonaje[0] = orientacion;
        bufferDatosPersonaje[1] = posX;
        bufferDatosPersonaje[2] = posY;
        bufferDatosPersonaje[3] = altura;
        bufferDatosPersonaje[4] = bajando ? 1 : 0;
        bufferDatosPersonaje[5] = enDesnivel ? 1 : 0;
        bufferDatosPersonaje[6] = giradoEnDesnivel ? 1 : 0;
        bufferDatosPersonaje[7] = numBitAcciones;
        bufferDatosPersonaje[8] = accionActual & 0xff;
        bufferDatosPersonaje[9] = posAcciones;
    }

    // restaura los datos guardados del personaje
    void cargaEstado() {
        orientacion = bufferDatosPersonaje[0];
        posX = bufferDatosPersonaje[1];
        posY = bufferDatosPersonaje[2];
        altura = bufferDatosPersonaje[3];
        bajando = bufferDatosPersonaje[4] == 1;
        enDesnivel = bufferDatosPersonaje[5] == 1;
        giradoEnDesnivel = bufferDatosPersonaje[6] == 1;
        numBitAcciones = bufferDatosPersonaje[7];
        accionActual = bufferDatosPersonaje[8] & 0xff;
        posAcciones = bufferDatosPersonaje[9];
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos auxiliares para la lógica
    /////////////////////////////////////////////////////////////////////////////

    boolean estaCerca(Personaje pers) {
        // si los 2 personajes no están en la misma planta, devuelve false
        if (juego.motor.obtenerAlturaBasePlanta(altura) != juego.motor.obtenerAlturaBasePlanta(pers.altura)) {
            return false;
        }

        // si la distancia en X supera un umbral, devuelve false
        int dist = pers.posX - posX + distanciasOri[orientacion][0];
        if ((dist < 0) || (dist >= distanciasOri[orientacion][1])) {
            return false;
        }

        // si la distancia en Y supera un umbral, devuelve false
        dist = pers.posY - posY + distanciasOri[orientacion][2];
        if ((dist < 0) || (dist >= distanciasOri[orientacion][3])) {
            return false;
        }

        // si llega aquí es porque el personaje está cerca
        return true;
    }

    // si ha llegado al sitio al que quería llegar, avanza el estado
    boolean siHaLlegadoAvanzaEstado() {
        if (aDondeHaLlegado != aDondeVa) {
            return false;
        }

        estado++;

        return true;
    }
}
