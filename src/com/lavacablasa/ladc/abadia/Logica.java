package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.MomentosDia.COMPLETAS;
import static com.lavacablasa.ladc.abadia.MomentosDia.NOCHE;
import static com.lavacablasa.ladc.abadia.MomentosDia.NONA;
import static com.lavacablasa.ladc.abadia.MomentosDia.SEXTA;
import static com.lavacablasa.ladc.abadia.MomentosDia.VISPERAS;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.GAFAS;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.GUANTES;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LAMPARA;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LIBRO;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LLAVE1;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LLAVE2;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LLAVE3;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.PERGAMINO;
import static com.lavacablasa.ladc.abadia.Orientacion.ABAJO;
import static com.lavacablasa.ladc.abadia.Orientacion.ARRIBA;
import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;
import static com.lavacablasa.ladc.abadia.Orientacion.IZQUIERDA;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_ABAD;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_GUILLERMO;
import static com.lavacablasa.ladc.abadia.PosicionesPredefinidas.POS_LIBRO;

import com.lavacablasa.ladc.core.Input;
import com.lavacablasa.ladc.core.Promise;
import java.util.Random;

class MomentosDia {
    static final int NOCHE = 0;
    static final int PRIMA = 1;
    static final int TERCIA = 2;
    static final int SEXTA = 3;
    static final int NONA = 4;
    static final int VISPERAS = 5;
    static final int COMPLETAS = 6;
}

// objetos del juego
class ObjetosJuego {
    static final int LIBRO = 0x80;
    static final int GUANTES = 0x40;
    static final int GAFAS = 0x20;
    static final int PERGAMINO = 0x10;
    static final int LLAVE1 = 0x08;
    static final int LLAVE2 = 0x04;
    static final int LLAVE3 = 0x02;
    static final int LAMPARA = 0x01;
}

class Logica {

    private static final String[] tablaNumerosRomanos = { "IXX", "XIX", "XXI" };

    static final int[][] duracionEtapasDia = {
            { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 },
            { 0x00, 0x00, 0x05, 0x00, 0x05, 0x00, 0x00 },
            { 0x00, 0x00, 0x05, 0x00, 0x05, 0x00, 0x00 },
            { 0x0f, 0x00, 0x00, 0x00, 0x05, 0x00, 0x00 },
            { 0x0f, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00 },
            { 0x0f, 0x00, 0x05, 0x00, 0x05, 0x00, 0x00 },
            { 0x0f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 }
    };

    Juego juego;

    AccionesDia accionesDia;        // ejecutor de las acciones dependiendo del momento del día
    BuscadorRutas buscRutas;        // buscador y generador de rutas

    Guillermo guillermo;            // guillermo
    Adso adso;                      // adso
    Malaquias malaquias;            // malaquias
    Abad abad;                      // el abad
    Berengario berengario;          // berengario
    Severino severino;              // severino
    Jorge jorge;                    // jorge
    Bernardo bernardo;              // bernardo gui

    int dia;                        // dia actual
    int momentoDia;                 // momento del día
    int duracionMomentoDia;         // indica lo que falta para pasar al siguiente momento del día
    int oldMomentoDia;              // indica el momento del día de las últimas acciones programadas ejecutadas
    boolean avanzarMomentoDia;      // indica si debe avanzar el momento del día
    int obsequium;                  // nivel de obsequium (de 0 a 31)
    boolean haFracasado;            // indica si guillermo ha fracasado en la investigación
    boolean investigacionCompleta;  // indica si se ha completado la investigación
    int bonus;                      // bonus que se han conseguido

    int mascaraPuertas;             // máscara de las puertas que pueden abrirse

    boolean espejoCerrado;          // indica si el espejo está cerrado o se ha abierto
    int numeroRomano;               // indica el número romano de la habitación del espejo (en el caso de que se haya generado)
    int despDatosAlturaEspejo;      // desplazamiento hasta el final de los datos de altura de la habitación del espejo
    int despBloqueEspejo;           // desplazamiento hasta los datos del bloque que forma el espejo

    boolean seAcabaLaNoche;         // indica si falta poco para que se termine la noche
    boolean haAmanecido;            // indica si ya ha amanecido
    boolean usandoLampara;          // indica si se está usando la lámpara
    boolean lamparaDesaparecida;    // indica si ha desaparecido la lámpara
    int tiempoUsoLampara;           // contador del tiempo de uso de la lámpara
    int cambioEstadoLampara;        // indica un cambio en el estado de la lámpara
    int cntTiempoAOscuras;          // contador del tiempo que pueden ir a oscuras por la biblioteca

    int cntLeeLibroSinGuantes;      // contador para llevar un control del tiempo que lee guillermo el libro sin los guantes
    boolean pergaminoGuardado;      // indica que el pergamino lo tiene el abad en su habitación o está detrás de la habitación del espejo

    int numeroAleatorio;            // número aleatorio

    boolean hayMovimiento;          // cuando hay algún movimiento de un personaje, esto se pone a true
    int cntMovimiento;              // contador que se pone a 0 con cada movimiento de guillermo (usado para cambios de cámara)

    int numPersonajeCamara;         // indica el personaje al que sigue la cámara actualmente
    int opcionPersonajeCamara;      // indica el personaje al que podría seguir la cámara si no hay movimiento

    public Logica(Juego juego, byte[] buffer) {
        this.juego = juego;
        // crea los objetos usados por la lógica
        accionesDia = new AccionesDia(juego);
        buscRutas = new BuscadorRutas(juego, new int[2048]);
    }

    // inicia la lógica
    void inicia() {
        // inicia las entidades del juego
        iniciaSprites();
        iniciaPersonajes();
        iniciaPuertas();
        iniciaObjetos();

        // inicia la lógica relacionada con la habitación del espejo
        iniciaHabitacionEspejo();

        // inicia las variables de la lógica del juego
        dia = 1;
        momentoDia = NONA;
        duracionMomentoDia = 0;
        oldMomentoDia = 0;
        avanzarMomentoDia = false;
        obsequium = 31;
        haFracasado = false;
        investigacionCompleta = false;
        bonus = 0;

        mascaraPuertas = 0xef;

        seAcabaLaNoche = false;
        haAmanecido = false;
        usandoLampara = false;
        lamparaDesaparecida = true;
        tiempoUsoLampara = 0;
        cambioEstadoLampara = 0;
        cntTiempoAOscuras = 0;

        cntLeeLibroSinGuantes = 0;
        pergaminoGuardado = false;

        hayMovimiento = false;
        cntMovimiento = 0;

        // inicialmente la cámara sigue a guillermo
        numPersonajeCamara = 0;
        opcionPersonajeCamara = 0;
        juego.motor.personaje = guillermo;

        buscRutas.generadoCamino = false;
    }

    void decrementaObsequium(int unidades) {
        obsequium = obsequium - unidades;

        // si se ha terminado el obsequium
        if (obsequium < 0) {
            // si guillermo no ha muerto, cambia el estado del abad para que le eche de la abadía
            if (!haFracasado) {
                abad.estado = 0x0b;
            }

            obsequium = 0;
        }

        // dibuja la parte de la barra correspondiente a la vida que tenemos
        juego.marcador.dibujaObsequium(obsequium);
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con el libro
    /////////////////////////////////////////////////////////////////////////////

    void compruebaLecturaLibro() {
        // si guillermo no tiene el libro, sale
        if ((guillermo.objetos & LIBRO) == 0) return;

        // si guillermo tiene los guantes, sale
        if ((guillermo.objetos & GUANTES) != 0) return;

        cntLeeLibroSinGuantes++;

        // si guillermo ha leido un poco del libro sin los guantes, muere
        if ((cntLeeLibroSinGuantes & 0xff) == 0) {

            // modifica el estado de fray guillermo para que suba al morir
            guillermo.estado = guillermo.sprite.posYPant / 2;
            guillermo.incrPosY = -2;

            haFracasado = true;

            // escribe en el marcador la frase: ESTAIS MUERTO, FRAY GUILLERMO, HABEIS CAIDO EN LA TRAMPA
            juego.gestorFrases.muestraFrase(0x22);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con los bonus y los cambios de cámara
    /////////////////////////////////////////////////////////////////////////////

    void actualizaBonusYCamara() {
        // comprueba si hay que seguir a berengario
        if (((berengario.aDondeVa == POS_LIBRO) && (berengario.posX < 0x50) && (berengario.estaVivo)) || (
                berengario.aDondeVa == POS_ABAD)) {
            // si va al scriptorium a por el libro o va a avisar al abad, indica el posible cambio de cámara
            opcionPersonajeCamara = 4;

            return;
        }

        // comprueba si hay que seguir a bernardo gui
        if (bernardo.aDondeVa == POS_ABAD) {
            // si va a avisar al abad, indica el posible cambio de cámara
            opcionPersonajeCamara = 7;

            return;
        }

        // comprueba si hay que seguir al abad
        if (((momentoDia == SEXTA) && (abad.aDondeHaLlegado >= 2)) || (abad.estado == 0x15)
                || (abad.guillermoHaCogidoElPergamino) || (abad.estado == 0x0b)) {
            // si en sexta va a algún lugar interesante o si va a dejar el pergamino a su celda o si berengario le ha dicho
            // que bernardo tiene el pergamino o si está en estado de echar a guillermo, indica el posible cambio de cámara
            opcionPersonajeCamara = 3;

            return;
        }

        // comprueba si hay que seguir a malaquías
        if ((malaquias.aDondeVa == POS_ABAD) || ((momentoDia == VISPERAS) && (malaquias.estado < 0x06))) {
            // si va a avisar al abad o es vísperas y no ha llegado a la cocina, indica el posible cambio de cámara
            opcionPersonajeCamara = 2;

            return;
        }

        // comprueba si hay que seguir a severino
        if (severino.aDondeVa == POS_GUILLERMO) {
            // si va hacia la posición de guillermo, indica el posible cambio de cámara
            opcionPersonajeCamara = 5;

            return;
        }

        // en otro caso, la cámara sigue a guillermo
        opcionPersonajeCamara = 0;

        // actualiza los bonus dependiendo de si guillermo y adso tienen los objetos que dan bonus
        bonus |= (guillermo.objetos & (GUANTES | LLAVE1 | LLAVE2)) | (adso.objetos & LLAVE3);

        // si guillermo tiene el pergamino
        if ((guillermo.objetos & PERGAMINO) == PERGAMINO) {
            // si es la noche del tercer día
            if ((dia == 3) && (momentoDia == NOCHE)) {
                bonus |= 0x1000;
            }

            // si tiene las gafas
            if ((guillermo.objetos & GAFAS) == GAFAS) {
                bonus |= 0x0100;
            }

            // si guillermo entra en la habitación del abad
            if ((juego.motor.numPantalla == 0x0d) && (numPersonajeCamara == 0)) {
                bonus |= 0x2000;
            }
        }

        // si guillermo visita el ala izquierda por la noche
        if ((momentoDia == NOCHE) && (guillermo.posX < 0x60)) {
            bonus |= 0x0001;
        }

        // si guillermo está en la biblioteca
        if (guillermo.altura >= 0x16) {
            // si tiene las gafas
            if ((guillermo.objetos & GAFAS) == GAFAS) {
                bonus |= 0x0080;
            }

            // si adso ha cogido la lámpara
            if ((adso.objetos & LAMPARA) == LAMPARA) {
                bonus |= 0x0020;
            }

            bonus |= 0x0010;
        }

        // si ha entrado en la habitación que hay detrás del espejo
        if (juego.motor.numPantalla == 0x72) {
            bonus |= 0x0200;
        }
    }

    void compruebaBonusYCambiosDeCamara() {
        // comprueba si hay opción de seguir a algún monje y actualiza los bonus
        actualizaBonusYCamara();

        boolean teclaPulsada = false;

        // si estamos en la conversación con jorge sobre el libro, la cámara sigue a jorge
        if (((guillermo.objetos & GUANTES) == GUANTES) && ((jorge.estado == 0x0d) || (jorge.estado == 0x0e) || (
                jorge.estado == 0x0f))) {
            cntMovimiento = 0x32;
            opcionPersonajeCamara = 6;
        } else {
            // comprueba si se está moviendo guillermo
            if ((juego.controles.estaSiendoPulsado(Input.UP)) || (juego.controles.estaSiendoPulsado(Input.LEFT))
                    || (juego.controles.estaSiendoPulsado(Input.RIGHT))) {
                teclaPulsada = true;
            }
        }

        // si no se pulsa ninguna tecla y el contador llega al umbral, comprueba los cambios de cámara
        if (!teclaPulsada) {
            cntMovimiento++;

            // si no se ha llegado al límite, sale
            if (cntMovimiento < 0x32) {
                return;
            }

            // si hay la opción de seguir a un personaje distinto, cambia de cámara
            if (numPersonajeCamara != opcionPersonajeCamara) {
                numPersonajeCamara = opcionPersonajeCamara;
                cntMovimiento = opcionPersonajeCamara;
            }
        } else {
            // en otro caso, la cámara sigue a guillermo
            numPersonajeCamara = 0;
            cntMovimiento = 0;
        }

        // fija el personaje al que sigue la cámara
        juego.motor.personaje = juego.personajes[numPersonajeCamara & 0x7f];
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos para coger/dejar objetos
    /////////////////////////////////////////////////////////////////////////////

    // comprueba los objetos que pueden coger los personajes
    void compruebaCogerObjetos() {
        // para cada personaje
        for (int i = 0; i < Juego.numPersonajes; i++) {
            Personaje pers = juego.personajes[i];

            // si el personaje está cogiendo o dejando un objeto, pasa al siguiente personaje
            pers.contadorObjetos--;
            if (pers.contadorObjetos != -1) return;
            pers.contadorObjetos++;

            // elimina de la máscara de los objetos que podemos coger los que ya tenemos
            int objetosACoger = (pers.mascaraObjetos ^ pers.objetos) & pers.mascaraObjetos;

            int mascara = 1 << Juego.numObjetos;

            // recorre los objetos que se pueden coger
            for (int j = 0; j < Juego.numObjetos; j++) {
                mascara = mascara >> 1;

                // si no hay que comprobar el objeto actual, pasa al siguiente
                if ((objetosACoger & mascara) == 0) continue;

                // si el personaje ha cogido el objeto, pasa al siguiente personaje
                if (juego.objetos[j].seHaCogidoPor(pers, mascara)) break;
            }
        }
    }

    // comprueba si el personaje que se le pasa puede dejar un objeto
    void dejaObjeto(Personaje pers) {
        int[] posicion = new int[3];

        // comprueba si el personaje puede dejar un objeto
        int numObj = pers.puedeDejarObjeto(posicion);

        // si el personaje puede dejar un objeto, lo hace
        if (numObj != -1) {
            juego.objetos[numObj]
                    .dejar(pers, 1 << (Juego.numObjetos - 1 - numObj), posicion[0], posicion[1], posicion[2]);
        }
    }

    void compruebaCogerDejarObjetos() {
        // guarda una copia de los objetos que tenemos
        int objetosGuillermo = guillermo.objetos;
        int objetosAdso = adso.objetos;

        // comprueba si los personajes cogen algún objeto
        compruebaCogerObjetos();

        // comprueba si los personajes dejan algún objeto
        // si se pulsa el espacio, deja un objeto (si tiene)
        if (juego.controles.estaSiendoPulsado(Input.BUTTON)) {
            dejaObjeto(guillermo);
        }

        // actualiza las puertas a las que pueden entrar guillermo y adso
        guillermo.permisosPuertas =
                (guillermo.permisosPuertas) | ((guillermo.objetos & LLAVE1) >> 3) | (guillermo.objetos & LLAVE2);
        adso.permisosPuertas = (adso.permisosPuertas & 0xef) | ((adso.objetos & LLAVE3) << 3);

        // calcula los objetos que se han cambiado
        int difObjetos = guillermo.objetos ^ objetosGuillermo;

        // si ha cambiado el estado de las gafas o el pergamino, y si tenemos los 2 objetos, comprueba 
        // si hay que generar el número del espejo y muestra el texto del pergamino
        if ((difObjetos & (PERGAMINO | GAFAS)) != 0) {
            if ((guillermo.objetos & (PERGAMINO | GAFAS)) == (PERGAMINO | GAFAS)) {
                generaNumeroRomano();
            }
        }

        // si han cambiado los objetos de guillermo, actualiza el marcador
        if (objetosGuillermo != guillermo.objetos) {
            juego.marcador.dibujaObjetos(guillermo.objetos, difObjetos);
        }

        // recorre los objetos indicando que ya no se están cogiendo
        for (int i = 0; i < Juego.numObjetos; i++) {
            juego.objetos[i].seEstaCogiendo = false;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con las puertas
    /////////////////////////////////////////////////////////////////////////////

    void compruebaAbrirCerrarPuertas() {
        for (int i = 0; i < Juego.numPuertas; i++) {
            Puerta puerta = juego.puertas[i];

            // inicialmente no hay que redibujar la puerta
            puerta.hayQueRedibujar = false;

            // comprueba si hay que abrir o cerrar la puerta
            puerta.compruebaAbrirCerrar(juego.personajes);

            // actualiza la posición del sprite según la cámara
            if (!juego.motor.actualizaCoordCamara(puerta)) {
                puerta.sprite.esVisible = false;
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // acciones programadas
    /////////////////////////////////////////////////////////////////////////////

    Promise<Void> ejecutaAccionesMomentoDia() {
        // obtiene el estado actualizado del gestor de frases
        juego.gestorFrases.actualizaEstado();

        // si el personaje que muestra la cámara está en medio de una animación, sale
        if ((juego.motor.personaje.contadorAnimacion & 0x01) != 0) return Promise.done();

        if (!avanzarMomentoDia) return accionesDia.ejecutaAccionesProgramadas();

        // si está mostrando una frase, sale
        if (juego.gestorFrases.mostrandoFrase) return Promise.done();

        // si ha cambiado el momento del día, ejecuta unas acciones dependiendo del momento del día
        avanzarMomentoDia = false;
        avanzaMomentoDia();
        return accionesDia.ejecutaAccionesProgramadas();
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con el tiempo
    /////////////////////////////////////////////////////////////////////////////

    // comprueba si se ha agotado la lámpara
    void compruebaFinLampara() {
        // si adso no tiene la lámpara, sale
        if ((adso.objetos & LAMPARA) == 0) return;

        // si no se está usando la lámpara, sale
        if (!usandoLampara) return;

        // si está en una pantalla iluminada, sale
        if (juego.motor.pantallaIluminada) return;

        // si llega aquí es porque se está usando la lámpara
        tiempoUsoLampara++;

        // cada 0x100 veces, comprueba el estado
        if ((tiempoUsoLampara & 0xff) != 0) return;

        // si no se ha procesado todavía el último cambio en el estado de la lámpara, sale
        if (cambioEstadoLampara != 0) return;

        if (((tiempoUsoLampara >> 8) & 0xff) == 3) {
            // si el tiempo de uso de la lámpara llega a 0x300, indica que se está agotando la lámpara
            cambioEstadoLampara = 1;
        } else if (((tiempoUsoLampara >> 8) & 0xff) == 6) {
            // si el tiempo de uso de la lámpara llega a 0x600, indica que se ha agotado la lámpara
            cambioEstadoLampara = 2;
        }
    }

    // comprueba si se está acabando la noche
    void compruebaFinNoche() {
        seAcabaLaNoche = false;

        // si esta etapa del día no tiene una duración programada, sale
        if (duracionMomentoDia == 0) return;

        // cada 0x100 veces, comprueba si se está acabando la noche
        if (((duracionMomentoDia & 0xff) == 0) && (momentoDia == NOCHE)) {
            if (((duracionMomentoDia >> 8) & 0xff) == 2) {
                seAcabaLaNoche = true;
            } else {
                if (((duracionMomentoDia >> 8) & 0xff) == 0) {
                    haAmanecido = true;
                }
            }
        }
    }

    // actualiza las variables relacionadas con el paso del tiempo
    void actualizaVariablesDeTiempo() {
        // comprueba si hay que pasar al siguiente momento del día
        compruebaFinMomentoDia();

        // comprueba si se ha agotado la lámpara
        compruebaFinLampara();

        // comprueba si se está acabando la noche
        compruebaFinNoche();
    }

    // comprueba si hay que pasar al siguiente momento del día
    void compruebaFinMomentoDia() {
        // si esta etapa del día tiene una duración programada, comprueba si ha terminado
        if (duracionMomentoDia != 0) {
            duracionMomentoDia--;
            if (duracionMomentoDia == 0) avanzaMomentoDia();
        }
    }

    // calcula el porcentaje de misión completada. Si se ha completado el juego, muestra el final
    Promise<Integer> calculaPorcentajeMision() {
        if (!investigacionCompleta) {
            // asigna un porcentaje según el tiempo que haya pasado de misión
            int porc = 7 * (dia - 1) + momentoDia;

            // modifica el porcentaje según los bonus obtenidos
            for (int i = 0; i < 16; i++) if ((bonus & (1 << i)) != 0) porc += 4;

            // si no hemos obtenido un porcentaje >= 5%, pone el porcentaje a 0
            if (porc < 5) porc = 0;

            return Promise.of(porc);
        } else {
            // si se ha completado la investigación, muestra el pergamino del final
            return juego.muestraFinal().map(n -> 0);
        }
    }

    void reiniciaContadoresLampara() {
        // si malaquías no tiene la lámpara y no se ha usado, sale
        if (((malaquias.objetos & LAMPARA) == 0) && (tiempoUsoLampara == 0)) return;

        // pone a 0 el tiempo de uso de la lámpara e indica que no se está usando
        tiempoUsoLampara = 0;
        usandoLampara = false;

        // se asegura de que ni adso ni malaquías tengan la lámpara
        adso.objetos = adso.objetos & ~LAMPARA;
        malaquias.objetos = malaquias.objetos & ~LAMPARA;

        // desaparece la lámpara
        lamparaDesaparecida = true;
        juego.objetos[7].seHaCogido = false;
        juego.objetos[7].seEstaCogiendo = false;
        juego.objetos[7].personaje = null;
        juego.objetos[7].posX = 0;
        juego.objetos[7].posY = 0;
        juego.objetos[7].altura = 0;
        juego.objetos[7].orientacion = DERECHA;
    }

/////////////////////////////////////////////////////////////////////////////
// métodos relacionados con la habitación del espejo
/////////////////////////////////////////////////////////////////////////////

    // si el espejo está cerrado, actualiza los sprites de los reflejos de adso y guillermo
    void realizaReflejoEspejo() {
        if (espejoCerrado) {
            if (!reflejaPersonaje(guillermo, juego.sprites[Juego.spritesReflejos])) {
                juego.sprites[Juego.spritesReflejos].esVisible = false;
            }
            if (!reflejaPersonaje(adso, juego.sprites[Juego.spritesReflejos + 1])) {
                juego.sprites[Juego.spritesReflejos + 1].esVisible = false;
            }
        } else {
            juego.sprites[Juego.spritesReflejos].esVisible = false;
            juego.sprites[Juego.spritesReflejos + 1].esVisible = false;
        }
    }

    // comprueba si un personaje está enfrente del espejo, y si es así, rellena el sprite con su reflejo
    boolean reflejaPersonaje(Personaje pers, Sprite spr) {
        // si no se está en la habitación del espejo, sale
        if (juego.motor.rejilla.minPosX != 0x1c) return false;
        if (juego.motor.rejilla.minPosY != 0x5c) return false;
        if (juego.motor.obtenerAlturaBasePlanta(pers.altura) != 0x16) return false;

        // si el personaje no está a una altura donde puede reflejarse en el espejo, sale
        if ((pers.altura - juego.motor.obtenerAlturaBasePlanta(pers.altura)) >= 8) return false;

        // si el personaje no está en frente del espejo, sale
        int posX = pers.posX - 0x20;
        if ((posX < 0) || (posX >= 10)) return false;
        int posY = pers.posY - 0x62;
        if ((posY < 0) || (posY >= 10)) return false;

        // guarda los valores del personaje que se modificarán
        int orientacion = pers.orientacion;
        int contadorAnimacion = pers.contadorAnimacion;
        int oldPosX = pers.posX;

        // refleja el personaje con respecto al espejo
        pers.contadorAnimacion = contadorAnimacion ^ 2;
        pers.posX = 0x21 - (oldPosX - 0x21);
        if ((orientacion & 0x01) == 0) {
            pers.orientacion = orientacion ^ 2;
        }
        if (pers.enDesnivel) {
            pers.posX--;
        }

        // actualiza los valores del sprite
        spr.oldPosXPant = spr.posXPant;
        spr.oldPosYPant = spr.posYPant;
        spr.oldAlto = spr.alto;
        spr.oldAncho = spr.ancho;

        // calcula los valores del sprite reflejado
        Sprite sprTemp = pers.sprite;
        pers.sprite = spr;
        pers.actualizaSprite();
        pers.sprite = sprTemp;

        // restaura los valores del personaje
        pers.orientacion = orientacion;
        pers.contadorAnimacion = contadorAnimacion;
        pers.posX = oldPosX;

        return true;
    }

    // comprueba si se está delante del espejo y si se ha pulsado la Q y la R en alguna de las escaleras
    int pulsadoQR() {
        // si no está delante del espejo, sale
        if ((guillermo.posX != 0x22) || (guillermo.altura != 0x1a)) {
            return 0;
        }

        // si no se ha pulsado la Q y la R, sale
        if (!juego.controles.estaSiendoPulsado(Input.Q) || !juego.controles.estaSiendoPulsado(Input.R)) {
            return 0;
        }

        // comprueba si se ha pulsado la Q y la R en una de las escaleras
        switch (guillermo.posY) {
            case 0x6d:    // si está en la escalera de la izquierda, sale devolviendo 1
                return 1;
            case 0x69:    // si está en la escalera del centro, sale devolviendo 2
                return 2;
            case 0x65:    // si está en la escalera de la derecha, sale devolviendo 3
                return 3;
            default:    // en otro caso, devuelve 0
                return 0;
        }
    }

    // comprueba si se ha pulsado QR en la habitación del espejo y actúa en consecuencia
    void compruebaAbreEspejo() {
        // si se ha abierto el espejo, sale
        if (!espejoCerrado) return;

        // comprueba si se está delante del espejo y si se ha pulsado la Q y la R en alguna de las escaleras
        int estadoQR = pulsadoQR();

        // si no se ha pulsado QR en alguna escalera del espejo, sale
        if (estadoQR == 0) return;

        // marca como conseguido el bonus de abrir el espejo
        bonus |= 0x0400;

        // si pulsó QR en el lugar correcto
        if (estadoQR == numeroRomano) {
            // modifica los datos de altura de la habitación del espejo para que guillermo puede atravesarlo
            juego.gameData(despDatosAlturaEspejo, 0xff);

            // cambia los datos de un bloque de la habitación del espejo para que el espejo esté abierto
            juego.gameData(despBloqueEspejo, 0x51);
        } else {
            // en otro caso, cambia el estado de guillermo y lo mata
            guillermo.estado = 0x14;
            haFracasado = true;

            // cambia los datos de un bloque de la habitación del espejo para que se abra una trampa y se caiga guillermo
            juego.gameData(despBloqueEspejo - 2, 0x6b);

            // escribe en el marcador la frase: ESTAIS MUERTO, FRAY GUILLERMO, HABEIS CAIDO EN LA TRAMPA
            juego.gestorFrases.muestraFrase(0x22);
        }

        // indica que se ha abierto el espejo y hay que la pantalla ha cambiado
        juego.motor.posXPantalla = juego.motor.posYPantalla = -1;
        espejoCerrado = false;
    }

    // si no se había generado el número romano para el enigma de la habitación del espejo, lo genera
    void generaNumeroRomano() {
        if (numeroRomano == 0) {
            // genera un número aleatorio entre 1 y 3
            numeroRomano = new Random().nextInt(4);
            if (numeroRomano == 0) {
                numeroRomano = 1;
            }

            // copia el número romano a la frase que se muestra al leer el manuscrito
            for (int i = 0; i < 3; i++) {
                StringBuilder str = new StringBuilder(GestorFrases.frases[0]);
                str.setCharAt(36 + i, tablaNumerosRomanos[numeroRomano - 1].charAt(i));
                GestorFrases.frases[0] = str.toString();
            }
        }

        // escribe en el marcador la frase: SECRETUM FINIS AFRICAE, MANUS SUPRA XXX AGE PRIMUM ET SEPTIMUM DE QUATUOR
        juego.gestorFrases.muestraFrase(0x00);
    }

    /////////////////////////////////////////////////////////////////////////////
    // inicialización de la habitación del espejo
    /////////////////////////////////////////////////////////////////////////////

    // obtiene el desplazamiento hasta los datos de la habitación del espejo
    void despHabitacionEspejo() {
        // apunta a datos de altura de la segunda planta de la abadía
        int desp = 0x18000 + 0x1056;

        // busca el fín de la tabla
        while (juego.gameData(desp) != 0xff) {
            desp = ((juego.gameData(desp) & 0x08) == 0x08) ? desp + 5 : desp + 4;
        }

        // guarda la dirección para luego
        despDatosAlturaEspejo = desp;

        // apunta al inicio de los datos de los bloques que forman las pantallas
        desp = 0x1c000;

        // avanza hasta la habitación del espejo
        for (int i = 0; i < 0x72; i++) {
            desp = desp + (juego.gameData(desp));
        }

        despBloqueEspejo = 0;

        // recorre los datos que forman la habitación del espejo buscando el bloque del espejo
        for (int i = 0; i < 0x100; i++) {
            if (juego.gameData(desp) == 0x1f) {
                // si encuentra el bloque que forma el espejo y está abierto
                if ((juego.gameData(desp + 1) == 0xaa) && (juego.gameData(desp + 2) == 0x51)) {
                    desp = desp + 2;

                    // modifica el bloque para que el espejo se muestre cerrado
                    juego.gameData(desp, 0x11);

                    // guarda el desplazamiento al bloque para después
                    despBloqueEspejo = desp;
                    break;
                }
            }

            desp++;
        }
    }

    // fija el estado inicial de la habitación del espejo
    void iniciaHabitacionEspejo() {
        // inicialmente, el espejo está cerrado y no se ha generado el número romano para el enigma del espejo
        espejoCerrado = true;
        numeroRomano = 0;

        int datosAltura[] = { 0xf5, 0x20, 0x62, 0x0b, 0xff };

        // modifica los datos de altura de la habitación del espejo
        for (int i = 0; i < 5; i++) {
            juego.gameData(despDatosAlturaEspejo + i, datosAltura[i]);
        }

        // modifica la habitación del espejo para que el espejo aparezca cerrado
        juego.gameData(despBloqueEspejo, 0x11);

        // modifica la habitación del espejo para que la trampa no esté abierta
        juego.gameData(despBloqueEspejo - 2, 0x1f);
    }

    /////////////////////////////////////////////////////////////////////////////
    // inicialización de las entidades del juego
    /////////////////////////////////////////////////////////////////////////////

    // inicia los sprites del juego poniéndolos como no visibles
    void iniciaSprites() {
        for (int i = 0; i < Juego.numSprites; i++) {
            juego.sprites[i].esVisible = false;
        }
    }

    // inicia los personajes del juego
    void iniciaPersonajes() {
        // obtiene los personajes del juego
        guillermo = (Guillermo) juego.personajes[0];
        adso = (Adso) juego.personajes[1];
        malaquias = (Malaquias) juego.personajes[2];
        abad = (Abad) juego.personajes[3];
        berengario = (Berengario) juego.personajes[4];
        severino = (Severino) juego.personajes[5];
        jorge = (Jorge) juego.personajes[6];
        bernardo = (Bernardo) juego.personajes[7];

        // recorre los personajes e inicia sus características comunes
        for (int i = 0; i < Juego.numPersonajes; i++) {
            Personaje pers = juego.personajes[i];
            pers.contadorAnimacion = 0;
            pers.orientacion = DERECHA;
            pers.bajando = false;
            pers.giradoEnDesnivel = false;
            pers.enDesnivel = false;

            if (pers == guillermo) continue;

            PersonajeConIA persIA = (PersonajeConIA) juego.personajes[i];
            persIA.numBitAcciones = 0;
            persIA.pensarNuevoMovimiento = false;
            persIA.posAcciones = 0;
            persIA.bufAcciones[0] = 0x10;    // acción para que piense un nuevo movimiento
        }

        // guillermo
        guillermo.objetos = GAFAS;
        guillermo.mascaraObjetos = LIBRO | GUANTES | GAFAS | PERGAMINO | LLAVE1 | LLAVE2;
        guillermo.permisosPuertas = 0x08;
        guillermo.posX = 0x88;
        guillermo.posY = 0xa8;
        guillermo.altura = 0x00;
        guillermo.estado = 0x00;
        guillermo.incrPosY = 2;

        // adso
        adso.valorPosicion = 0x20;
        adso.mascaraObjetos = LLAVE3 | LAMPARA;
        adso.permisosPuertas = 0x08;
        adso.posX = guillermo.posX - 2;
        adso.posY = guillermo.posY + 2;
        adso.altura = guillermo.altura;
        adso.estado = 0;
        adso.aDondeHaLlegado = -1;
        adso.oldEstado = 0;
        adso.movimientosFrustados = 0;
        adso.cntParaDormir = 0;

        // malaquías
        malaquias.posX = 0x26;
        malaquias.posY = 0x26;
        malaquias.altura = 0x0f;
        malaquias.mascaraObjetos = LLAVE3 | LAMPARA;
        malaquias.permisosPuertas = 0x1f;
        malaquias.estado = 0;
        malaquias.aDondeHaLlegado = -6;
        malaquias.estado2 = 0;
        malaquias.estaMuerto = 0;

        // el abad
        abad.posX = 0x88;
        abad.posY = 0x84;
        abad.altura = 0x02;
        abad.mascaraObjetos = PERGAMINO;
        abad.permisosPuertas = 0x19;
        abad.puedeQuitarObjetos = true;
        abad.estado = 0;
        abad.aDondeHaLlegado = -6;
        abad.guillermoHaCogidoElPergamino = false;
        abad.contador = 0;
        abad.numFrase = 0;

        // berengario
        berengario.posX = 0x28;
        berengario.posY = 0x48;
        berengario.altura = 0x0f;
        berengario.mascaraObjetos = 0x00;
        berengario.permisosPuertas = 0x1f;
        berengario.fijaCapucha(false);
        berengario.estado = 0;
        berengario.aDondeHaLlegado = -6;
        berengario.estado2 = 0;
        berengario.estaVivo = true;
        berengario.contadorPergamino = 0;

        // severino
        severino.posX = 0xc8;
        severino.posY = 0x28;
        severino.altura = 0x00;
        severino.mascaraObjetos = 0x00;
        severino.permisosPuertas = 0x0c;
        severino.estado = 0;
        severino.aDondeHaLlegado = -6;
        severino.estaVivo = true;

        // jorge
        jorge.posX = 0x00;
        jorge.posY = 0x00;
        jorge.altura = 0x00;
        jorge.mascaraObjetos = 0x00;
        jorge.permisosPuertas = 0x1f;
        jorge.estado = 0;
        jorge.aDondeHaLlegado = -6;
        jorge.estaActivo = false;
        jorge.contadorHuida = 0;

        // bernardo gui
        bernardo.posX = 0x00;
        bernardo.posY = 0x00;
        bernardo.altura = 0x00;
        bernardo.permisosPuertas = 0x1f;
        bernardo.puedeQuitarObjetos = true;
        bernardo.estado = 0;
        bernardo.aDondeHaLlegado = -6;
        bernardo.estaEnLaAbadia = false;
    }

    // inicia las puertas del juego
    void iniciaPuertas() {
        Puerta[] puertas = juego.puertas;

        // puerta de la habitación del abad
        puertas[0].identificador = 0x01;
        puertas[0].orientacion = ABAJO;
        puertas[0].posX = 0x61;
        puertas[0].posY = 0x37;
        puertas[0].altura = 0x02;
        puertas[0].haciaDentro = true;

        // puerta de la habitación de los monjes
        puertas[1].identificador = 0x02;
        puertas[1].orientacion = IZQUIERDA;
        puertas[1].posX = 0xb7;
        puertas[1].posY = 0x1e;
        puertas[1].altura = 0x02;
        puertas[1].haciaDentro = true;

        // puerta de la habitación de severino
        puertas[2].identificador = 0x04;
        puertas[2].orientacion = DERECHA;
        puertas[2].posX = 0x66;
        puertas[2].posY = 0x5f;
        puertas[2].altura = 0x02;
        puertas[2].haciaDentro = false;

        // puerta que conecta las habitaciones con la iglesia
        puertas[3].identificador = 0x08;
        puertas[3].orientacion = ARRIBA;
        puertas[3].posX = 0x9e;
        puertas[3].posY = 0x28;
        puertas[3].altura = 0x02;
        puertas[3].haciaDentro = true;

        // puerta del pasadizo de detrás de la cocina
        puertas[4].identificador = 0x10;
        puertas[4].orientacion = ARRIBA;
        puertas[4].posX = 0x7e;
        puertas[4].posY = 0x26;
        puertas[4].altura = 0x02;
        puertas[4].haciaDentro = false;

        // primera puerta que cierra el paso a la parte izquierda de la planta baja de la abadía
        puertas[5].identificador = 0x00;
        puertas[5].orientacion = IZQUIERDA;
        puertas[5].posX = 0x60;
        puertas[5].posY = 0x76;
        puertas[5].altura = 0x00;
        puertas[5].haciaDentro = true;
        puertas[5].estaFija = true;
        puertas[5].estaAbierta = true;

        // segunda puerta que cierra el paso a la parte izquierda de la planta baja de la abadía
        puertas[6].identificador = 0x00;
        puertas[6].orientacion = IZQUIERDA;
        puertas[6].posX = 0x60;
        puertas[6].posY = 0x7b;
        puertas[6].altura = 0x00;
        puertas[6].haciaDentro = false;
        puertas[6].estaFija = true;
        puertas[6].estaAbierta = true;
    }

    // inicia los objetos del juego
    void iniciaObjetos() {
        Objeto[] objetos = juego.objetos;

        // libro
        objetos[0].orientacion = ABAJO;
        objetos[0].posX = 0x34;
        objetos[0].posY = 0x5e;
        objetos[0].altura = 0x13;

        // guantes
        objetos[1].orientacion = DERECHA;
        objetos[1].posX = 0x6b;
        objetos[1].posY = 0x55;
        objetos[1].altura = 0x06;

        // gafas
        objetos[2].orientacion = DERECHA;
        objetos[2].seHaCogido = true;
        objetos[2].personaje = guillermo;

        // pergamino
        objetos[3].orientacion = ABAJO;
        objetos[3].posX = 0x36;
        objetos[3].posY = 0x5e;
        objetos[3].altura = 0x13;

        // llave 1
        objetos[4].orientacion = DERECHA;
        objetos[4].posX = 0x00;
        objetos[4].posY = 0x00;
        objetos[4].altura = 0x00;

        // llave 2
        objetos[5].orientacion = DERECHA;
        objetos[5].posX = 0x00;
        objetos[5].posY = 0x00;
        objetos[5].altura = 0x00;

        // llave 3
        objetos[6].orientacion = DERECHA;
        objetos[6].posX = 0x35;
        objetos[6].posY = 0x35;
        objetos[6].altura = 0x13;

        // lámpara
        objetos[7].orientacion = DERECHA;
        objetos[7].posX = 0x08;
        objetos[7].posY = 0x08;
        objetos[7].altura = 0x02;
    }

    // avanza el momento del día
    void avanzaMomentoDia() {
        momentoDia = momentoDia + 1;

        // si se han terminado los momentos del día, avanza al siguiente día
        if (momentoDia > COMPLETAS) {
            momentoDia = NOCHE;
            dia = dia + 1;

            // si se ha terminado el séptimo día, vuelve al primer día
            if (dia > 7) dia = 1;

            // dibuja el nuevo día en el marcador
            juego.marcador.dibujaDia(dia);
        }

        juego.marcador.dibujaMomentoDia(momentoDia);

        // obtiene la duración de esta etapa del día
        duracionMomentoDia = duracionEtapasDia[dia - 1][momentoDia] * 0x100;
    }
}
