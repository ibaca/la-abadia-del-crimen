package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.ObjetosJuego.LIBRO;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.LLAVE1;
import static com.lavacablasa.ladc.abadia.ObjetosJuego.PERGAMINO;
import static com.lavacablasa.ladc.abadia.Orientacion.ARRIBA;
import static com.lavacablasa.ladc.abadia.Orientacion.DERECHA;
import static com.lavacablasa.ladc.abadia.Orientacion.IZQUIERDA;

class AccionesDia {

    private final Juego juego;
    private final Runnable[] acciones;

    AccionesDia(Juego juego) {
        this.juego = juego;
        this.acciones = new Runnable[7];

        this.acciones[MomentosDia.NOCHE] = this::ejecutaAccionesNoche;
        this.acciones[MomentosDia.PRIMA] = this::ejecutaAccionesPrima;
        this.acciones[MomentosDia.TERCIA] = this::ejecutaAccionesTercia;
        this.acciones[MomentosDia.SEXTA] = this::ejecutaAccionesSexta;
        this.acciones[MomentosDia.NONA] = this::ejecutaAccionesNona;
        this.acciones[MomentosDia.VISPERAS] = this::ejecutaAccionesVisperas;
        this.acciones[MomentosDia.COMPLETAS] = this::ejecutaAccionesCompletas;
    }

    /////////////////////////////////////////////////////////////////////////////
    // ejecución de las acciones programadas
    /////////////////////////////////////////////////////////////////////////////

    void ejecutaAccionesProgramadas() {
        // si no ha cambiado el momento del día, sale
        if (juego.logica.momentoDia != juego.logica.oldMomentoDia) {
            juego.logica.oldMomentoDia = juego.logica.momentoDia;
            juego.logica.cntMovimiento = 0;

            // ejecuta unas acciones dependiendo del momento del día
            acciones[juego.logica.momentoDia].run();
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // acciones programadas según el momento del día
    /////////////////////////////////////////////////////////////////////////////

    private void ejecutaAccionesNoche() {
        if (juego.logica.dia == 5) {
            // pone las gafas en la habitación iluminada del laberinto
            colocaObjeto(juego.objetos[2], 0x1b, 0x23, 0x18);

            // pone la llave 1 en el altar
            colocaObjeto(juego.objetos[4], 0x89, 0x3e, 0x08);
        } else if (juego.logica.dia == 6) {
            // pone la llave de la habitación de severino en la mesa de malaquías
            colocaObjeto(juego.objetos[5], 0x35, 0x35, 0x13);

            // coloca a jorge en la habitación de detrás del espejo
            colocaPersonaje(juego.logica.jorge, 0x12, 0x65, 0x18, ARRIBA);
            juego.logica.jorge.estaActivo = true;
        }
    }

    private void ejecutaAccionesPrima() {
        // dibuja el efecto de la espiral
        dibujaEfectoEspiral();

        // modifica las puertas que pueden abrirse
        juego.logica.mascaraPuertas = 0xef;

        // fija la paleta de día
        juego.paleta.setGamePalette(2);

        // abre las puertas del ala izquierda de la abadía
        juego.puertas[5].orientacion = IZQUIERDA;
        juego.puertas[5].haciaDentro = true;
        juego.puertas[5].estaFija = true;
        juego.puertas[5].estaAbierta = true;
        juego.puertas[6].orientacion = IZQUIERDA;
        juego.puertas[6].haciaDentro = false;
        juego.puertas[6].estaFija = true;
        juego.puertas[6].estaAbierta = true;

        if (juego.logica.dia >= 3) {
            // si se ha usado la lámpara, desaparece
            juego.logica.reiniciaContadoresLampara();

            // si la lámpara había desaparecido, la pone en la cocina
            if (juego.logica.lamparaDesaparecida) {
                juego.logica.lamparaDesaparecida = false;

                colocaObjeto(juego.objetos[7], 0x5a, 0x2a, 0x04);
            }
        }

        if (juego.logica.dia == 2) {
            // desaparecen las gafas
            juego.logica.guillermo.objetos &= 0xdf;
            juego.logica.berengario.objetos &= 0xdf;

            colocaObjeto(juego.objetos[2], 0, 0, 0);

            // dibuja los objetos que tiene guillermo en el marcador
            juego.marcador.dibujaObjetos(juego.logica.guillermo.objetos, 0xff);
        }

        if (juego.logica.dia == 3) {
            // jorge coge el libro y lo esconde
            juego.logica.jorge.objetos = LIBRO;
            colocaObjeto(juego.objetos[0], 0x0f, 0x2e, 0x00);

            // escribe un comando para pensar un nuevo movimiento
            juego.logica.jorge.numBitAcciones = 0;
            juego.logica.jorge.posAcciones = 0;
            juego.logica.jorge.bufAcciones[0] = 0x10;

            // coloca a jorge al final del pasillo de las celdas de los monjes
            colocaPersonaje(juego.logica.jorge, 0xc8, 0x24, 0x00, DERECHA);
            juego.logica.jorge.estaActivo = true;

            // indica que el abad no tiene ningún objeto
            juego.logica.abad.objetos = 0;

            // si guillermo no tiene el pergamino, se coloca en la habitación de detrás del espejo
            if ((juego.logica.guillermo.objetos & PERGAMINO) == 0) {
                colocaObjeto(juego.objetos[3], 0x18, 0x64, 0x18);
                juego.logica.pergaminoGuardado = true;
            }
        }

        // si es el quinto día y no tenemos la llave 1, ésta desaparece
        if ((juego.logica.dia == 5) && ((juego.logica.guillermo.objetos & LLAVE1) == 0)) {
            colocaObjeto(juego.objetos[4], 0, 0, 0);
        }
    }

    private void ejecutaAccionesTercia() {
        // dibuja el efecto de la espiral
        dibujaEfectoEspiral();
    }

    private void ejecutaAccionesSexta() {
        if (juego.logica.dia == 4) {
            // bernardo gui aparece en las escaleras de la abadía
            juego.logica.bernardo.estaEnLaAbadia = true;
            colocaPersonaje(juego.logica.bernardo, 0x88, 0x88, 0x02, DERECHA);

            // indica que bernardo puede coger el pergamino
            juego.logica.bernardo.mascaraObjetos = PERGAMINO;
        }
    }

    private void ejecutaAccionesNona() {
        // dibuja el efecto de la espiral
        dibujaEfectoEspiral();

        // si es el tercer día, jorge pasa a estar inactivo y desaparece
        if (juego.logica.dia == 3) {
            juego.logica.jorge.estaActivo = false;
            juego.logica.jorge.posX = juego.logica.jorge.posY = juego.logica.jorge.altura = 0;
        }
    }

    private void ejecutaAccionesVisperas() {
    }

    private void ejecutaAccionesCompletas() {
        // dibuja el efecto de la espiral
        dibujaEfectoEspiral();

        // fija la paleta de noche
        juego.paleta.setGamePalette(3);

        // modifica las puertas que pueden abrirse
        juego.logica.mascaraPuertas = 0xdf;
    }

    /////////////////////////////////////////////////////////////////////////////
    // dibujo de la espiral
    /////////////////////////////////////////////////////////////////////////////

    // genera el efecto de la espiral
    private void dibujaEfectoEspiral() {
        juego.marcador.dibujaEspiral();

        // indica un cambio de pantalla
        juego.motor.posXPantalla = juego.motor.posYPantalla = -1;
    }

    /////////////////////////////////////////////////////////////////////////////
    // método de ayuda para colocar los objetos y los personajes
    /////////////////////////////////////////////////////////////////////////////

    private void colocaObjeto(Objeto obj, int posX, int posY, int altura) {
        obj.seHaCogido = false;
        obj.seEstaCogiendo = false;
        obj.personaje = null;
        obj.posX = posX;
        obj.posY = posY;
        obj.altura = altura;
        obj.orientacion = DERECHA;
    }

    private void colocaPersonaje(Personaje pers, int posX, int posY, int altura, int orientacion) {
        pers.posX = posX;
        pers.posY = posY;
        pers.altura = altura;
        pers.orientacion = orientacion;
        pers.enDesnivel = false;
        pers.giradoEnDesnivel = false;
        pers.bajando = false;
    }
}
