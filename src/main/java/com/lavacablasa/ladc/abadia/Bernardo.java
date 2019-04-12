package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.MomentosDia.COMPLETAS;
import static com.lavacablasa.ladc.abadia.MomentosDia.NOCHE;
import static com.lavacablasa.ladc.abadia.MomentosDia.PRIMA;
import static com.lavacablasa.ladc.abadia.MomentosDia.SEXTA;
import static com.lavacablasa.ladc.abadia.MomentosDia.VISPERAS;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.PERGAMINO;
import static com.lavacablasa.ladc.abadia.Orientacion.ABAJO;
import static com.lavacablasa.ladc.abadia.Orientacion.ARRIBA;
import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;
import static com.lavacablasa.ladc.abadia.Orientacion.IZQUIERDA;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_ABAD;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_GUILLERMO;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_PERGAMINO;

class Bernardo extends Monje {

    /////////////////////////////////////////////////////////////////////////////
    // posiciones a las que puede ir el personaje según el estado
    /////////////////////////////////////////////////////////////////////////////

    private static final PosicionJuego[] posicionesPredef = {
        new PosicionJuego(ABAJO, 0x8c, 0x48, 0x02),		// posición en la iglesia
        new PosicionJuego(ARRIBA, 0x32, 0x35, 0x02),	    // posición en el refectorio
        new PosicionJuego(IZQUIERDA, 0x3d, 0x5c, 0x0f),	// posición de su mesa en el scriptorium
        new PosicionJuego(DERECHA, 0xbc, 0x15, 0x02),	    // celda de los monjes
        new PosicionJuego(ARRIBA, 0x88, 0xa8, 0x00)		// salida de la abadía
    };

    boolean estaEnLaAbadia;

    Bernardo(Juego juego, SpriteMonje sprite) {
        super(juego, sprite);

        // coloca los datos de la cara de bernardo
        datosCara[0] = 0xb293;
        datosCara[1] = 0xb293 + 0x32;

        mascarasPuertasBusqueda = 0x3f;

        // asigna las posiciones predefinidas
        posiciones = posicionesPredef;
    }

    //////////////////////////  ///////////////////////////////////////////////////
    // comportamiento
    /////////////////////////////////////////////////////////////////////////////

    // Los estados en los que puede estar bernardo son:
    //		0x00 . estado incial
    //		0x07 . estado en el que persigue a guillermo hasta quitarle el pergamino
    //		0x14 . estado en el que ya no tiene nada que hacer, por lo que tan sólo se pasea por la abadía
    void piensa()
    {
        // si bernardo no está en la abadía, sale
        if (!estaEnLaAbadia){
            juego.logica.buscRutas.seBuscaRuta = false;

            return;
        }

        // si es sexta, va al comedor
        if (juego.logica.momentoDia == SEXTA){
            aDondeVa = 1;

            return;
        }

        // si es prima, va a la iglesia
        if (juego.logica.momentoDia == PRIMA){
            aDondeVa = 0;

            return;
        }

        // al quinto día, abandona la abadía
        if (juego.logica.dia == 5){
            // si llega a la salida de las escaleras, se va de la abadía
            if (aDondeHaLlegado == 4){
                estaEnLaAbadia = false;
                posX = posY = altura = 0;
            }

            // se va de la abadía
            aDondeVa = 4;
        }

        // en completas o por la noche, se va a la celda de los monjes
        if ((juego.logica.momentoDia == COMPLETAS) || (juego.logica.momentoDia == NOCHE)){
            aDondeVa = 3;

            return;
        }

        // si es vísperas, va a la iglesia
        if (juego.logica.momentoDia == VISPERAS){
            aDondeVa = 0;

            return;
        }

        // si ya no tiene nada que hacer y ha llegado a su destino, se mueve a una posición aleatoria
        if (estado == 0x14){
            if (aDondeHaLlegado == aDondeVa){
                aDondeVa = juego.logica.numeroAleatorio & 0x03;
            }

            return;
        }

        // si es el cuarto día
        if (juego.logica.dia == 4){
            // si va a por el abad y ya le ha dado el pergamino
            if ((aDondeVa == POS_ABAD) && ((juego.logica.abad.objetos & PERGAMINO) == PERGAMINO)){
                // indica que ya no tiene nada que hacer
                estado = 0x14;

                // va al refectorio
                aDondeVa = 1;

                // cambia el estado del abad para que deje el pergamino en su celda
                juego.logica.abad.estado = 0x15;

                return;
            }
        }

        // si bernardo tiene el pergamino, va a dárselo al abad
        if ((objetos & PERGAMINO) == PERGAMINO){
            aDondeVa = POS_ABAD;

            // cambia la máscara de los objetos para no volver a coger el pergamino
            mascaraObjetos = 0;

            return;
        }

        // si el pergamino está a buen recaudo o el abad va a echar a guillermo, bernardo ya no tiene nada que hacer
        if (juego.logica.pergaminoGuardado || ((juego.logica.abad.objetos & PERGAMINO) == PERGAMINO) || (juego.logica.abad.estado == 0x0b)){
            // va al scriptorium
            aDondeVa = 2;
            estado = 0x14;

            return;
        }

        juego.logica.pergaminoGuardado = false;

        // deshabilita el contador para que avance el momento del día de forma automática
        juego.logica.duracionMomentoDia = 0;

        // si guillermo tiene el pergamino
        if ((juego.logica.guillermo.objetos & PERGAMINO) == PERGAMINO){
            // si está persiguiendo a guillermo
            if (estado == 7){
                aDondeVa = POS_GUILLERMO;

                // si está cerca de guillermo, le exige el manuscrito y decrementa su vida
                if (estaCerca(juego.logica.guillermo)){
                    if (!juego.gestorFrases.mostrandoFrase){
                        // pone en el marcador la frase DADME EL MANUSCRITO, FRAY GUILLERMO
                        juego.gestorFrases.muestraFrase(0x05);

                        juego.logica.decrementaObsequium(2);
                    }
                }
            } else if (estaCerca(juego.logica.guillermo)){
                // si está cerca de guillermo, se va a la celda de los monjes
                aDondeVa = 3;
            } else {
                // cambia al estado de seguir a guillermo
                estado = 7;
            }
        } else {
            // si guillermo no tiene el pergamino, va hacia donde esté el pergamino
            aDondeVa = POS_PERGAMINO;
        }
    }

}
