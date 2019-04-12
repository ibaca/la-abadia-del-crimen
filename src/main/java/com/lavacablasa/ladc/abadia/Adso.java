package com.lavacablasa.ladc.abadia;

import com.lavacablasa.ladc.core.Input;

import static com.lavacablasa.ladc.abadia.MomentosDia.COMPLETAS;
import static com.lavacablasa.ladc.abadia.MomentosDia.NOCHE;
import static com.lavacablasa.ladc.abadia.MomentosDia.PRIMA;
import static com.lavacablasa.ladc.abadia.MomentosDia.SEXTA;
import static com.lavacablasa.ladc.abadia.MomentosDia.VISPERAS;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LAMPARA;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.PERGAMINO;
import static com.lavacablasa.ladc.abadia.Orientacion.ABAJO;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_GUILLERMO;

class Adso extends PersonajeConIA {

    /////////////////////////////////////////////////////////////////////////////
    // tabla de orientaciones a probar para moverse en un determinado sentido
    // Hay 2 grandes grupos de entradas en la tabla. Las primeras 4 entradas dan más prioridad a los
    // movimientos a la derecha y las 4 últimas a los movimientos a la izquierda. Dentro de cada grupo
    // de entradas, las 2 primeras entradas dan más prioridad a los movimientos hacia abajo, y las otras
    // 2 entradas dan más prioridad a los movimientos hacia arriba.
    /////////////////////////////////////////////////////////////////////////////

    private static final int[][] oriMovimiento = {
        { 3, 0, 2, 1 },
        { 0, 3, 1, 2 },
        { 1, 0, 2, 3 },
        { 0, 1, 3, 2 },

        { 3, 2, 0, 1 },
        { 2, 3, 1, 0 },
        { 1, 2, 0, 3 },
        { 2, 1, 3, 0 }
    };

    /////////////////////////////////////////////////////////////////////////////
    // tabla de desplzamientos dentro del buffer de alturas según la orientación
    /////////////////////////////////////////////////////////////////////////////

    private static final int[][] despBufferSegunOri = {
        { +1,  0 },
        {  0, -1 },
        { -1,  0 },
        {  0, +1 }
    };

    /////////////////////////////////////////////////////////////////////////////
    // tabla de la animación del personaje
    /////////////////////////////////////////////////////////////////////////////

    private static final DatosFotograma[] tablaAnimacion = {
        new DatosFotograma(0xa78a, 0x05, 0x20 ),
        new DatosFotograma(0xa6ea, 0x05, 0x20 ),
        new DatosFotograma(0xa78a, 0x05, 0x20 ),
        new DatosFotograma(0xa82a, 0x05, 0x1f ),
        new DatosFotograma(0xa8c5, 0x04, 0x1e ),
        new DatosFotograma(0xa93d, 0x04, 0x1e ),
        new DatosFotograma(0xa8c5, 0x04, 0x1e ),
        new DatosFotograma(0xa9b5, 0x04, 0x1e )
    };

    /////////////////////////////////////////////////////////////////////////////
    // posiciones a las que puede ir el personaje según el estado
    /////////////////////////////////////////////////////////////////////////////

    private static final PosicionJuego[] posicionesPredef = {
        new PosicionJuego(ABAJO, 0x84, 0x4e, 0x02),     // posición en la iglesia
        new PosicionJuego(ABAJO, 0x34, 0x39, 0x02),     // posición en el refectorio
        new PosicionJuego(ABAJO, 0xa8, 0x18, 0x00)      // posición en la celda
    };


    int oldEstado;              // indica el estado anterior de adso
    int movimientosFrustados;   // indica el número de movimientos frustados del personaje
    int cntParaDormir;          // contador para controlar el tiempo transcurrido desde que se le pregunta a guillermo si duermen

    Adso(Juego juego, Sprite sprite) {
        super(juego, sprite);

        // asigna la tabla de animación del personaje
        animacion = tablaAnimacion;
        numFotogramas = 8;

        mascarasPuertasBusqueda = 0x3c;

        // asigna las posiciones predefinidas
        posiciones = posicionesPredef;
    }

    /////////////////////////////////////////////////////////////////////////////
    // movimiento
    /////////////////////////////////////////////////////////////////////////////

    void run()
    {
        // inicialmente el personaje va a tratar de moverse
        juego.logica.buscRutas.seBuscaRuta = true;

        // ejecuta la lógica del personaje
        piensa();

        // modifica el número aleatorio generado
        juego.logica.numeroAleatorio = bufAcciones[0] & 0xff;

        // modifica la tabla de conexiones de las habitaciones dependiendo de las puertas a las que se tenga acceso
        juego.logica.buscRutas.modificaPuertasRuta(mascarasPuertasBusqueda);

        // mueve el personaje
        mueve();

        if (aDondeVa != POS_GUILLERMO){
            // si se han terminado los comandos de movimiento, genera los comandos para ir a donde se quiere
            juego.logica.buscRutas.generaAccionesMovimiento(this);
        } else {
            // si va hacia donde se mueve guillermo

            // si la cámara no sigue a guillermo o a adso, sale
            if (juego.logica.numPersonajeCamara >= 2) return;

            if (!pensarNuevoMovimiento){
                // si tiene un movimiento pensado pero no se pudo mover hacia donde quería
                if (!juego.logica.hayMovimiento){
                    // si no ha podido moverse varias veces, piensa otro movimiento
                    movimientosFrustados++;

                    if (movimientosFrustados >= 10){
                        movimientosFrustados = 0;

                        posAcciones = 0;
                        numBitAcciones = 0;
                        bufAcciones[0] = 0x10;
                    }
                }

                return;
            }

            // aquí llega si adso no tenía un movimiento pensado

            Personaje guillermo = juego.logica.guillermo;
            RejillaPantalla rejilla = juego.motor.rejilla;

            int[] posRejilla = new int[2];

            // si adso está en la pantalla que se muestra
            if (rejilla.estaEnRejillaCentral(this, posRejilla)){
                // si se pulsó cursor arriba
                if (juego.controles.estaSiendoPulsado(Input.UP)){

                    int[] difAltura = new int[2];
                    int[] avance = new int[2];

                    // obtiene las posiciones hacia las que se va a mover guillermo
                    rejilla.obtenerAlturaPosicionesAvance2(guillermo, difAltura, avance);

                    // comprueba si adso está en alguna de las posiciones a las que va a moverse guillermo
                    int aux = rejilla.bufCalculoAvance[0][1] | rejilla.bufCalculoAvance[0][2] | rejilla.bufCalculoAvance[1][1] | rejilla.bufCalculoAvance[1][2];

                    // si adso impide el paso a guillermo, lo aparta
                    if ((aux & 0x20) != 0){
                        liberaPasoAGuillermo();
                        return;
                    }
                }

                // si se pulsó cursor abajo, trata de avanzar en la orientación de guillermo
                if (juego.controles.estaSiendoPulsado(Input.DOWN)){
                    avanzaSegunGuillermo();
                    return;
                }

                // limpia las posiciones que ocupan adso y guillermo en el buffer de alturas
                marcaPosicion(rejilla, 0);
                guillermo.marcaPosicion(rejilla, 0);

                // ajusta la posición de adso y guillermo a las coordenadas de rejilla
                int[] posDest = new int[2];
                boolean noHayError = rejilla.ajustaAPosRejilla(posX, posY, posDest);
                int[] tmp = {juego.logica.buscRutas.posXIni, juego.logica.buscRutas.posYIni};
                noHayError |= rejilla.ajustaAPosRejilla(guillermo.posX, guillermo.posY, tmp);
                juego.logica.buscRutas.posXIni = tmp[0];
                juego.logica.buscRutas.posYIni = tmp[1];
                assert(noHayError);

                // busca un camino para llegar a donde está guillermo
                RejillaPantalla temp = juego.logica.buscRutas.rejilla;
                juego.logica.buscRutas.rejilla = rejilla;
                boolean encontrado = juego.logica.buscRutas.buscaEnPantalla(posDest[0], posDest[1]);
                juego.logica.buscRutas.limpiaBitsBusquedaEnPantalla();
                juego.logica.buscRutas.rejilla = temp;

                // restaura las posiciones que ocupan adso y guillermo en el buffer de alturas
                marcaPosicion(rejilla, valorPosicion);
                guillermo.marcaPosicion(rejilla, guillermo.valorPosicion);

                // si no encontró un camino para llegar a donde está guillermo, sale
                if (!encontrado) return;

                int numIteraciones = 4;

                if (!enDesnivel){
                    numIteraciones--;

                    // si ninguna de las coordenadas es igual, se permite una iteración más
                    if (posX != guillermo.posX){
                        if (posY != guillermo.posY){
                            numIteraciones++;
                        }
                    }
                }

                // si el número de iteraciones para llegar a guillermo es grande, avanza hacia guillermo
                if (juego.logica.buscRutas.nivelRecursion >= numIteraciones){
                    // avanza hacia guillermo
                    grabaComandosAvance(juego.logica.buscRutas.oriFinal);

                    // ejecuta otra vez el comportamiento de adso
                    run();
                }
            } else {
                // aquí llega si adso no está en la zona de la pantalla que se muestra
                juego.logica.buscRutas.alternativaActual = 0;
                juego.logica.buscRutas.numAlternativas = 1;

                // busca un camino para ir a por guillermo
                if (juego.logica.buscRutas.buscaCamino(this, guillermo) == -1){
                    // si encuentra el camino, ejecuta otra vez el comportamiento de adso
                    run();
                }
            }
        }
    }

/////////////////////////////////////////////////////////////////////////////
// comportamiento
/////////////////////////////////////////////////////////////////////////////

    // Los estados en los que puede estar adso son:
    //		0x00 . estado incial
    //		0x01 . estado en prima y vísperas para ir a misa
    //		0x04 . estado dentro de su celda, esperando contestación de si duerme o no
    //		0x05 . estado que se alcanza una vez que guillermo le dice que no se duerme
    //		0x06 . estado fuera de la celda por la noche o en completas para dirigirse a su celda
    //		0x07 . estado en sexta para ir al refectorio
    void piensa()
    {
        int numFrase = 0;
        Personaje guillermo = juego.logica.guillermo;

        if ((guillermo.objetos & PERGAMINO) == PERGAMINO){
            juego.logica.pergaminoGuardado = false;
        }

        // si se está acabando la noche, pone en el marcador la frase: PRONTO AMANECERA, MAESTRO
        if (juego.logica.seAcabaLaNoche){
            juego.gestorFrases.muestraFrase(0x27);
        }

        // si hay un cambio de estado de la lámpara, informa de ello
        if (juego.logica.cambioEstadoLampara == 1){
            juego.logica.cambioEstadoLampara = 0;
            juego.gestorFrases.muestraFraseYa(0x28);	// LA LAMPARA SE AGOTA
        } else if (juego.logica.cambioEstadoLampara == 2){
            juego.logica.cambioEstadoLampara = 0;
            juego.logica.usandoLampara = false;
            juego.gestorFrases.muestraFraseYa(0x2a);	// SE HA AGOTADO LA LAMPARA

            // inicia el contador de tiempo que pueden ir a oscuras
            juego.logica.cntTiempoAOscuras = 0x32;

            // pone la pantalla en negro y le quita la lámpara a adso
            juego.motor.genPant.limpiaPantalla(3);
            juego.logica.reiniciaContadoresLampara();
        }

        // si guillermo no ha muerto
        if (!juego.logica.haFracasado){
            // si se ha activado el contador del tiempo que pueden ir a oscuras
            if (juego.logica.cntTiempoAOscuras >= 1){
                // si guillermo ha salido de la biblioteca, reinicia el contador
                if (guillermo.altura < 0x18){
                    juego.logica.cntTiempoAOscuras = 0;

                    return;
                }

                // decrementa el tiempo que pueden ir a oscuras
                juego.logica.cntTiempoAOscuras--;

                // si se termina el contador, muestra la frase JAMAS CONSEGUIREMOS SALIR DE AQUI y termina la partida
                if (juego.logica.cntTiempoAOscuras == 1){
                    juego.logica.haFracasado = true;
                    juego.gestorFrases.muestraFraseYa(0x2b);

                    return;
                }
            } else {
                // si adso acaba de entrar en la biblioteca
                if (altura >= 0x18){
                    // sigue a guillermo
                    aDondeVa = POS_GUILLERMO;

                    // si adso no tiene la lámpara, muestra la frase: DEBEMOS ENCONTRAR UNA LAMPARA, MAESTRO
                    if ((objetos & LAMPARA) != LAMPARA){
                        juego.gestorFrases.muestraFraseYa(0x13);

                        // inicia el contador de tiempo que pueden ir a oscuras
                        juego.logica.cntTiempoAOscuras = 0x64;

                        return;
                    } else {
                        // en otro caso, enciende la lámpara
                        juego.logica.usandoLampara = true;
                    }
                } else {
                    // si adso no está en la biblioteca, apaga la lámpara y reinicia el contador del tiempo que pueden ir a oscuras
                    juego.logica.usandoLampara = false;
                    juego.logica.cntTiempoAOscuras = 0;
                }
            }
        }

        // realiza acciones dependiendo del momento del día
        switch (juego.logica.momentoDia){
            case SEXTA:	// va al refectorio
                aDondeVa = 1;
                oldEstado = 7;
                numFrase = 0x0c;	// frase DEBEMOS IR AL REFECTORIO, MAESTRO
                break;

            case PRIMA:	case VISPERAS:	// va a la iglesia
                aDondeVa = 0;
                oldEstado = 1;
                numFrase = 0x0b;	// frase DEBEMOS IR A LA IGLESIA, MAESTRO
                break;

            case COMPLETAS:	// va a su celda
                estado = 6;
                aDondeVa = 2;
                return;

            case NOCHE:
                // si está esperando contestación de si se duerme o no
                if (estado == 4){
                    // si está en la pantalla de fuera de la celda, indica que hay que avanzar el momento del día
                    if (juego.motor.numPantalla == 0x37){
                        juego.logica.avanzarMomentoDia = true;
                    }

                    // si no se está reproduciendo una voz
                    if (!juego.gestorFrases.mostrandoFrase){
                        // si lleva mucho tiempo sin responder a la pregunta de dormir, duerme
                        if (cntParaDormir >= 100){
                            juego.logica.avanzarMomentoDia = true;
                        } else {
                            // incrementa el contador para dormir
                            cntParaDormir++;

                            // escribe o limpia S:N
                            escribeSN((cntParaDormir & 0x01) == 0);

                            // comprueba si se pulso la S o la N
                            if ((cntParaDormir & 0x01) == 0x01){
                                if (juego.controles.estaSiendoPulsado(Input.S)){
                                    juego.logica.avanzarMomentoDia = true;
                                }

                                if (juego.controles.estaSiendoPulsado(Input.N)){
                                    estado = 5;
                                }
                            }
                        }
                    }

                    return;
                } else {
                    aDondeVa = POS_GUILLERMO;

                    // si no dormimos
                    if (estado == 5){
                        // si estamos en nuestra celda, sale
                        if (juego.motor.numPantalla == 0x3e){
                            return;
                        }

                        // si ha salido de la celda, cambia el estado
                        estado = 6;
                    }

                    // si estabamos fuera de la celda
                    if (estado == 6){
                        // si estamos cerca de guillermo y en nuestra celda
                        if (estaCerca(guillermo) && (juego.motor.numPantalla == 0x3e)){
                            // reinicia el contador para dormir y pone la frase ¿DORMIMOS?, MAESTRO
                            cntParaDormir = 0;
                            estado = 4;
                            juego.gestorFrases.muestraFrase(0x12);
                        }

                        return;
                    }
                }
            default:
                // sigue a guillermo
                aDondeVa = POS_GUILLERMO;

                return;
        }

        // aquí solo llega en SEXTA, PRIMA y VISPERAS

        // si ha cambiado el estado, muestra una frase
        if (estado != oldEstado){
            // si esta cerca de guillermo muestra la frase correspondiente
            if (estaCerca(guillermo)){
                juego.gestorFrases.muestraFrase(numFrase);
            }
            estado = oldEstado;
        }
    }

    // oculta o muestra el texto S:N
    void escribeSN(boolean muestra)
    {
        if (muestra){
            juego.marcador.imprimeFrase("S:N", 148, 164, 2, 3);
        } else {
            juego.marcador.imprimeFrase("   ", 148, 164, 2, 3);
        }
    }

    void grabaComandosAvance(int nuevaOri)
    {
        int oldPosX = posX;
        int oldPosY = posY;
        int oldAltura = altura;
        int oldOri = orientacion;
        boolean oldEnDesnivel = enDesnivel;
        boolean oldGiradoEnDesnivel = giradoEnDesnivel;
        boolean oldBajando = bajando;

        reiniciaPosicionBuffer();

        // comprueba si debe cambiar la orientación del personaje
        if (orientacion != nuevaOri){
            modificaOrientacion(nuevaOri);
            orientacion = nuevaOri;
        }

        int[] difAltura = new int[2];
        int[] avance = new int[2];

        // obtiene la altura de las posiciones hacia las que se va a mover
        juego.motor.rejilla.obtenerAlturaPosicionesAvance(this, difAltura, avance);

        // escribe un comando dependiendo de si sube, baja o se mantiene
        avanzaPosicion(difAltura[0], difAltura[1], avance[0], avance[1]);

        escribeComandos(0x1000, 12);
        reiniciaPosicionBuffer();

        bajando = oldBajando;
        giradoEnDesnivel = oldGiradoEnDesnivel;
        enDesnivel = oldEnDesnivel;
        orientacion = oldOri;
        altura = oldAltura;
        posY = oldPosY;
        posX = oldPosX;
    }

    // trata de avanzar en la orientación de guillermo
    void avanzaSegunGuillermo()
    {
        // limpia la posición ocupada por adso
        marcaPosicion(juego.motor.rejilla, 0);

        // calcula la entrada con las orientaciones a probar para moverse
        int numEntrada = juego.logica.guillermo.orientacion + 1;
        if (numEntrada == 3) numEntrada = 7;

        // prueba a moverse hacia otra orientación según la entrada correspondiente
        pruebaMover(numEntrada);
    }

    // se aparta del avance de guillermo
    void liberaPasoAGuillermo()
    {
        // limpia la posición ocupada por adso
        marcaPosicion(juego.motor.rejilla, 0);

        int numEntrada = 0;

        // calcula la distancia en x entre guillermo y adso
        int distX = posX - juego.logica.guillermo.posX;

        // si adso está a la izquierda de guillermo, modifica la entrada
        if (distX < 0){
            distX = -distX;
            numEntrada |= 0x04;
        }

        // calcula la distancia en y entre guillermo y adso
        int distY = posY - juego.logica.guillermo.posY;

        // si adso está a la izquierda de guillermo, modifica la entrada
        if (distY < 0){
            distY = -distY;
            numEntrada |= 0x02;
        }

        // si la distancia en y es menor que la distancia en x, modifica la entrada
        if (distY < distX){
            numEntrada++;
        }

        // prueba a moverse hacia otra orientación según la entrada correspondiente
        pruebaMover(numEntrada);
    }

    // prueba a moverse hacia otra orientación según la entrada correspondiente
    void pruebaMover(int numEntrada)
    {
        int[] posRejilla = new int[2];

        // obtiene la posición del personaje dentro de la rejilla
        juego.motor.rejilla.estaEnRejillaCentral(this, posRejilla);

        // obtiene la altura base del personaje
        int alturaBase = juego.motor.rejilla.bufAlturas[posRejilla[1]][posRejilla[0]] & 0x0f;

        RejillaPantalla temp = juego.logica.buscRutas.rejilla;
        juego.logica.buscRutas.rejilla = juego.motor.rejilla;

        // recorre las 3 orientaciones a probar
        for (int i = 0; i < 3; i++){
            int ori = oriMovimiento[numEntrada][i];
            int posX = posRejilla[0] + despBufferSegunOri[ori][0];
            int posY = posRejilla[1] + despBufferSegunOri[ori][1];
            juego.motor.rejilla.bufAlturas[posY][posX] &= 0x7f;

            // obtiene la altura si se mueve en esa orientación
            int altura = juego.motor.rejilla.bufAlturas[posY][posX] & 0xff;

            // si la posición es accesible, se mueve hacia esa posición
            if (juego.logica.buscRutas.esPosicionDestino(posX, posY, altura, alturaBase, false)){
                juego.motor.rejilla.bufAlturas[posY][posX] &= 0x7f;
                grabaComandosAvance(ori);

                // marca la posición que ocupa adso
                marcaPosicion(juego.motor.rejilla, valorPosicion);

                juego.logica.buscRutas.rejilla = temp;

                // ejecuta otra vez el comportamiento de adso
                run();

                return;
            }
        }

        // marca la posición que ocupa adso
        marcaPosicion(juego.motor.rejilla, valorPosicion);

        juego.logica.buscRutas.rejilla = temp;
    }
}
