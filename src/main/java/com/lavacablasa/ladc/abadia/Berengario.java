package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.MomentosDia.COMPLETAS;
import static com.lavacablasa.ladc.abadia.MomentosDia.NOCHE;
import static com.lavacablasa.ladc.abadia.MomentosDia.PRIMA;
import static com.lavacablasa.ladc.abadia.MomentosDia.SEXTA;
import static com.lavacablasa.ladc.abadia.MomentosDia.VISPERAS;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LIBRO;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.PERGAMINO;
import static com.lavacablasa.ladc.abadia.Orientacion.ABAJO;
import static com.lavacablasa.ladc.abadia.Orientacion.ARRIBA;
import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;
import static com.lavacablasa.ladc.abadia.Orientacion.IZQUIERDA;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_ABAD;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_GUILLERMO;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_LIBRO;

class Berengario extends Monje {

    /////////////////////////////////////////////////////////////////////////////
    // posiciones a las que puede ir el personaje según el estado
    /////////////////////////////////////////////////////////////////////////////

    private static final PosicionJuego[] posicionesPredef = {
            new PosicionJuego(ABAJO, 0x8c, 0x48, 0x02),     // posición en la iglesia
            new PosicionJuego(ARRIBA, 0x32, 0x35, 0x02),    // posición en el refectorio
            new PosicionJuego(IZQUIERDA, 0x3d, 0x5c, 0x0f), // posición de su mesa en el scriptorium
            new PosicionJuego(DERECHA, 0xbc, 0x15, 0x02),   // celda de los monjes
            new PosicionJuego(ARRIBA, 0x52, 0x67, 0x04),
            // posición al pie de las escaleras para subir al scriptorium
            new PosicionJuego(ARRIBA, 0x68, 0x57, 0x02)     // celda de severino
    };

    boolean estaVivo;       // indica si el personaje está vivo
    int estado2;            // guarda información extra sobre el estado del personaje
    int contadorPergamino;  // contador usado para informar al abad si guillermo no suelta el pergamino

    Berengario(Juego juego, SpriteMonje sprite) {
        super(juego, sprite);

        // coloca los datos de la cara de berengario
        fijaCapucha(false);

        mascarasPuertasBusqueda = 0x3f;

        // asigna las posiciones predefinidas
        posiciones = posicionesPredef;
    }

    /////////////////////////////////////////////////////////////////////////////
    // comportamiento
    /////////////////////////////////////////////////////////////////////////////

    // Los estados en los que puede estar berengario son:
    //		0x00 . estado incial
    //		0x01 . estado en vísperas para ir a la iglesia
    //		0x04 . estado en el que está pendiente de que guillermo deje el pergamino
    //		0x05 . estado en el que va a avisar al abad de que guillermo ha cogido el pergamino
    //		0x06 . en este estado se ha colocado la capucha y se va a por el libro y después a la celda de severino
    void piensa() {
        // si no está vivo, sale
        if (!estaVivo) {
            juego.logica.buscRutas.seBuscaRuta = false;

            return;
        }

        // si es sexta, va al comedor
        if (juego.logica.momentoDia == SEXTA) {
            aDondeVa = 1;

            return;
        }

        // si es prima, va a la iglesia
        if (juego.logica.momentoDia == PRIMA) {
            aDondeVa = 0;

            return;
        }

        // en completas se va a la celda de los monjes
        if (juego.logica.momentoDia == COMPLETAS) {
            aDondeVa = 3;

            return;
        }

        // si es de noche
        if (juego.logica.momentoDia == NOCHE) {
            // si es el tercer día
            if (juego.logica.dia == 3) {
                if (estado == 6) {
                    // indica que puede coger el libro
                    mascaraObjetos = LIBRO;

                    // si está en su celda, va hacia las escaleras al pie del scriptorium
                    if (aDondeHaLlegado == 3) {
                        aDondeVa = 4;
                    } else {
                        // en otro caso, se dirige a por el libro
                        aDondeVa = POS_LIBRO;

                        // si tiene el libro
                        if ((objetos & LIBRO) == LIBRO) {
                            // si ha llegado a la celda de severino, muere y avanza el momento del día
                            if (aDondeHaLlegado == 5) {
                                estaVivo = false;
                                posX = posY = altura = 0;

                                juego.logica.avanzarMomentoDia = true;
                            }

                            // se dirige a la celda de severino
                            aDondeVa = 5;
                        }
                    }

                    return;
                }

                // si está en su celda, cambia su cara por la del encapuchado y pasa al estado 6
                if (aDondeHaLlegado == 3) {
                    fijaCapucha(true);
                    estado = 6;

                    return;
                }
            }

            // se dirige a la celda de los monjes
            aDondeVa = 3;

            return;
        }

        // si es vísperas
        if (juego.logica.momentoDia == VISPERAS) {
            // si es el segundo día y malaquías no ha bajado del scriptorium, se queda quieto protegiendo el pergamino
            if ((juego.logica.dia == 2) && (juego.logica.malaquias.estado < 0x04)) {
                juego.logica.buscRutas.seBuscaRuta = false;
            } else {
                // pasa al estado 1 y va a la iglesia
                estado = 1;
                aDondeVa = 0;
            }

            return;
        }

        // si es el primer o segundo día
        if (juego.logica.dia < 3) {
            // si berengario está pendiente de que guillermo deje el pergamino
            if (estado == 4) {
                // incrementa el contador del tiempo que tiene el pergamino guillermo
                contadorPergamino++;

                // si aún no ha llegado al límite de tiempo y no ha cambiado de pantalla
                if ((contadorPergamino < 0x41) && (juego.motor.numPantalla == 0x40)) {
                    // si guillermo no tiene el pergamino, cambia el estado
                    if ((juego.logica.guillermo.objetos & PERGAMINO) == 0) {
                        estado = 0;
                    }
                } else {
                    // cambia el estado de berengario porque va a avisar al abad
                    estado = 5;

                    // deshabilita el contador para que avance el momento del día de forma automática
                    juego.logica.duracionMomentoDia = 0;
                }

                return;
            }

            if (estado == 5) {
                aDondeVa = POS_ABAD;

                // si ha llegado a la posición del abad, le informa de que guillermo ha cogido el pergamino
                if (aDondeHaLlegado == POS_ABAD) {
                    estado = 0;

                    juego.logica.abad.guillermoHaCogidoElPergamino = true;
                    juego.logica.abad.contador = 0xc9;

                    // indica que ha avisado al abad
                    estado2 |= 0x01;
                }

                return;

            }

            // si berengario está en la mesa del scriptorium
            if (aDondeHaLlegado == 2) {
                // si guillermo ha cogido el pergamino
                if (guillermoHaCogidoElPergamino()) {
                    // reinicia el contador para avisar al abad y cambia de estado
                    contadorPergamino = 0;
                    estado = 4;

                    // si está cerca de guillermo, le dice que deje el manuscrito o advertirá al abad
                    if (estaCerca(juego.logica.guillermo)) {
                        // pone en el marcador la frase DEJAD EL MANUSCRITO DE VENACIO O ADVERTIRE AL ABAD
                        juego.gestorFrases.muestraFrase(0x04);
                    } else {
                        // si ya no está cerca de él, avis al abad
                        estado = 5;

                        // deshabilita el contador para que avance el momento del día de forma automática
                        juego.logica.duracionMomentoDia = 0;
                    }

                    return;
                }
            }

            // si malaquías le ha dicho a guillermo que le enseñe el scriptorium y éste no está en la planta baja
            if (((juego.logica.malaquias.estado2 & 0x40) == 0x40) && (juego.logica.guillermo.altura >= 0x0d)) {
                // si no le ha dicho que aquí trabajan los mejores copistas
                if ((estado2 & 0x10) == 0) {
                    aDondeVa = POS_GUILLERMO;

                    // si está cerca de guillermo
                    if (estaCerca(juego.logica.guillermo)) {
                        if (!juego.gestorFrases.mostrandoFrase) {
                            aDondeHaLlegado = POS_GUILLERMO;

                            // descarta los movimientos pensados
                            descartarMovimientosPensados();

                            // indica que ya le ha dicho la frase
                            estado2 |= 0x10;

                            // pone en el marcador la frase AQUI TRABAJAN LOS MEJORES COPISTAS DE OCCIDENTE
                            juego.gestorFrases.muestraFrase(0x35);

                            juego.logica.buscRutas.seBuscaRuta = false;
                        }
                    }

                    return;
                }

                // si no le ha enseñado dónde trabajaba venacio
                if ((estado2 & 0x08) == 0) {
                    // va a su sitio en el scriptorium
                    aDondeVa = 2;

                    // si está cerca de guillermo
                    if (estaCerca(juego.logica.guillermo)) {
                        // si ha llegado al scriptorium y no estaba reproduciendo una voz
                        if ((aDondeHaLlegado == 2) && (!juego.gestorFrases.mostrandoFrase)) {
                            // indica que ya le ha enseñado dónde trabaja venacio
                            estado2 |= 0x08;

                            // pone en el marcador la frase AQUI TRABAJABA VENACIO
                            juego.gestorFrases.muestraFrase(0x36);
                        }

                        return;
                    }
                }
            }

            estado = 0;

            // no se mueve de su puesto de trabajo
            aDondeVa = 2;

        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda
    /////////////////////////////////////////////////////////////////////////////

    // se pone o se quita la capucha
    void fijaCapucha(boolean puesta) {
        if (!puesta) {
            // coloca los datos de la cara de berengario
            datosCara[0] = 0xb22f;
            datosCara[1] = 0xb22f + 0x32;
        } else {
            // coloca los datos del encapuchado
            datosCara[0] = 0xb35b;
            datosCara[1] = 0xb35b + 0x32;
        }
    }

    // comprueba si guillermo ha cogido el pergamino
    boolean guillermoHaCogidoElPergamino() {
        // si ha avisado al abad, sale
        if ((estado2 & 0x01) == 0x01) return false;

        // si guillermo tiene el pergamino, sale
        return (juego.logica.guillermo.objetos & PERGAMINO) == PERGAMINO;

    }
}
