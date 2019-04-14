package com.lavacablasa.ladc.abadia;

import com.lavacablasa.ladc.core.Input;
import com.lavacablasa.ladc.core.TimingHandler;

class Pergamino {

    private static final int SCROLL_TOP = 0x788a;
    private static final int SCROLL_RIGHT = 0x7a0a;
    private static final int SCROLL_LEFT = 0x7b8a;
    private static final int SCROLL_BOTTOM = 0x7d0a;
    // campos
    private final Juego juego;

    Pergamino(Juego juego) {
        this.juego = juego;
    }

    // métodos

    void muestraTexto(String texto) {
        // pone la paleta negra
        juego.paleta.setGamePalette(0);

        // dibuja el pergamino
        dibuja();

        // pone la paleta del pergamino
        juego.paleta.setGamePalette(1);

        // dibuja el texto que se le pasa
        dibujaTexto(texto);
    }

    // métodos de ayuda
    private void dibuja() {
        // limpia la memoria de video
        juego.cpc6128.fillMode1Rect(0, 0, 320, 200, 0);

        // limpia los bordes del rectángulo que formará el pergamino
        juego.cpc6128.fillMode1Rect(0, 0, 64, 200, 1);
        juego.cpc6128.fillMode1Rect(192 + 64, 0, 64, 200, 1);
        juego.cpc6128.fillMode1Rect(0, 192, 320, 8, 1);

        // rellena la parte superior del pergamino
        dibujaTiraHorizontal(0, SCROLL_TOP);

        // rellena la parte derecha del pergamino
        dibujaTiraVertical(248, SCROLL_RIGHT);

        // rellena la parte izquierda del pergamino
        dibujaTiraVertical(64, SCROLL_LEFT);

        // rellena la parte inferior del pergamino
        dibujaTiraHorizontal(184, SCROLL_BOTTOM);
    }

    private void dibujaTiraHorizontal(int y, int pos) {
        // recorre el ancho del pergamino
        for (int i = 0; i < 192 / 4; i++) {
            // la parte superior ocupa 8 pixels de alto
            for (int j = 0; j < 8; j++) {
                int value = juego.gameData(pos);
                for (int k = 0; k < 4; k++) {
                    juego.cpc6128.setMode1Pixel(64 + 4 * i + k, j + y, CPC6128.unpackPixelMode1(value, k));
                }
                pos++;
            }
        }
    }

    private void dibujaTiraVertical(int x, int pos) {
        // recorre el alto del pergamino
        for (int j = 0; j < 192; j++) {
            // lee 8 pixels y los escribe en pantalla
            for (int i = 0; i < 2; i++) {
                int value = juego.gameData(pos);
                for (int k = 0; k < 4; k++) {
                    juego.cpc6128.setMode1Pixel(x + 4 * i + k, j, CPC6128.unpackPixelMode1(value, k));
                }
                pos++;
            }
        }
    }

    private void dibujaTexto(String texto) {
        // obtiene acceso al temporizador y a las entradas
        TimingHandler timer = juego.timer;

        // posición inicial del texto en el pergamino
        int posX = 76;
        int posY = 16;

        // puntero a la tabla de punteros a los gráficos de los caracteres
        int charTable = 0x680c;

        // repite hasta que se pulse el botón 1
        int pos = 0;
        while (true) {
            juego.controles.actualizaEstado();

            // si se pulsó el botón 1 o espacio, termina
            if (juego.controles.estaSiendoPulsado(Input.BUTTON) || juego.controles.estaSiendoPulsado(Input.SPACE)) {
                break;
            } else {
                // dependiendo del carácter leido
                char c = texto.charAt(pos);
                switch (c) {
                    case 0x1a:            // fín de pergamino
                        break;
                    case 0x0d:            // salto de línea
                        posX = 76;
                        posY += 16;
                        timer.sleep(600);

                        // si hay que pasar página del pergamino
                        if (posY > 164) {
                            posX = 76;
                            posY = 16;
                            timer.sleep(2000);
                            pasaPagina();
                        }
                        break;
                    case 0x20:            // espacio
                        posX += 10;
                        timer.sleep(30);
                        break;
                    case 0x0a:            // salto de página
                        posX = 76;
                        posY = 16;
                        timer.sleep(3 * 525);
                        pasaPagina();
                        break;

                    default:            // carácter imprimible
                        // elige un color dependiendo de si es mayúsculas o minúsculas
                        int color = (((c) & 0x60) == 0x40) ? 3 : 2;

                        // obtiene el desplazamiento a los datos de formación del carácter
                        int charOffset = juego.gameDataW(charTable + 2 * (c - 0x20));

                        // si el caracter no está definido, muestra una 'z'
                        if (charOffset == 0) {
                            charOffset = juego.gameDataW(charTable + 2 * ('z' - 0x20));
                        }

                        // mientras queden trazos del carácter
                        while ((juego.gameData(charOffset) & 0xf0) != 0xf0) {
                            // halla el desplazamiento del trazo
                            int newPosX = posX + (juego.gameData(charOffset) & 0x0f);
                            int newPosy = posY + ((juego.gameData(charOffset) >> 4) & 0x0f);

                            // dibuja el trazo del carácter
                            juego.cpc6128.setMode1Pixel(newPosX, newPosy, color);

                            charOffset++;

                            // espera un poco para que se pueda apreciar como se traza el carácter
                            timer.sleep(8);
                        }

                        // avanza la posición hasta el siguiente carácter
                        posX += juego.gameData(charOffset) & 0x0f;
                }

                // apunta al siguiente carácter a imprimir
                if (c != 0x1a) {
                    pos++;
                }
            }
        }
    }

    /**
     * dibuja un triángulo rectángulo de color 1 con catetos paralelos a los ejes x e y, y limpia los 4
     * pixels a la derecha de la hipotenusa del triángulo con el color 0
     */
    private void dibujaTriangulo(int x, int y, int dim) {
        dim = dim * 4;

        for (int j = 0; j < dim; j++) {
            // dibuja el triángulo
            for (int i = 0; i <= j; i++) {
                juego.cpc6128.setMode1Pixel(x + i, y + j, 1);
            }

            // elimina restos de una ejecución anterior
            for (int i = 0; i < 4; i++) {
                juego.cpc6128.setMode1Pixel(x + j + i + 1, y + j, 0);
            }
        }

    }

    // restaura un trozo de 8x8 pixels de la parte superior y otro de la parte derecha del pergamino
    private void restauraParteSuperiorYDerecha(int x, int y, int lado) {
        x = x + 4;

        // apunta a los datos borrados del borde superior del pergamino
        int data = SCROLL_TOP + (48 - lado) * 4 * 2;

        // 8 pixels de ancho
        for (int i = 0; i < 2; i++) {
            // 8 pixels de alto
            for (int j = 0; j < 8; j++) {
                int value = juego.gameData(data);
                for (int k = 0; k < 4; k++) {
                    juego.cpc6128.setMode1Pixel(x + 4 * i + k, y + j, CPC6128.unpackPixelMode1(value, k));
                }
                data++;
            }
        }

        x = 248;
        y = (lado - 3) * 4;

        // apunta a los datos borrados de la parte derecha del pergamino
        data = SCROLL_RIGHT + y * 2;

        // 8 pixels de alto
        for (int j = 0; j < 8; j++) {
            // 8 pixels de ancho
            for (int i = 0; i < 2; i++) {
                int value = juego.gameData(data);
                for (int k = 0; k < 4; k++) {
                    juego.cpc6128.setMode1Pixel(x + 4 * i + k, y + j, CPC6128.unpackPixelMode1(value, k));
                }
                data++;
            }
        }
    }

    // restaura un trozo de 4x8 pixels de la parte inferior del pergamino
    private void restauraParteInferior(int x, int y, int lado) {
        x = 64 + lado * 4;
        y = 184;

        // apunta a los datos borrados del borde inferior del pergamino
        int data = SCROLL_BOTTOM + lado * 4 * 2;

        // dibuja un trozo de 4x8 pixels de la parte inferior del pergamino
        for (int j = 0; j < 8; j++) {
            int value = juego.gameData(data);
            for (int k = 0; k < 4; k++) {
                juego.cpc6128.setMode1Pixel(x + k, y + j, CPC6128.unpackPixelMode1(value, k));
            }
            data++;
        }
    }

    private void pasaPagina() {
        // obtiene acceso al temporizador
        TimingHandler timer = juego.timer;

        int x = 240;
        int y = 0;
        int dim = 3;

        // realiza el efecto del paso de página desde la esquina superior derecha hasta la mitad de la página
        for (int num = 0; num < 45; num++) {
            dibujaTriangulo(x, y, dim);
            timer.sleep(20);
            restauraParteSuperiorYDerecha(x, y, dim);

            x = x - 4;
            dim++;
        }
        restauraParteSuperiorYDerecha(x, y, dim);

        x = 64;
        y = 4;
        dim = 47;

        // realiza el efecto del paso de página desde la mitad de la página hasta terminar en la esquina inferior izquierda
        for (int num = 0; num < 46; num++) {
            dibujaTriangulo(x, y, dim);
            timer.sleep(20);

            y = y - 4;

            // apunta a los datos borrados del borde izquierdo del pergamino
            int data = SCROLL_LEFT + y * 2;

            // dibuja un trozo de 8x4 de la parte izquierda del pergamino
            for (int j = 0; j < 4; j++) {
                for (int i = 0; i < 2; i++) {
                    int value = juego.gameData(data);
                    for (int k = 0; k < 4; k++) {
                        juego.cpc6128.setMode1Pixel(x + 4 * i + k, y + j, CPC6128.unpackPixelMode1(value, k));
                    }
                    data++;
                }
            }

            // restaura un trozo de 4x8 pixels de la parte inferior del pergamino
            restauraParteInferior(x, y, dim);

            y = y + 8;
            dim--;
        }
        restauraParteInferior(x, y, 1);
        restauraParteInferior(x, y, 0);
    }
}
