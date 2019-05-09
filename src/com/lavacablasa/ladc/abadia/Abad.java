package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.MomentosDia.COMPLETAS;
import static com.lavacablasa.ladc.abadia.MomentosDia.NOCHE;
import static com.lavacablasa.ladc.abadia.MomentosDia.NONA;
import static com.lavacablasa.ladc.abadia.MomentosDia.PRIMA;
import static com.lavacablasa.ladc.abadia.MomentosDia.SEXTA;
import static com.lavacablasa.ladc.abadia.MomentosDia.TERCIA;
import static com.lavacablasa.ladc.abadia.MomentosDia.VISPERAS;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LLAVE1;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.PERGAMINO;
import static com.lavacablasa.ladc.abadia.Orientacion.ABAJO;
import static com.lavacablasa.ladc.abadia.Orientacion.ARRIBA;
import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;
import static com.lavacablasa.ladc.abadia.Orientacion.IZQUIERDA;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_ABAD;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_GUILLERMO;

class Abad extends Monje {

    /////////////////////////////////////////////////////////////////////////////
    // posiciones a las que puede ir el personaje según el estado
    /////////////////////////////////////////////////////////////////////////////

    private static final PosicionJuego[] posicionesPredef = {
            new PosicionJuego(ARRIBA, 0x88, 0x3c, 0x04),    // posición en el altar de la iglesia
            new PosicionJuego(IZQUIERDA, 0x3d, 0x37, 0x02), // posición en el refectorio
            new PosicionJuego(DERECHA, 0x54, 0x3c, 0x02),   // posición en su celda
            new PosicionJuego(ARRIBA, 0x88, 0x84, 0x02),    // posición en la entrada de la abadía
            new PosicionJuego(ABAJO, 0xa4, 0x58, 0x00),
            // posición de la primera parada durante el discurso de bienvenida
            new PosicionJuego(DERECHA, 0xa5, 0x21, 0x02),   // posición para que entremos a nuestra celda
            new PosicionJuego(DERECHA, 0x9c, 0x2a, 0x02),
            // posición en la puerta de acceso de los monjes a la iglesia
            new PosicionJuego(DERECHA, 0xc7, 0x27, 0x00),   // posición en la pantalla en la que presenta a jorge
            new PosicionJuego(ABAJO, 0x68, 0x61, 0x02),     // posición en la puerta de la celda de severino
            new PosicionJuego(DERECHA, 0x3a, 0x34, 0x0f)
            // posición a la entrada del pasillo por el que lleva a la biblioteca
    };

    /////////////////////////////////////////////////////////////////////////////
    // personajes que deben estar en la iglesia o en el refectorio según el día
    /////////////////////////////////////////////////////////////////////////////

    private static final int[] monjesIglesiaEnPrima = { 0, 0x36, 0x26, 0x26, 0xa6, 0x02, 0x02 };
    private static final int[] frasesIglesiaEnPrima = { 0, 0x15, 0x18, 0x1a, 0, 0, 0x17 };
    private static final int[] monjesEnRefectorio = { 0, 0x32, 0x22, 0x22, 0x02, 0x02, 0 };
    private static final int[] monjesIglesiaEnVisperas = { 0x36, 0x36, 0x26, 0xa6, 0, 0x02, 0 };

    int contador;                               // contador usado en varias situaciones
    int numFrase;                               // indica la frase que debe decir en varias situaciones
    int guillermoBienColocado;                  // indica si guillermo está bien colocado en misa o en el refectorio
    int lleganLosMonjes;                        // indica si los monjes han llegado a misa o al refectorio
    boolean guillermoHaCogidoElPergamino;       // indica si guillermo ha cogido el pergamino cuando lo vigilaba berengario

    Abad(Juego juego, SpriteMonje sprite) {
        super(juego, sprite);

        // coloca los datos de la cara del abad
        datosCara[0] = 0xb167;
        datosCara[1] = 0xb167 + 0x32;

        mascarasPuertasBusqueda = 0x3f;

        // asigna las posiciones predefinidas
        posiciones = posicionesPredef;
    }

/////////////////////////////////////////////////////////////////////////////
// comportamiento
/////////////////////////////////////////////////////////////////////////////

    // Los estados en los que puede estar el abad son:
    //		0x00 . estado en el que está esperando a que guillermo llegue a la abadía
    //		0x01 . estado en el que va a la primera parada reproduciendo la frase
    //		0x02 . estado en el que va a la primera parada pero ya ha terminado de reproducir la frase
    //		0x03 . estado en el que va a la segunda parada reproduciendo la frase
    //		0x04 . se va a su celda y al llegar avanza el momento del día
    //		0x05 . cambia a este estado cuando está en vísperas para ir a misa
    //		0x06 . estado en completas después de que finalice la misa
    //		0x07 . estado al llegar a la entrada de la celda de guillermo
    //		0x08 . si guillermo tarda en entrar, le ordena que entre
    //		0x09 . va hacia la puerta que comunica las celdas con la iglesia
    //		0x0a . cierra la puerta que comunica las celdas con la iglesia, avanza el momento del día y se va a su celda
    //		0x0b . busca a guillermo y le dice que debe abandonar la abadía
    //		0x0c . estado en que el abad duerme
    //		0x0d . estado en que el abad busca a guillermo para echarlo por la noche
    //		0x0e . cambia a este estado cuando está en prima para ir a misa
    //		0x0f . estado en el que se espera a que el abad termine la frase que le dice tras misa
    //		0x10 . cambia a este estado cuando está en sexta para ir al refectorio, o después de que el abad le diga a guillermo una frase al terminar la misa
    //		0x11 . estado al que se cambia cuando el abad le ha dicho a guillermo que se acerque después de misa
    //		0x12 . estado al que se cambia cuando termina la frase de acercarse y se dice la frase que tenía pensada
    //		0x13 . estado en el que el abad le ha dicho que hay que buscar a severino y van a su celda
    //		0x15 . va a su celda, deja el pergamino y avanza el momento del día
    //		0x1f . estado en el que va a la segunda parada pero ya ha terminado de reproducir la frase
    // si el bit 7 del estado es 1 indica que está recriminandole algo a guillermo
    void piensa() {
        // si guillermo visita el ala izquierda de la abadía el primer día o cuando es prima, lo echa
        if ((juego.logica.guillermo.posX < 0x60) && ((juego.logica.dia == 1) || (juego.logica.momentoDia == PRIMA))) {
            estado = 0x0b;
        }

        // si guillermo visita la biblioteca cuando no es de noche, lo espera en la entrada de la biblioteca y lo echa
        if ((juego.logica.momentoDia >= PRIMA) && (juego.logica.guillermo.altura >= 0x16)) {
            aDondeVa = 9;
            estado = 0x0b;

            return;
        }

        // si está en estado de echar a guillermo de la abadía
        if (estado == 0x0b) {
            // va a por guillermo
            aDondeVa = POS_GUILLERMO;

            // si está cerca de guillermo
            if (estaCerca(juego.logica.guillermo)) {
                // si guillermo no ha muerto y se no se está reproduciendo ninguna frase, lo echa
                if (!juego.logica.haFracasado) {
                    if (!juego.gestorFrases.mostrandoFrase) {
                        // pone en el marcador la frase NO HABEIS RESPETADO MIS ORDENES. ABANDONAD PARA SIEMPRE ESTA ABADIA
                        juego.gestorFrases.muestraFrase(0x0e);

                        juego.logica.haFracasado = true;
                    }
                }
            }

            return;
        }

        // si guillermo ha entrado en la habitación del abad
        if ((juego.motor.numPantalla == 0x0d) && (juego.logica.opcionPersonajeCamara == 0)) {
            // si está cerca de guillermo le recrimina que haya entrado en su celda y le expulsa
            if (estaCerca(juego.logica.guillermo)) {
                aDondeVa = POS_GUILLERMO;

                // pone en el marcador la frase HABEIS ENTRADO EN MI CELDA
                juego.gestorFrases.muestraFrase(0x29);

                estado = 0x0b;
            } else {
                // va a su celda
                aDondeVa = 2;
            }

            return;
        }

        // si ha llegado a su celda y tiene el pergamino
        if ((aDondeHaLlegado == aDondeVa) && (aDondeHaLlegado == 2) && ((objetos & PERGAMINO) == PERGAMINO)) {
            juego.logica.pergaminoGuardado = true;

            // modifica la máscara de los objetos que puede coger para no coger el pergamino otra vez
            mascaraObjetos = 0;

            // deja el pergamino
            juego.logica.dejaObjeto(this);

            juego.logica.cntMovimiento = 0;

            // si está en el estado 0x15 y no tiene el pergamino, cambia de estado y avanza el momento del día
            if ((estado == 0x15) && ((objetos & PERGAMINO) == 0)) {
                estado = 0x10;

                juego.logica.avanzarMomentoDia = true;

                return;
            }
        }

        // si está en el estado 0x15, se va a su celda a dejar el pergamino
        if (estado == 0x15) {
            aDondeVa = 2;

            return;
        }

        // si está recriminando a guillermo
        if (estado >= 0x80) {
            // si ha terminado de reproducirse la frase, vuelve al estado anterior
            if (!juego.gestorFrases.mostrandoFrase) {
                estado = estado & 0x7f;
            } else {
                // en otro caso va a por guillermo
                aDondeVa = POS_GUILLERMO;

                return;
            }
        }

        // si está en visperas, va a la iglesia y espera a que todos estén en su sitio para avanzar el momento del día
        if (juego.logica.momentoDia == VISPERAS) {
            estado = 0x05;

            // comprueba que guillermo esté en la posición correcta en la iglesia
            compruebaPosGuillermoEnIglesia();

            // se dirige al altar
            aDondeVa = 0;

            // frase OREMOS
            numFrase = 0x17;

            // comprueba si los personajes con IA están en su sitio para la misa de vísperas
            compruebaPosMonjesEnIglesiaEnVisperas(juego.logica.dia);

            // espera a que el abad, guillermo y los personajes con IA estén en su sitio para comenzar la misa o la comida
            esperaParaComenzarActo();

            return;
        }

        // si está en prima, va a la iglesia y espera a que todos estén en su sitio para avanzar el momento del día
        if (juego.logica.momentoDia == PRIMA) {
            estado = 0x0e;

            // comprueba que guillermo esté en la posición correcta en la iglesia
            compruebaPosGuillermoEnIglesia();

            // se dirige al altar
            aDondeVa = 0;

            // frase OREMOS
            numFrase = 0x17;

            // comprueba si los personajes con IA están en su sitio para la misa de prima
            compruebaPosMonjesEnIglesiaEnPrima(juego.logica.dia);

            // espera a que el abad, guillermo y los personajes con IA estén en su sitio para comenzar la misa o la comida
            esperaParaComenzarActo();

            return;
        }

        if (juego.logica.momentoDia == SEXTA) {
            estado = 0x10;

            // comprueba que guillermo esté en la posición correcta en el refectorio
            compruebaPosGuillermoEnRefectorio();

            // va al refectorio
            aDondeVa = 1;

            // frase PODEIS COMER, HERMANOS
            numFrase = 0x19;

            // comprueba si los personajes con IA están en su sitio para comer
            compruebaPosMonjesEnRefectorio(juego.logica.dia);

            // espera a que el abad, guillermo y los personajes con IA estén en su sitio para comenzar la misa o la comida
            esperaParaComenzarActo();

            return;
        }

        // si es completas y estaba en el esatdo 0x05
        if ((juego.logica.momentoDia == COMPLETAS) && (estado == 0x05)) {
            estado = 0x06;

            // si están en la iglesia
            if (juego.motor.numPantalla == 0x22) {
                // pone en el marcador la frase PODEIS IR A VUESTRAS CELDAS
                juego.gestorFrases.muestraFrase(0x0d);
            }

            return;
        }

        // si berengario nos ha dicho que guillermo ha cogido el pergamino, va a por guillermo
        if (guillermoHaCogidoElPergamino) {
            aDondeVa = POS_GUILLERMO;

            // si el abad le ha quitado el pergamino a guillermo, cambia de estado para dejarlo en su celda
            if ((objetos & PERGAMINO) == PERGAMINO) {
                estado = 0x15;
                aDondeHaLlegado = POS_GUILLERMO;
                guillermoHaCogidoElPergamino = false;

                return;
            } else {
                // si está cerca de guillermo
                if (estaCerca(juego.logica.guillermo)) {
                    // si el contador ha rebasado el límite, abronca a guillermo
                    if (contador >= 0xc8) {
                        contador = 0;

                        // muestra la frase DADME EL MANUSCRITO, FRAY GUILLERMO
                        juego.gestorFrases.muestraFrase(0x05);
                        juego.logica.decrementaObsequium(2);
                    }

                    contador++;
                } else {
                    // pone el contador al nivel máximo para que le llame la atención a guillermo
                    contador = 0xc9;
                }

                return;
            }
        }

        // si es completas
        if (juego.logica.momentoDia == COMPLETAS) {
            // si ha terminado la misa y no se está reproduciendo ninguna frase, se marcha a la entrada de la celda de guillermo
            if (estado == 0x06) {
                if (!juego.gestorFrases.mostrandoFrase) {
                    contador = 0;
                    aDondeVa = 5;

                    // al llegar a la entrada de la celda de guillermo, avanza el estado
                    siHaLlegadoAvanzaEstado();
                }

                return;
            }

            // si el abad está en la entrada a la celda de guillermo
            if (estado == 0x07) {
                // si guillermo ha entrado en su celda, cambia de estado
                if (juego.motor.numPantalla == 0x3e) {
                    estado = 0x09;
                } else {
                    // si está cerca guillermo, le dice que entre en su celda y pasa al estado 0x08
                    if (estaCerca(juego.logica.guillermo)) {
                        estado = 0x08;

                        // muestra la frase ENTRAD EN VUESTRA CELDA, FRAY GUILLERMO
                        juego.gestorFrases.muestraFrase(0x10);
                    } else {
                        contador++;

                        // si el contador ha sobrepasado el límite, cambia de estado
                        if (contador >= 0x32) {
                            estado = 0x08;
                        }
                    }
                }

                return;
            }

            // si guillermo tarda en entrar en su celda, le ordena que entre
            if (estado == 0x08) {
                // si guillermo ha entrado en su celda, cambia de estado
                if (juego.motor.numPantalla == 0x3e) {
                    estado = 0x09;
                } else {
                    contador++;

                    // si el contador ha sobrepasado el límite, lo mantiene en el máximo
                    if (contador >= 0x32) {
                        contador = 0x32;
                    }

                    // si está cerca de guillermo
                    if (estaCerca(juego.logica.guillermo)) {
                        // si el contador está al máximo, le echa una bronca y lo reinicia
                        if (contador == 0x32) {
                            contador = 0;

                            // muestra la frase ENTRAD EN VUESTRA CELDA, FRAY GUILLERMO
                            juego.gestorFrases.muestraFrase(0x10);
                            juego.logica.decrementaObsequium(2);
                        }
                    } else {
                        // va a por guillermo
                        aDondeVa = POS_GUILLERMO;
                    }
                }

                return;
            }

            if (estado == 0x09) {
                // si guillermo sigue en su celda, se mueve hacia la puerta
                if (juego.motor.numPantalla == 0x3e) {
                    aDondeVa = 6;

                    // al llegar a la puerta, avanza el estado
                    siHaLlegadoAvanzaEstado();
                } else {
                    // si guillermo ha salido de su celda, cambia al estado 0x08 y va a por él
                    descartarMovimientosPensados();
                    estado = 0x08;
                    aDondeVa = POS_GUILLERMO;
                }

                return;
            }

            // si ha llegado a la puerta, la cierra y se avanza el momento del día
            if (estado == 0x0a) {
                juego.logica.avanzarMomentoDia = true;
                juego.logica.mascaraPuertas &= 0xf7;
            }

            return;
        }

        // si es de noche
        if (juego.logica.momentoDia == NOCHE) {
            // va a su celda
            aDondeVa = 2;

            // si ha llegado a su celda después de cerrar la puerta, inicia el contador y cambia de estado
            if ((estado == 0x0a) && (aDondeHaLlegado == 2)) {
                contador = 0;
                estado = 0x0c;
            }

            // si está durmiendo
            if (estado == 0x0c) {
                // si guillermo no está en el ala izquierda de la abadía, sigue durmiendo
                if (juego.logica.guillermo.posX >= 0x60) {
                    contador++;

                    // si el contador ha sobrepasado el límite o es el quinto día y hemos cogido la llave del abad, se despierta
                    if ((contador >= 0xfa) || ((juego.logica.dia == 5) && ((juego.logica.guillermo.objetos & LLAVE1)
                            == LLAVE1))) {
                        estado = 0x0d;
                    }
                }

                return;
            }

            // si el abad se ha despertado
            if (estado == 0x0d) {
                // si guillermo está en el ala izquierda de la abadía o en su celda, se vuelve a dormir
                if ((juego.logica.guillermo.posX < 0x60) || (juego.motor.numPantalla == 0x3e)) {
                    estado = 0x0c;
                    contador = 0x32;
                } else {
                    // si está cerca de guillermo, lo echa
                    if (estaCerca(juego.logica.guillermo)) {
                        estado = 0x0b;
                    }

                    aDondeVa = POS_GUILLERMO;
                }
            }

            return;
        }

        // si es el primer día y nona, le explica a guillermo las normas de la abadía
        if (juego.logica.dia == 1) {
            if (juego.logica.momentoDia == NONA) {
                if (estado == 0x04) {
                    // va a su celda
                    aDondeVa = 2;

                    // si ha llegado a su celda, avanza el momento del día
                    if (aDondeHaLlegado == 2) {
                        juego.logica.avanzarMomentoDia = true;
                    }

                    return;
                }

                // si está esperando a que guillermo llegue a la abadía
                if (estado == 0x00) {
                    // si guillermo está cerca del abad, le da la bienvenida y le dice que le siga
                    if (estaCerca(juego.logica.guillermo)) {
                        // muestra la frase BIENVENIDO A ESTA ABADIA, HERMANO. OS RUEGO QUE ME SIGAIS. HA SUCEDIDO ALGO TERRIBLE
                        juego.gestorFrases.muestraFrase(0x01);

                        // cambia de estado y va a por guillermo
                        estado = 0x01;
                        aDondeVa = POS_GUILLERMO;
                    } else {
                        // va a la entrada de la abadía
                        aDondeVa = 3;
                    }

                    return;
                }

                // si guillermo está cerca del abad, continúa normalmente
                if (estaCerca(juego.logica.guillermo)) {
                    // si está diciendo la primera frase
                    if (estado == 0x01) {
                        // si va a la primera parada y no se está reproduciendo ninguna voz, cambia al estado 0x02
                        if ((aDondeVa == 4) && (!juego.gestorFrases.mostrandoFrase)) {
                            estado = 0x02;
                        } else if (!juego.gestorFrases.mostrandoFrase) {
                            // si ha terminado la primera frase, dice la segunda y se va a la primera parada

                            // muestra la frase TEMO QUE UNO DE LOS MONJES HA COMETIDO UN CRIMEN. OS RUEGO QUE LO ENCONTREIS ANTES DE QUE LLEGUE BERNARDO GUI, PUES	NO DESEO QUE SE MANCHE EL NOMBRE DE ESTA ABADIA
                            juego.gestorFrases.muestraFrase(0x02);
                            aDondeVa = 4;
                        }
                    }

                    // si ya terminó la segunda frase, espera a que llegue al destino
                    if (estado == 0x02) {
                        // va a la primera parada
                        aDondeVa = 4;

                        // si ha llegado a la primera parada y no está reproduciendo una frase, pasa al estado 0x03
                        if ((aDondeHaLlegado == 4) && (!juego.gestorFrases.mostrandoFrase)) {
                            estado = 0x03;
                        }
                    }

                    // si está diciendo la tercera frase
                    if (estado == 0x03) {
                        // si va a la primera parada y no se está reproduciendo ninguna voz, cambia al estado 0x1f
                        if ((aDondeVa == 5) && (!juego.gestorFrases.mostrandoFrase)) {
                            estado = 0x1f;
                        } else if (!juego.gestorFrases.mostrandoFrase) {
                            // si ha terminado la segunda frase, dice la tercera y se va a la segunda parada

                            // muestra la frase DEBEIS RESPETAR MIS ORDENES Y LAS DE LA ABADIA. ASISTIR A LOS OFICIOS Y A LA COMIDA. DE NOCHE DEBEIS ESTAR EN VUESTRA CELDA
                            juego.gestorFrases.muestraFrase(0x03);
                            aDondeVa = 5;
                        }
                    }

                    // si ya terminó la tercera frase, espera a que llegue al destino
                    if (estado == 0x1f) {
                        // va a la segunda parada (entrada de nuestra celda)
                        aDondeVa = 5;

                        // si ha llegado a la segunda parada y no está reproduciendo una frase, pasa al estado 0x04 y se despide
                        if ((aDondeHaLlegado == 5) && (!juego.gestorFrases.mostrandoFrase)) {
                            estado = 0x04;

                            // muestra la frase ESTA ES VUESTRA CELDA, DEBO IRME
                            juego.gestorFrases.muestraFrase(0x07);
                        }
                    }

                    return;
                } else {
                    // si no está cerca de guillermo, le abronca
                    recriminaAGuillermo();

                    return;
                }
            }

            return;
        }

        if (juego.logica.dia == 2) {
            // DEBEIS SABER QUE LA BIBLIOTECA ES UN LUGAR SECRETO. SOLO MALAQUIAS PUEDE ENTRAR. PODEIS IROS
            numFrase = 0x16;

            // llama a guillermo para hablar con él
            avisaAGuillermoOPasea();

            return;
        }

        // si es el tercir día
        if (juego.logica.dia == 3) {
            // si guillermo se ha acercado a ver que quería el abad
            if ((estado == 0x10) && (juego.logica.momentoDia == TERCIA)) {
                // si guillermo está cerca, va al lugar donde está jorge
                if (estaCerca(juego.logica.guillermo)) {
                    aDondeVa = 7;
                } else {
                    // si han llegado a donde estaba jorge, reprime a guillermo y sigue por donde lo dejó
                    if (juego.logica.jorge.estado >= 0x1e) {
                        juego.logica.jorge.estado--;

                        return;
                    }
                    juego.logica.avanzarMomentoDia = false;
                    recriminaAGuillermo();
                }
            } else {
                // frase QUIERO QUE CONOZCAIS AL HOMBRE MAS VIEJO Y SABIO DE LA ABADIA
                numFrase = 0x30;

                // llama a guillermo para hablar con él
                avisaAGuillermoOPasea();
            }

            return;
        }

        // si es el cuarto día
        if (juego.logica.dia == 4) {
            // frase HA LLEGADO BERNARDO, DEBEIS ABANDONAR LA INVESTIGACION
            numFrase = 0x11;

            // llama a guillermo para hablar con él
            avisaAGuillermoOPasea();

            return;
        }

        // si es el quinto día
        if (juego.logica.dia == 5) {
            if (juego.logica.momentoDia == NONA) {
                // si ha llegado a la puerta de la celda de severino
                if (aDondeHaLlegado == 8) {
                    contador++;

                    // si se ha esperado suficiente, cambia de estado e informa de la muerte de severino
                    if (contador >= 0x1e) {
                        estado = 0x10;

                        // muestra la frase DIOS SANTO... HAN ASESINADO A SEVERINO Y LE HAN ENCERRADO
                        juego.gestorFrases.muestraFrase(0x1c);
                        juego.logica.avanzarMomentoDia = true;
                    }
                } else {
                    // si va a la celda de severino y está en el estado 0x13
                    if ((aDondeVa == 8) || (estado == 0x13)) {
                        contador = 0;

                        if (estado == 0x13) {
                            // si está cerca de guillermo, va a la celda de severino. En otro caso, le recrimina
                            if (estaCerca(juego.logica.guillermo)) {
                                aDondeVa = 8;
                            } else {
                                recriminaAGuillermo();
                            }
                        } else if (!juego.gestorFrases.mostrandoFrase) {
                            // si no se está mostrando una frase, pasa al estado 0x13
                            estado = 0x13;
                        }
                    } else {
                        // muestra la frase VENID, FRAY GUILLERMO, DEBEMOS ENCONTRAR A SEVERINO
                        juego.gestorFrases.muestraFraseYa(0x1b);

                        // va a la celda de severino
                        aDondeVa = 8;
                    }
                }
            } else {
                // frase BERNARDO ABANDONARA HOY LA ABADIA
                numFrase = 0x1d;

                // llama a guillermo para hablar con él
                avisaAGuillermoOPasea();
            }

            return;
        }

        // si es el sexto día
        if (juego.logica.dia == 6) {
            // frase MAÑANA ABANDONAREIS LA ABADIA
            numFrase = 0x1e;

            // llama a guillermo para hablar con él
            avisaAGuillermoOPasea();

            return;
        }

        // si es el séptimo día
        if (juego.logica.dia == 7) {
            // frase DEBEIS ABANDONAR YA LA ABADIA
            numFrase = 0x25;

            // en tercia del septimo día termina el juego
            if (juego.logica.momentoDia == TERCIA) {
                juego.logica.haFracasado = true;
            }

            // llama a guillermo para hablar con él
            avisaAGuillermoOPasea();

        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda
    /////////////////////////////////////////////////////////////////////////////

    // le ordena a guillermo que vaya a donde está él
    void recriminaAGuillermo() {
        // si no acaba de recriminar a guillermo, lo hace ahora
        if (estado < 0x80) {
            // descarta los movimientos que tenía pensados
            descartarMovimientosPensados();

            // muestra la frase OS ORDENO QUE VENGAIS y le decrementa la vida
            juego.gestorFrases.muestraFraseYa(0x08);
            juego.logica.decrementaObsequium(2);

            // indica que le acaba de llamar la atención
            estado = estado + 0x80;

            // en esta iteración no busca ninguna ruta
            juego.logica.buscRutas.seBuscaRuta = false;
        }
    }

    // espera a que el abad, guillermo y los personajes con IA estén en su sitio para comenzar la misa o la comida
    void esperaParaComenzarActo() {
        // si ha llegado a donde debía ir
        if (aDondeHaLlegado == aDondeVa) {
            // si los monjes están listos
            if (lleganLosMonjes == 0) {
                // si guillermo ha llegado a la pantalla del acto
                if (guillermoBienColocado >= 0x01) {
                    // si se ha superado el contador de puntualidad, lo reinicia y reprende a guillermo
                    if (contador >= 0x32) {
                        contador = 0;

                        // muestra la frase LLEGAIS TARDE, FRAY GUILLERMO
                        juego.gestorFrases.muestraFrase(0x06);
                        juego.logica.decrementaObsequium(2);
                    } else {
                        // si no se está mostrando ninguna frase
                        if (!juego.gestorFrases.mostrandoFrase) {
                            // si guillermo no está en su sitio, incrementa el contador
                            if (guillermoBienColocado == 2) {
                                contador++;

                                // si el contador pasa el límite tolerable, le indica que ocupe su sitio
                                if (contador >= 0x1e) {
                                    contador = 0;

                                    // muestra la frase OCUPAD VUESTRO SITIO, FRAY GUILLERMO
                                    juego.gestorFrases.muestraFrase(0x2d);
                                    juego.logica.decrementaObsequium(2);
                                }

                                return;
                            } else {
                                // si guillermo está en su sitio, el abad habla y se avanzará el día cuando termine de hablar
                                juego.gestorFrases.muestraFrase(numFrase);
                                juego.logica.avanzarMomentoDia = true;
                            }
                        }

                        // si se tenía que avanzar el momento del día pero guillermo se ha movido del sitio, le reprime
                        if ((juego.logica.avanzarMomentoDia == true) && (guillermoBienColocado == 2)) {
                            contador = 0;

                            juego.logica.avanzarMomentoDia = false;

                            // muestra la frase OCUPAD VUESTRO SITIO, FRAY GUILLERMO
                            juego.gestorFrases.muestraFraseYa(0x2d);
                            juego.logica.decrementaObsequium(2);
                        }
                    }
                } else {
                    // si guillermo no ha llegado a la pantalla del acto

                    // si se le ha esperado suficiente, le echa
                    if (contador >= 0xc8) {
                        estado = 0x0b;
                        juego.logica.avanzarMomentoDia = true;
                    } else {
                        contador++;
                    }
                }
            }
        } else {
            // si aun no ha llegado al destino, mantiene el contador a 0
            contador = 0;
        }
    }

    // comprueba la posición de guillermo en la iglesia
    void compruebaPosGuillermoEnIglesia() {
        compruebaPosGuillermo(0x84, 0x4b, ABAJO);
    }

    // comprueba la posición de guillermo en el refectorio
    void compruebaPosGuillermoEnRefectorio() {
        compruebaPosGuillermo(0x38, 0x39, ABAJO);
    }

    // comprueba que guillermo esté en una posición determinada (de la planta baja)
    void compruebaPosGuillermo(int posX, int posY, int orientacion) {
        // inicialmente guillermo no está bien colocado ni en la habitación de destino
        guillermoBienColocado = 0;

        // si no está en la planta baja, sale devolviendo 0
        if (juego.logica.guillermo.altura >= 0x0b) return;

        // halla la diferencia de posición en X y en Y
        int difX = juego.logica.guillermo.posX ^ posX;
        int difY = juego.logica.guillermo.posY ^ posY;
        int dif = (difX | difY);

        // si no está en la misma habitación, sale devolviendo 0
        if (dif >= 0x10) return;

        // aquí por lo menos está en la misma habitación de destino
        guillermoBienColocado = 2;

        // si no está en la posición de destino, sale devolviendo 2
        if (dif != 0) return;

        // si está en la posición de destino y con la orientación deseada, sale devolviendo 1
        if (juego.logica.guillermo.orientacion == orientacion) {
            guillermoBienColocado = 1;
        }
    }

    // comprueba que todos los personajes con IA estén en su sitio para la misa de vísperas
    void compruebaPosMonjesEnIglesiaEnVisperas(int numDia) {
        // el quinto día tiene un tratamiento especial 
        if (numDia == 5) {
            // si malaquías se está muriendo
            if (juego.logica.malaquias.estaMuerto >= 0x01) {
                // frase MALAQUIAS HA MUERTO
                numFrase = 0x20;

                // indica que todos los monjes están en su sitio
                lleganLosMonjes = 0;
            } else {
                // indica que aún no están todos en su sitio
                lleganLosMonjes = 1;
            }

            return;
        }

        // en otro caso, comprueba que los monjes que deben estar presentes hayan llegado a la iglesia
        if (monjesIglesiaEnVisperas[numDia - 1] != 0) {
            lleganLosMonjes = 0;

            // recorre los personajes y los que interesan, los combina con el resultado
            for (int i = 1; i < Juego.numPersonajes; i++) {
                if ((monjesIglesiaEnVisperas[numDia - 1] & (1 << i)) != 0) {
                    PersonajeConIA pers = (PersonajeConIA) juego.personajes[i];
                    lleganLosMonjes |= pers.aDondeHaLlegado;
                }
            }
        }
    }

    // comprueba que todos los personajes con IA estén en su sitio para la misa de prima
    void compruebaPosMonjesEnIglesiaEnPrima(int numDia) {
        // fija la frase a decir según el día que sea
        if (frasesIglesiaEnPrima[numDia - 1] != 0) {
            numFrase = frasesIglesiaEnPrima[numDia - 1];
        }

        // comprueba que los monjes que deben estar presentes hayan llegado a la iglesia y cambia la frase a mostrar
        if (monjesIglesiaEnPrima[numDia - 1] != 0) {
            lleganLosMonjes = 0;

            // recorre los personajes y los que interesan, los combina con el resultado
            for (int i = 1; i < Juego.numPersonajes; i++) {
                if ((monjesIglesiaEnPrima[numDia - 1] & (1 << i)) != 0) {
                    PersonajeConIA pers = (PersonajeConIA) juego.personajes[i];
                    lleganLosMonjes |= pers.aDondeHaLlegado;
                }
            }
        }
    }

    // comprueba que todos los personajes con IA estén en su sitio para comer
    void compruebaPosMonjesEnRefectorio(int numDia) {
        lleganLosMonjes = 1;

        // comprueba que los monjes que deben estar presentes hayan llegado a la iglesia y cambia la frase a mostrar
        if (monjesEnRefectorio[numDia - 1] != 0) {
            // recorre los personajes y los que interesan, los combina con el resultado
            for (int i = 1; i < Juego.numPersonajes; i++) {
                if ((monjesEnRefectorio[numDia - 1] & (1 << i)) != 0) {
                    PersonajeConIA pers = (PersonajeConIA) juego.personajes[i];
                    lleganLosMonjes &= pers.aDondeHaLlegado;
                }
            }

            // invierte el resultado
            lleganLosMonjes = lleganLosMonjes ^ 1;
        }
    }

    // si es tercia, habla con guillermo. En otro caso se pasea
    void avisaAGuillermoOPasea() {
        if (estado == 0x10) {
            paseaPorLaAbadia();
        } else {
            if (juego.logica.momentoDia == TERCIA) {
                diceFraseAGuillermoEnTercia();
            }
        }
    }

    // se pasea por la abadía
    void paseaPorLaAbadia() {
        // si malaquías, berengario o bernardo van a por el abad
        if ((juego.logica.malaquias.aDondeVa == POS_ABAD) || (juego.logica.berengario.aDondeVa == POS_ABAD) || (
                juego.logica.bernardo.aDondeVa == POS_ABAD)) {
            // si el abad ha llegado a donde quería ir, se queda quieto esperándoles
            if (aDondeHaLlegado == aDondeVa) {
                juego.logica.buscRutas.seBuscaRuta = false;
            } else {
                // se va a su celda
                aDondeVa = 2;

                // si bernardo tiene el pergamino, va a la entrada de la abadía a esperarle
                if ((juego.logica.bernardo.objetos & PERGAMINO) == PERGAMINO) {
                    aDondeVa = 3;
                }
            }
        } else {
            // si el abad tiene el pergamino, va a su celda a dejarlo
            if ((objetos & PERGAMINO) == PERGAMINO) {
                aDondeVa = 2;
            }

            // si ha llegado a donde quería ir, se mueve aleatoriamente
            if (aDondeHaLlegado == aDondeVa) {
                aDondeVa = (juego.logica.numeroAleatorio & 0x03) + 2;
            }
        }

    }

    // llama a guillermo y le dice una frase, para finalmente pasar al estado 0x10
    void diceFraseAGuillermoEnTercia() {
        // si acaba de terminar la misa de prima, le dice a guillermo que se acerque
        if (estado == 0x0e) {
            // muestra la frase VENID AQUI, FRAY GUILLERMO
            juego.gestorFrases.muestraFrase(0x14);
            estado = 0x11;
        }

        // si le ha dicho a guillermo que se acerque
        if (estado == 0x11) {
            // si no se está mostrando ninguna frase, pasa al estado 0x12
            if (!juego.gestorFrases.mostrandoFrase) {
                estado = 0x12;
                contador = 0;
            }
        }

        // si ha terminado la frase que le indicaba que se acercase, comienza a decir la frase que tenía pensada
        if (estado == 0x12) {
            estado = 0x0f;
            aDondeVa = 0;

            // muestra la frase que tenía almacenada
            juego.gestorFrases.muestraFrase(numFrase);

            return;
        }

        // si está esperando a que el abad termine la frase
        if (estado == 0x0f) {
            // si no se está mostrando ninguna frase, pasa al estado 0x10 y sale
            if (!juego.gestorFrases.mostrandoFrase) {
                estado = 0x10;
            } else {

                // si guillermo no está cerca del abad, se lo recrimina
                if (!estaCerca(juego.logica.guillermo)) {
                    estado = 0x12;

                    recriminaAGuillermo();
                }
            }
        }
    }
}
