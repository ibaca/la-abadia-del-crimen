package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.MomentosDia.VISPERAS;
import static com.lavacablasa.ladc.core.Input.BUTTON;
import static com.lavacablasa.ladc.core.Input.SPACE;

import com.lavacablasa.ladc.core.GameContext;
import com.lavacablasa.ladc.core.Input;
import com.lavacablasa.ladc.core.Promise;
import com.lavacablasa.ladc.core.TimingHandler;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public class Juego {

    static final int numPersonajes = 8;
    static final int numPuertas = 7;
    static final int numObjetos = 8;
    static final int primerSpritePersonajes = 0;
    static final int primerSpritePuertas = primerSpritePersonajes + numPersonajes;
    static final int primerSpriteObjetos = primerSpritePuertas + numPuertas;
    static final int spritesReflejos = primerSpriteObjetos + numObjetos;
    static final int spriteLuz = spritesReflejos + 2;
    static final int numSprites = spriteLuz + 1;

    final byte[] introData;
    private final byte[] gameData;
    final GameContext context;
    final TimingHandler timer;
    final CPC6128 cpc6128;

    final Controles controles;
    final Paleta paleta;
    final Pergamino pergamino;

    final Sprite[] sprites = new Sprite[numSprites];                // sprites del juego
    final Puerta[] puertas = new Puerta[numPuertas];                // puertas del juego
    final Objeto[] objetos = new Objeto[numObjetos];                // objetos del juego
    final Personaje[] personajes = new Personaje[numPersonajes];    // personajes del juego

    public volatile int contadorInterrupcion;

    public volatile boolean pausa;

    public GestorFrases gestorFrases;
    public MotorGrafico motor;
    public Marcador marcador;
    public Logica logica;

    public Juego(byte[] memoryData, CPC6128 cpc6128, GameContext context, TimingHandler timer) {
        Objects.requireNonNull(memoryData);
        this.introData = Arrays.copyOfRange(memoryData, 0, 0x4000);
        this.gameData = Arrays.copyOfRange(memoryData, 0x4000, memoryData.length);

        this.timer = Objects.requireNonNull(timer);
        this.cpc6128 = Objects.requireNonNull(cpc6128);
        this.context = Objects.requireNonNull(context);

        byte[] buffer = new byte[8192];                        // buffer para mezclar los sprites y para buscar las rutas
        this.paleta = new Paleta(this);
        this.pergamino = new Pergamino(this);
        this.gestorFrases = new GestorFrases(this);
        this.motor = new MotorGrafico(this, buffer);
        this.marcador = new Marcador(this);
        this.logica = new Logica(this, buffer);
        this.controles = new Controles(context);

        this.contadorInterrupcion = 0;
        this.pausa = false;
    }

    /////////////////////////////////////////////////////////////////////////////
    // método principal del juego
    /////////////////////////////////////////////////////////////////////////////

    public Promise<?> run() {
        return muestraPresentacion()
                .andThen(this::muestraIntroduccion)
                .andThen(this::creaEntidadesJuego)
                .andThen(this::generaGraficosFlipeados)
                .andThen(() -> {
                    marcador.limpiaAreaMarcador();
                    logica.despHabitacionEspejo();
                })
                // aquí ya se ha completado la inicialización de datos para el juego
                // ahora realiza la inicialización para poder empezar a jugar una partida
                .andThen(() -> Promise.doWhile(() -> {
                    // inicia la lógica del juego
                    logica.inicia();

                    // limpia el área de juego y dibuja el marcador
                    limpiaAreaJuego(0);
                    marcador.dibujaMarcador();

                    // inicia el contador de la interrupción
                    contadorInterrupcion = 0;

                    // pone una posición de pantalla inválida para que se redibuje la pantalla
                    motor.posXPantalla = motor.posYPantalla = -1;

                    // dibuja los objetos que tiene guillermo en el marcador
                    marcador.dibujaObjetos(personajes[0].objetos, 0xff);

                    // inicia el marcador (día y momento del día, obsequium y el espacio de las frases)
                    muestraDiaYMomentoDia();
                    marcador.dibujaObsequium(logica.obsequium);
                    marcador.limpiaAreaFrases();

                    // el bucle principal del juego empieza aquí
                    return Promise.doWhile(() -> {
                        // actualiza el estado de los controles
                        controles.actualizaEstado();

                        // obtiene el contador de la animación de guillermo para saber si se generan caminos en esta iteración
                        logica.buscRutas.contadorAnimGuillermo = logica.guillermo.contadorAnimacion;

                        // comprueba si se debe abrir el espejo
                        logica.compruebaAbreEspejo();

                        // comprueba si se ha pulsado la pausa
                        return compruebaPausa()
                                .andThen(() -> {
                                    // actualiza las variables relacionadas con el paso del tiempo
                                    logica.actualizaVariablesDeTiempo();

                                    // si guillermo ha muerto, empieza una partida
                                    return muestraPantallaFinInvestigacion();
                                })
                                .andThen(guillermoMuerto -> {
                                    if (guillermoMuerto) return Promise.of(Boolean.FALSE);
                                    else return Promise.done()
                                            .andThen(() -> {
                                                // comprueba si guillermo lee el libro, y si lo hace sin guantes, lo mata
                                                logica.compruebaLecturaLibro();

                                                // comprueba si hay que avanzar la parte del momento del día en el marcador
                                                marcador.realizaScrollMomentoDia();

                                                // comprueba si hay que ejecutar las acciones programadas según el momento del día
                                                return logica.ejecutaAccionesMomentoDia();
                                            })
                                            .andThen(() -> {
                                                // comprueba si hay opciones de que la cámara siga a otro personaje y calcula los bonus obtenidos
                                                logica.compruebaBonusYCambiosDeCamara();

                                                // comprueba si se ha cambiado de pantalla y actúa en consecuencia
                                                motor.compruebaCambioPantalla();

                                                // comprueba si los personajes cogen o dejan algún objeto
                                                logica.compruebaCogerDejarObjetos();

                                                // comprueba si se abre o se cierra alguna puerta
                                                logica.compruebaAbrirCerrarPuertas();

                                                // ejecuta la lógica de los personajes
                                                for (int i = 0; i < numPersonajes; i++) personajes[i].run();

                                                // indica que en esta iteración no se ha generado ningún camino
                                                logica.buscRutas.generadoCamino = false;

                                                // actualiza el sprite de la luz para que se mueva siguiendo a adso
                                                actualizaLuz();

                                                // si guillermo o adso están frente al espejo, muestra su reflejo
                                                logica.realizaReflejoEspejo();

                                                // dibuja la pantalla si fuera necesario
                                                return motor.dibujaPantalla();
                                            })
                                            .andThen(() -> {
                                                // dibuja los sprites visibles que hayan cambiado
                                                motor.dibujaSprites();

                                                // espera un poco para actualizar el estado del juego
                                                return Promise.doWhile(() -> {
                                                    if (contadorInterrupcion < 0x24) return timer.sleep(5).map(true);
                                                    else return Promise.of(false);
                                                }).andThen(() -> {
                                                    // reinicia el contador de la interrupción
                                                    contadorInterrupcion = 0;
                                                }).map(true);
                                            });
                                });
                    }).map(true);
                }));
    }

    public void runSync() {
        if (!pausa) {
            // incrementa el contador de la interrupción
            contadorInterrupcion++;

            // si se está mostrando alguna frase en el marcador, continúa mostrándola
            gestorFrases.procesaFraseActual();
        }
    }

    // limpia el área de juego de color que se le pasa y los bordes de negro
    void limpiaAreaJuego(int color) {
        cpc6128.fillMode1Rect(0, 0, 32, 160, 3);
        cpc6128.fillMode1Rect(32, 0, 256, 160, color);
        cpc6128.fillMode1Rect(32 + 256, 0, 32, 160, 3);
    }

    private void generaGraficosFlipeados() {
        byte[] tablaFlipX = new byte[256];

        // inicia la tabla para flipear los gráficos
        for (int i = 0; i < 256; i++) {
            // extrae los pixels
            int pixel0 = CPC6128.unpackPixelMode1(i, 0);
            int pixel1 = CPC6128.unpackPixelMode1(i, 1);
            int pixel2 = CPC6128.unpackPixelMode1(i, 2);
            int pixel3 = CPC6128.unpackPixelMode1(i, 3);

            int data = 0;

            // combina los pixels en orden inverso
            data = cpc6128.packPixelMode1(data, 0, pixel3);
            data = cpc6128.packPixelMode1(data, 1, pixel2);
            data = cpc6128.packPixelMode1(data, 2, pixel1);
            data = cpc6128.packPixelMode1(data, 3, pixel0);

            // guarda el resultado
            tablaFlipX[i] = (byte) data;
        }

        // genera los gráficos de las animaciones de guillermo flipeados respecto a x
        flipeaGraficos(tablaFlipX, 0x0a300, 0x16300, 5, 0x366);
        flipeaGraficos(tablaFlipX, 0x0a666, 0x16666, 4, 0x084);

        // genera los gráficos de las animaciones de adso flipeados respecto a x
        flipeaGraficos(tablaFlipX, 0x0a6ea, 0x166ea, 5, 0x1db);
        flipeaGraficos(tablaFlipX, 0x0a8c5, 0x168c5, 4, 0x168);

        // genera los gráficos de los trajes de los monjes flipeados respecto a x
        flipeaGraficos(tablaFlipX, 0x0ab59, 0x16b59, 5, 0x2d5);

        // genera los gráficos de las caras de los monjes flipeados respecto a x
        flipeaGraficos(tablaFlipX, 0x0b103, 0x17103, 5, 0x2bc);

        // genera los gráficos de las puertas flipeados respecto a x
        flipeaGraficos(tablaFlipX, 0x0aa49, 0x16a49, 6, 0x0f0);
    }

    // copia los gráficos de origen en el destino y los flipea
    private void flipeaGraficos(byte[] tablaFlip, int src, int dest, int ancho, int bytes) {
        // copia los gráficos del origen al destino
        System.arraycopy(gameData, src, gameData, dest, bytes);

        // calcula las variables que controlan el bucle
        int numLineas = bytes / ancho;
        int numIntercambios = (ancho + 1) / 2;

        // recorre todas las líneas que forman el gráfico
        for (int j = 0; j < numLineas; j++) {
            int ptr1 = dest;
            int ptr2 = ptr1 + ancho - 1;

            // realiza los intercambios necesarios para flipear esta línea
            for (int i = 0; i < numIntercambios; i++) {
                byte aux = gameData[ptr1];
                gameData[ptr1] = tablaFlip[gameData[ptr2] & 0xff];
                gameData[ptr2] = tablaFlip[aux & 0xff];

                ptr1++;
                ptr2--;
            }

            // pasa a la siguiente línea
            dest = dest + ancho;
        }
    }

    // actualiza el sprite de la luz para que se mueva siguiendo a adso
    private void actualizaLuz() {
        // desactiva el sprite de la luz
        sprites[spriteLuz].esVisible = false;

        // si la pantalla está iluminada, sale
        if (motor.pantallaIluminada) return;

        // si adso no es visible en la pantalla actual
        if (!(personajes[1].sprite.esVisible)) {
            for (int i = 0; i < numSprites; i++) {
                if (sprites[i].esVisible) {
                    sprites[i].haCambiado = false;
                }
            }

            return;
        }

        // actualiza las características del sprite de la luz según la posición del personaje
        SpriteLuz sprLuz = (SpriteLuz) sprites[spriteLuz];
        sprLuz.ajustaAPersonaje(personajes[1]);
    }

    // comprueba si se debe pausar el juego
    private Promise<?> compruebaPausa() {
        if (!controles.seHaPulsado(Input.SUPR)) return Promise.done();
        // si se ha pulsado suprimir, se para hasta que se vuelva a pulsar
        return Promise.doWhile(() -> timer.sleep(10).map(n -> {
            controles.actualizaEstado();
            return !controles.seHaPulsado(Input.SUPR);
        }));
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos para mostrar distintas las pantallas de distintas situaciones del juego
    /////////////////////////////////////////////////////////////////////////////

    // muestra la imagen de presentación del juego
    private Promise muestraPresentacion() {
        cpc6128.setMode(0);
        paleta.setIntroPalette();// fija la paleta de la presentación
        cpc6128.showMode0Screen(introData);// muestra la pantalla de la presentación
        return timer.sleep(5000);// espera 5 segundos
    }

    // muestra el pergamino de presentación
    private Promise<?> muestraIntroduccion() {
        cpc6128.setMode(1);
        return pergamino.muestraTexto(PergaminoTextos.PERGAMINO_INICIO)// muestra la introducción
                .andThen(() -> paleta.setGamePalette(0))// coloca la paleta negra
                .andThen(() -> Promise.doWhile(() -> timer.sleep(1).andThen(() -> {// espera a que se suelte el botón
                    controles.actualizaEstado();
                    return Promise.of(controles.estaSiendoPulsado(BUTTON));
                })));
    }

    // muestra indefinidamente el pergamino del final
    Promise<?> muestraFinal() {
        return Promise.doWhile(() -> pergamino.muestraTexto(PergaminoTextos.PERGAMINO_FINAL).map(true));
    }

    // muestra la parte de misión completada. Si se ha completado el juego, muestra el final
    private Promise<Boolean> muestraPantallaFinInvestigacion() {
        // si guillermo está vivo, sale
        if (!logica.haFracasado) return Promise.of(false);

        // indica que la cámara siga a guillermo y lo haga ya
        logica.numPersonajeCamara = 0x80;

        // si está mostrando una frase por el marcador, espera a que se termine de mostrar
        if (gestorFrases.mostrandoFrase) return Promise.of(false);

        // oculta el área de juego
        limpiaAreaJuego(3);

        // calcula el porcentaje de misión completada. Si se ha completado el juego, muestra el final
        return logica.calculaPorcentajeMision().andThen(porc -> {
            String porcentaje = String.format("  %02d POR CIENTO", porc);
            marcador.imprimeFrase("HAS RESUELTO EL", 96, 32, 2, 3);
            marcador.imprimeFrase(porcentaje, 88, 48, 2, 3);
            marcador.imprimeFrase("DE LA INVESTIGACION", 80, 64, 2, 3);
            marcador.imprimeFrase("PULSA ESPACIO PARA EMPEZAR", 56, 128, 2, 3);

            // espera a que se pulse y se suelte el botón
            Supplier<Boolean> pulsado = () -> controles.estaSiendoPulsado(BUTTON) || controles.estaSiendoPulsado(SPACE);
            return Promise.doWhile(() -> {
                controles.actualizaEstado();
                return timer.sleep(1).map(n -> !pulsado.get());
            }).andThen(() -> Promise.doWhile(() -> {
                controles.actualizaEstado();
                return timer.sleep(1).map(n -> pulsado.get());
            })).map(n -> true);
        });
    }

    /////////////////////////////////////////////////////////////////////////////
    // creación de las entidades del juego
    /////////////////////////////////////////////////////////////////////////////

    // crea los sprites, personajes, puertas y objetos del juego
    private void creaEntidadesJuego() {
        // sprites de los personajes

        // sprite de guillermo
        sprites[0] = new Sprite();

        // sprite de adso
        sprites[1] = new Sprite();

        // sprite de los monjes
        for (int i = 2; i < 8; i++) {
            sprites[i] = new SpriteMonje();
        }

        // sprite de las puertas
        for (int i = primerSpritePuertas; i < primerSpritePuertas + numPuertas; i++) {
            sprites[i] = new Sprite();
            sprites[i].ancho = sprites[i].oldAncho = 0x06;
            sprites[i].alto = sprites[i].oldAlto = 0x28;
        }

        int[] despObjetos = { 0x88f0, 0x9fb0, 0x9f80, 0xa010, 0x9fe0, 0x9fe0, 0x9fe0, 0x88c0 };

        // sprite de los objetos
        for (int i = primerSpriteObjetos; i < primerSpriteObjetos + numObjetos; i++) {
            sprites[i] = new Sprite();
            sprites[i].ancho = sprites[i].oldAncho = 0x04;
            sprites[i].alto = sprites[i].oldAlto = 0x0c;
            sprites[i].despGfx = despObjetos[i - primerSpriteObjetos];
        }

        // sprite de los reflejos en el espejo
        sprites[spritesReflejos] = new Sprite();
        sprites[spritesReflejos + 1] = new Sprite();

        // sprite de la luz
        sprites[spriteLuz] = new SpriteLuz();

        // crea los personajes del juego
        personajes[0] = new Guillermo(this, sprites[0]);
        personajes[1] = new Adso(this, sprites[1]);
        personajes[2] = new Malaquias(this, (SpriteMonje) sprites[2]);
        personajes[3] = new Abad(this, (SpriteMonje) sprites[3]);
        personajes[4] = new Berengario(this, (SpriteMonje) sprites[4]);
        personajes[5] = new Severino(this, (SpriteMonje) sprites[5]);
        personajes[6] = new Jorge(this, (SpriteMonje) sprites[6]);
        personajes[7] = new Bernardo(this, (SpriteMonje) sprites[7]);

        // inicia los valores comunes
        for (int i = 0; i < 8; i++) {
            personajes[i].despX = -2;
            personajes[i].despY = -34;
        }
        personajes[1].despY = -32;

        // crea las puertas del juego
        for (int i = 0; i < numPuertas; i++) {
            puertas[i] = new Puerta(this, sprites[primerSpritePuertas + i]);
        }

        // crea los objetos del juego
        for (int i = 0; i < numObjetos; i++) {
            objetos[i] = new Objeto(this, sprites[primerSpriteObjetos + i]);
        }
    }

    int gameData(int pos) {
        return gameData[pos] & 0xff;
    }

    void gameData(int pos, int value) {
        gameData[pos] = (byte) value;
    }

    int gameDataW(int pos) {
        return gameData[pos] & 0xff | ((gameData[pos + 1] & 0xff) << 8);
    }

    // avanza el momento del día del marcador
    void muestraDiaYMomentoDia() {
        // coloca una paleta según el momento del día
        if (logica.momentoDia < VISPERAS) {
            paleta.setGamePalette(2);
        } else {
            paleta.setGamePalette(3);
        }

        // dibuja el número de día en el marcador
        marcador.dibujaDia(logica.dia);

        // hace que avance el momento del día, para mostrar el efecto de scroll en las letras del día
        logica.momentoDia = logica.momentoDia - 1;
        logica.avanzaMomentoDia();
    }

}
