package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.MomentosDia.PRIMA;
import static com.lavacablasa.ladc.abadia.MomentosDia.SEXTA;
import static com.lavacablasa.ladc.abadia.MomentosDia.TERCIA;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.GUANTES;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LIBRO;
import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;

class Jorge extends Monje {

    /////////////////////////////////////////////////////////////////////////////
    // posiciones a las que puede ir el personaje según el estado
    /////////////////////////////////////////////////////////////////////////////

    private static final PosicionJuego[] posicionesPredef = {
            new PosicionJuego(DERECHA, 0xbc, 0x15, 0x02),    // celda de los monjes
            new PosicionJuego(DERECHA, 0x19, 0x2b, 0x1a)    // habitación iluminada de la biblioteca
    };

    boolean estaActivo;     // indica si el personaje está activo o no
    int contadorHuida;      // contador usado para la huida

    Jorge(Juego juego, SpriteMonje sprite) {
        super(juego, sprite);

        // coloca los datos de la cara de jorge
        datosCara[0] = 0xb2f7;
        datosCara[1] = 0xb2f7 + 0x32;

        mascarasPuertasBusqueda = 0x3f;

        // asigna las posiciones predefinidas
        posiciones = posicionesPredef;
    }

    /////////////////////////////////////////////////////////////////////////////
    // comportamiento
    /////////////////////////////////////////////////////////////////////////////

    // Los estados en los que puede estar jorge son:
    //		0x00 . estado normal
    //		0x0b . estado en el que se encuentra en la habitación de detrás del espejo cuando llega guillermo
    //		0x0c . estado en el que ha dejado el libro para que lo lea guillermo
    //		0x0d . estado en el que guillermo ha cogido el libro que le ha dado jorge
    //		0x0e . estado en el que adso le informa a jorge que guillermo lleva guantes
    //		0x0f . estado en el que jorge sale huyendo con el libro
    //		0x10 . estado en el que jorge llega al sitio donde se muere y se completa la investigación
    //		0x1e . estado en el que el abad está presentado a guillermo ante jorge
    //		0x1f . estado en el que jorge habla con guillermo después de que hayan sido presentados
    void piensa() {
        // si jorge no está activo, sale
        if (!estaActivo) {
            juego.logica.buscRutas.seBuscaRuta = false;

            return;
        }

        // si es el tercer día
        if (juego.logica.dia == 3) {
            // en prima, se queda quieto
            if (juego.logica.momentoDia == PRIMA) {
                juego.logica.buscRutas.seBuscaRuta = false;

                return;
            }

            // en tercia, conversa con guillermo
            if (juego.logica.momentoDia == TERCIA) {
                // no se mueve del sitio
                juego.logica.buscRutas.seBuscaRuta = false;

                // si el abad ha terminado de presentar a guillermo, cambia de estado
                if (estado == 0x1e) {
                    if (!juego.gestorFrases.mostrandoFrase) {
                        estado = 0x1f;
                    }

                    return;
                }

                // si el abad ha terminado de presentar a guillermo, éste le da la bienvenida
                if (estado == 0x1f) {
                    if (estaCerca(juego.logica.guillermo)) {
                        // pone en el marcador la frase SED BIENVENIDO, VENERABLE HERMANO; Y ESCUCHAD LO QUE OS DIGO. LAS VIAS DEL ANTICRISTO SON LENTAS Y TORTUOSAS. LLEGA CUANDO MENOS LO ESPERAS. NO DESPERDICIEIS LOS ULTIMOS DIAS
                        juego.gestorFrases.muestraFrase(0x32);

                        // al terminar la frase avanza el momento del día
                        juego.logica.avanzarMomentoDia = true;
                    }

                    return;
                }

                // si jorge está cerca de guillermo, el abad presenta a guillermo
                if (estaCerca(juego.logica.guillermo)) {
                    // pone en el marcador la frase VENERABLE JORGE, EL QUE ESTA ANTE VOS ES FRAY GUILLERMO, NUESTRO HUESPED
                    juego.gestorFrases.muestraFraseYa(0x31);

                    estado = 0x1e;
                }

                return;
            }

            // si es sexta, se va a la celda de los monjes
            if (juego.logica.momentoDia == SEXTA) {
                aDondeVa = 0;
                estado = 0;

                // si ha llegado a su celda, pasa a estar inactivo
                if (aDondeHaLlegado == 0) {
                    estaActivo = false;
                }

                return;
            }
        }

        // si es el sexto o séptimo día
        if (juego.logica.dia >= 6) {
            if (estado == 0x0b) {
                // si ha terminado de decir la frase, deja el libro para que lo coja guillermo
                if (!juego.gestorFrases.mostrandoFrase) {
                    juego.logica.dejaObjeto(this);

                    estado = 0x0c;
                }

                juego.logica.buscRutas.seBuscaRuta = false;

                return;
            }

            if (estado == 0x0c) {
                // si guillermo ha cogido el libro, le informa de que libro es y pasa al siguiente estado
                if ((juego.logica.guillermo.objetos & LIBRO) == LIBRO) {
                    // pone en el marcador la frase ES EL COENA CIPRIANI DE ARISTOTELES. AHORA COMPRENDEREIS POR QUE TENIA QUE PROTEGERLO. CADA PALABRA ESCRITA POR EL FILOSOFO HA DESTRUIDO UNA PARTE DEL SABER DE LA CRISTIANDAD. SE QUE HE ACTUADO SIGUIENDO LA VOLUNTAD DEL SEÑOR... LEEDLO, PUES, FRAY GUILLERMO. DESPUES TE LO MOSTRATE A TI MUCHACHO
                    juego.gestorFrases.muestraFrase(0x2e);

                    estado = 0x0d;
                }

                juego.logica.buscRutas.seBuscaRuta = false;

                return;

            }

            if (estado == 0x0d) {
                // si guillermo no tiene los guantes
                if ((juego.logica.guillermo.objetos & GUANTES) == 0) {
                    // si guillermo no ha muerto todavía
                    if (!juego.logica.haFracasado) {
                        // si ha salido de la pantalla de detrás del espejo o ha terminado la frase, mata a guillermo
                        if ((juego.motor.numPantalla == 0x72) || (!juego.gestorFrases.mostrandoFrase)) {
                            juego.logica.cntLeeLibroSinGuantes = 0xff;
                        } else {
                            juego.logica.cntLeeLibroSinGuantes = 0x01;
                        }
                    }

                    juego.logica.buscRutas.seBuscaRuta = false;
                } else {
                    // si guillermo tenía los guantes y ha terminado de mostrar la frase, adso le informa de que guillermo tenía puestos los guantes
                    if (!juego.gestorFrases.mostrandoFrase) {
                        // pone en el marcador la frase VENERABLE JORGE, VOIS NO PODEIS VERLO, PERO MI MAESTRO LLEVA GUANTES.  PARA SEPARAR LOS FOLIOS TENDRIA QUE HUMEDECER LOS DEDOS EN LA LENGUA, HASTA QUE HUBIERA RECIBIDO SUFICIENTE VENENO
                        juego.gestorFrases.muestraFrase(0x23);

                        estado = 0x0e;
                    }

                    juego.logica.buscRutas.seBuscaRuta = false;
                }

                return;

            }

            if (estado == 0x0e) {
                if (!juego.gestorFrases.mostrandoFrase) {
                    // inicia el contador para salir huyendo
                    contadorHuida = 0;

                    estado = 0x0f;

                    // pone en el marcador la frase FUE UNA BUENA IDEA ¿VERDAD?; PERO YA ES TARDE
                    juego.gestorFrases.muestraFrase(0x2f);

                }

                juego.logica.buscRutas.seBuscaRuta = false;

                return;
            }

            if (estado == 0x0f) {
                contadorHuida++;

                // si el contador para salir huyendo ha llegado al límite, jorge huye
                if (contadorHuida == 0x28) {
                    // oculta el área de juego
                    juego.limpiaAreaJuego(3);

                    // va hacia la habitación iluminada del laberinto
                    aDondeVa = 1;

                    // apaga la luza de la pantalla y le quita el libro a guillermo
                    juego.motor.pantallaIluminada = false;
                    juego.logica.guillermo.objetos &= 0x7f;

                    // quita el libro del marcador
                    juego.marcador.dibujaObjetos(juego.logica.guillermo.objetos, 0x80);

                    // hace desaparecer el libro
                    juego.objetos[0].seHaCogido = false;
                    juego.objetos[0].seEstaCogiendo = false;
                    juego.objetos[0].personaje = null;
                    juego.objetos[0].posX = 0;
                    juego.objetos[0].posY = 0;
                    juego.objetos[0].altura = 0;
                    juego.objetos[0].orientacion = DERECHA;

                    estado = 0x10;
                } else {
                    juego.logica.buscRutas.seBuscaRuta = false;
                }

                return;
            }

            if (estado == 0x10) {
                // si jorge ha llegado al sitio donde se muere y guillermo también, se ha completado la investigación
                if ((aDondeHaLlegado == 1) && (juego.motor.numPantalla == 0x67) && (juego.logica.guillermo.altura
                        < 0x1e)) {
                    juego.logica.haFracasado = true;
                    juego.logica.investigacionCompleta = true;
                }

                return;
            }

            // si entra a la habitación del espejo, inicia el estado de la secuencia final
            if (juego.motor.numPantalla == 0x73) {
                juego.logica.bonus |= 0x0800;

                // pone en el marcador la frase SOIS VOS, GUILERMO... PASAD, OS ESTABA ESPERANDO. TOMAD, AQUI ESTA VUESTRO PREMIO
                juego.gestorFrases.muestraFraseYa(0x21);

                estado = 0x0b;
            }

            juego.logica.buscRutas.seBuscaRuta = false;

        }
    }
}
