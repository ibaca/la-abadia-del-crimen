package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.MomentosDia.COMPLETAS;
import static com.lavacablasa.ladc.abadia.MomentosDia.NOCHE;
import static com.lavacablasa.ladc.abadia.MomentosDia.PRIMA;
import static com.lavacablasa.ladc.abadia.MomentosDia.TERCIA;
import static com.lavacablasa.ladc.abadia.MomentosDia.VISPERAS;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LLAVE3;
import static com.lavacablasa.ladc.abadia.Orientacion.ABAJO;
import static com.lavacablasa.ladc.abadia.Orientacion.ARRIBA;
import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;
import static com.lavacablasa.ladc.abadia.Orientacion.IZQUIERDA;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_ABAD;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_GUILLERMO;

import com.lavacablasa.ladc.core.Input;

class Malaquias extends Monje {

    /////////////////////////////////////////////////////////////////////////////
    // posiciones a las que puede ir el personaje según el estado
    /////////////////////////////////////////////////////////////////////////////

    private static final PosicionJuego[] posicionesPredef = {
            new PosicionJuego(ABAJO, 0x84, 0x48, 0x02), // posición en la iglesia
            new PosicionJuego(ARRIBA, 0x2f, 0x37, 0x02), // posición en el refectorio
            new PosicionJuego(ABAJO, 0x37, 0x38, 0x0f), // posición en la mesa que está en la entrada del pasillo para poder subir a la biblioteca
            new PosicionJuego(IZQUIERDA, 0x3a, 0x34, 0x0f), // posición para cerrar el paso al pasillo que lleva a la biblioteca
            new PosicionJuego(DERECHA, 0x5d, 0x77, 0x00), // posición para cerrar las 2 puertas del ala izquierda de la abadía
            new PosicionJuego(DERECHA, 0x58, 0x2a, 0x00), // posición en frente de la mesa de la cocina de delante del pasadizo
            new PosicionJuego(ABAJO, 0x35, 0x37, 0x13), // posición donde deja la llave en la mesa que está en la entrada del pasillo para poder subir a la biblioteca
            new PosicionJuego(DERECHA, 0xbc, 0x18, 0x02), // posición en su celda
            new PosicionJuego(DERECHA, 0x68, 0x52, 0x00) // posición en la celda de severino
    };

    int estaMuerto;             // indica si el personaje está muerto o muriéndose
    int estado2;                // guarda información extra sobre el estado del personaje
    int contadorEnScriptorium;  // indica el tiempo que guillermo está sin salir del scriptorium

    Malaquias(Juego juego, SpriteMonje sprite) {
        super(juego, sprite);

        // coloca los datos de la cara de malaquías
        datosCara[0] = 0xb1cb;
        datosCara[1] = 0xb1cb + 0x32;

        mascarasPuertasBusqueda = 0x3f;

        // asigna las posiciones predefinidas
        posiciones = posicionesPredef;
    }

    /////////////////////////////////////////////////////////////////////////////
    // comportamiento
    /////////////////////////////////////////////////////////////////////////////

    // Los estados en los que puede estar malaquías son:
    //		0x00 . estado en su mesa de trabajo del scriptorium
    //		0x02 . estado después de coger la llave de su mesa
    //		0x03 . estado después de advertir a guillermo que abandone el scriptorium
    //		0x04 . una vez que guillermo ha bajado del scriptorium, va a cerrar las puertas del ala izquierda
    //		0x05 . una vez cerradas las puertas, se dirige a la cocina
    //		0x06 . va a la iglesia (en vísperas)
    //		0x07 . después de que haya llegado a su sitio en la iglesia
    //		0x08 . si se va a su celda en completas/noche
    //		0x09 . va a la iglesia (en prima)
    //		0x0a . estado en el que va a su mesa de trabajo
    //		0x0b . estado en el que empieza a morirse
    //		0x0c . va a buscar al abad para que eche a guillermo
    void piensa() {
        int momentoDia = juego.logica.momentoDia;

        // si malaquías ha muerto, sale
        if (estaMuerto == 2) {
            juego.logica.buscRutas.seBuscaRuta = false;

            return;
        }

        // si malaquías está muriendo, incrementa su altura
        if (estaMuerto == 1) {
            altura++;

            // si ha desaparecido de la pantalla, muere
            if (altura >= 0x14) {
                posX = posY = altura = 0;
                estaMuerto = 2;
                aDondeHaLlegado = 0;
            }
            juego.logica.buscRutas.seBuscaRuta = false;

            return;
        }

        // si el abad va a echar a guillermo de la abadía, se queda quieto
        if (juego.logica.abad.estado == 0x0b) {
            juego.logica.buscRutas.seBuscaRuta = false;

            return;
        }

        // si es de noche o completas, se va a su celda y cambia al estado 8
        if ((juego.logica.momentoDia == NOCHE) || (juego.logica.momentoDia == COMPLETAS)) {
            aDondeVa = 7;
            estado = 0x08;

            return;
        }

        if (juego.logica.momentoDia == VISPERAS) {
            // en el estado 0x0c, busca al abad para que eche a guillermo de la abadía
            if (estado == 0x0c) {
                aDondeVa = POS_ABAD;

                if (aDondeHaLlegado == POS_ABAD) {
                    juego.logica.abad.estado = 0x0b;
                    estado = 0x06;
                }

                return;
            }

            // si está en el estado 0, va a por la llave de su mesa
            if (estado == 0x00) {
                mascaraObjetos = LLAVE3;
                aDondeVa = 6;

                if (aDondeHaLlegado == 6) {
                    estado = 0x02;
                } else {
                    return;
                }
            }

            // si aún no se ha ido del scriptorium
            if (estado < 0x04) {
                // si guillermo está en el scriptorium, va a por él
                if (juego.logica.guillermo.altura >= 0x0c) {
                    aDondeVa = POS_GUILLERMO;
                } else {
                    estado = 0x04;

                    return;
                }

                // si está yéndose del scriptorium y pasa cerca de guillermo, le advierte que debe marcharse
                if (estado == 0x02) {
                    // si está cerca de guillermo, le dice que abandone el scriptorium, pasa al estado 3 e inicia un contador de tiempo
                    if (estaCerca(juego.logica.guillermo)) {
                        // pone en el marcador la frase DEBEIS ABANDONAR EDIFICIO, HERMANO
                        juego.gestorFrases.muestraFraseYa(0x09);
                        estado = 0x03;
                        contadorEnScriptorium = 0;
                    }

                    return;
                }

                // si le ha advertido a guillermo para que se marche y no le hace caso, advierte al abad
                if (estado == 0x03) {
                    contadorEnScriptorium++;

                    // si no ha hecho caso y no ha abandonado el scriptorium, va a buscar al abad
                    if (contadorEnScriptorium >= 0xfa) {
                        // pone en el marcador la frase ADVERTIRE AL ABAD
                        juego.gestorFrases.muestraFraseYa(0x0a);

                        estado = 0x0c;
                    }

                    return;
                }
            }

            // si guillermo ya se ha marchado del scriptorium, va a cerrar las puertas del ala izquierda de la abadía
            if (estado == 0x04) {
                aDondeVa = 4;

                // si ha llegado a las puertas y no han salido berengario o bernardo, los espera
                if (aDondeHaLlegado == 4) {
                    if ((juego.logica.berengario.estaVivo && (juego.logica.berengario.posX < 0x62)) || (
                            juego.logica.bernardo.estaEnLaAbadia && (juego.logica.bernardo.posX < 0x62))) {
                        juego.logica.buscRutas.seBuscaRuta = false;
                    } else {
                        // pasa al estado 5 y cierra las puertas
                        estado = 0x05;
                        juego.puertas[5].estaFija = false;
                        juego.puertas[6].estaFija = false;
                    }
                }

                return;
            }

            // después de cerrar las puertas, se va a la mesa de la cocina delante del pasadizo
            if (estado == 0x05) {
                aDondeVa = 5;
                juego.logica.mascaraPuertas = 0xdf;

                // si guillermo se ha quedado en el ala izquierda de la abadía, advierte al abad
                if (juego.logica.guillermo.posX < 0x60) {
                    estado = 0x0c;
                }

                // si ha llegado a su destino, pasa al siguiente estado
                siHaLlegadoAvanzaEstado();

                return;
            }

            // en el estado 6, va a la iglesia
            if (estado == 0x06) {
                aDondeVa = 0;

                // si ha llegado a su destino, pasa al siguiente estado
                siHaLlegadoAvanzaEstado();
            }

            // si está en el estado 0x0b, empieza a morirse
            if (estado == 0x0b) {
                if (!juego.gestorFrases.mostrandoFrase) {
                    estaMuerto = 0x01;
                }

                return;
            }

            if (estado == 0x07) {
                // si es el quinto día y está en la iglesia, pasa al estado en el que empieza a morirse
                if (juego.logica.dia == 5) {
                    if ((juego.motor.numPantalla == 0x22) || (juego.motor.numPantalla == 0x22)) {
                        // indica que no ha llegado a la iglesia todavía
                        aDondeHaLlegado = 1;

                        // pasa al estado de morirse
                        estado = 0x0b;

                        // pone en el marcador la frase ERA VERDAD, TENIA EL PODER DE MIL ESCORPIONES
                        juego.gestorFrases.muestraFraseYa(0x1f);
                    }
                }
                return;
            }

            return;
        }

        // si es prima, cambia al estado 9 y va a misa
        if (juego.logica.momentoDia == PRIMA) {
            estado = 0x09;
            aDondeVa = 0;

            return;
        }

        // si malaquías ha llegado a su puesto en el scriptorium, deja la llave del pasadizo (si la tiene)
        if (aDondeHaLlegado == 2) {
            estado = 0x00;
            mascaraObjetos = 0;

            if ((objetos & LLAVE3) == LLAVE3) {
                objetos = objetos & 0xfd;

                juego.objetos[6].seHaCogido = false;
                juego.objetos[6].seEstaCogiendo = false;
                juego.objetos[6].personaje = null;
                juego.objetos[6].posX = 0x35;
                juego.objetos[6].posY = 0x35;
                juego.objetos[6].altura = 0x13;
                juego.objetos[6].orientacion = DERECHA;
            }
        }

        if (estado == 0) {
            // si está cerca guillermo
            if (estaCerca(juego.logica.guillermo)) {
                // si ha ido a cerrarle el paso a guillermo
                if (aDondeVa == 3) {
                    // si no le ha dicho que no puede subir a la biblioteca
                    if ((estado2 & 0x80) == 0) {
                        // si guillermo quiere entrar en la biblioteca, le dice que no puede
                        if (juego.logica.guillermo.posY < 0x38) {
                            estado2 |= 0x80;

                            // pone en el marcador la frase LO SIENTO, VENERABLE HERMANO, NO PODEIS SUBIR A LA BIBLIOTECA
                            juego.gestorFrases.muestraFraseYa(0x33);
                        }
                    } else {
                        // si no le ha dicho que berengario le puede enseñar el scriptorium
                        if ((estado2 & 0x40) == 0) {
                            // si es el segundo día y no está reproduciendo una frase, le dice que berengario le puede enseñar el scriptorium
                            if ((juego.logica.dia == 2) && (!juego.gestorFrases.mostrandoFrase)) {
                                estado2 |= 0x40;

                                // pone en el marcador la frase SI LO DESEAIS, BERENGARIO OS MOSTRARA EL SCRIPTORIUM
                                juego.gestorFrases.muestraFrase(0x34);
                            }
                        }
                    }

                    return;
                }

                // si no ha ido a cerrarle el paso a guillermo, pero éste está cerca

                // descarta los movimientos pensados
                descartarMovimientosPensados();

                // si guillermo no avanza, malaquías se queda quieto
                if (!juego.controles.estaSiendoPulsado(Input.UP)) {
                    juego.logica.buscRutas.seBuscaRuta = false;
                } else {
                    // en otro caso, sale a cerrarle el paso
                    aDondeVa = 3;
                }
            } else {
                // si guillermo no está cerca, va a su mesa
                aDondeVa = 2;
            }

            return;
        }

        if (juego.logica.momentoDia == TERCIA) {
            // si es el quinto día, va a matar a severino
            if ((estado == 0x09) && (juego.logica.dia == 5)) {
                aDondeVa = 8;

                // si ha llegado a la celda de severino y éste está en la celda, mata a severino y pasa al estado 0x0a
                if ((aDondeHaLlegado == 8) && (juego.logica.severino.aDondeHaLlegado == 2)) {
                    juego.logica.severino.estaVivo = false;
                    juego.logica.severino.posX = juego.logica.severino.posY = juego.logica.severino.altura = 0;
                    estado = 0x0a;
                }

                return;
            }

            // en el estado 0x0a, va a su mesa de trabajo
            estado = 0x0a;
            aDondeVa = 2;
        }
    }

    // dependiendo de como esté la animación del personaje, avanza la animación o realiza un movimiento
    @Override
    void avanzaAnimacionOMueve() {
        // si se está muriendo, actualiza el sprite
        if (estaMuerto == 1) {
            actualizaSprite();
        } else {
            // en otro caso, avanza la animación o realiza un movimiento
            super.avanzaAnimacionOMueve();
        }
    }
}
