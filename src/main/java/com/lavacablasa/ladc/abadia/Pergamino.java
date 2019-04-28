package com.lavacablasa.ladc.abadia;

import static com.lavacablasa.ladc.abadia.CPC6128.unpackPixelMode1;
import static com.lavacablasa.ladc.core.Input.BUTTON;
import static com.lavacablasa.ladc.core.Input.SPACE;

import com.lavacablasa.ladc.core.Promise;

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

    Promise<?> muestraTexto(String texto) {
        juego.paleta.setGamePalette(0);// pone la paleta negra
        dibuja();// dibuja el pergamino
        juego.paleta.setGamePalette(1);// pone la paleta del pergamino
        return dibujaTexto(texto);// dibuja el texto que se le pasa
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
                    juego.cpc6128.setMode1Pixel(64 + 4 * i + k, j + y, unpackPixelMode1(value, k));
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
                    juego.cpc6128.setMode1Pixel(x + 4 * i + k, j, unpackPixelMode1(value, k));
                }
                pos++;
            }
        }
    }

    private Promise<?> dibujaTexto(String texto) {
        var state = new Object() {
            // posición inicial del texto en el pergamino
            int posX = 76;
            int posY = 16;

            // puntero a la tabla de punteros a los gráficos de los caracteres
            int charTable = 0x680c;

            // repite hasta que se pulse el botón 1
            int pos = 0;
        };
        return Promise.doWhile(() -> {
            juego.controles.actualizaEstado();

            // si se pulsó el botón 1 o espacio, termina
            if (juego.controles.estaSiendoPulsado(BUTTON) || juego.controles.estaSiendoPulsado(SPACE)) {
                return Promise.of(false);
            }

            // dependiendo del carácter leido
            return Promise.of(texto.charAt(state.pos)).andThen(c -> {
                switch (c) {
                    case 0x1a: return Promise.of(c); // fín de pergamino
                    case 0x0d: // salto de línea
                        state.posX = 76;
                        state.posY += 16;
                        return juego.timer.sleep(600).andThen(() -> {
                            if (state.posY <= 164) return Promise.of(c);
                            // si hay que pasar página del pergamino
                            state.posX = 76; state.posY = 16;
                            return juego.timer.sleep(2000).andThen(this::pasaPagina).map(c);
                        });
                    case 0x20: // espacio
                        state.posX += 10;
                        return juego.timer.sleep(30).map(c);
                    case 0x0a: // salto de página
                        state.posX = 76;
                        state.posY = 16;
                        return juego.timer.sleep(3 * 525).andThen(this::pasaPagina).map(c);
                    default: // carácter imprimible
                        // elige un color dependiendo de si es mayúsculas o minúsculas
                        int color = (((c) & 0x60) == 0x40) ? 3 : 2;
                        // obtiene el desplazamiento a los datos de formación del carácter
                        int charOffset = juego.gameDataW(state.charTable + 2 * (c - 0x20));
                        // si el caracter no está definido, muestra una 'z'
                        if (charOffset == 0) charOffset = juego.gameDataW(state.charTable + 2 * ('z' - 0x20));
                        // mientras queden trazos del carácter
                        return Promise.doWhile(charOffset, co -> (juego.gameData(co) & 0xf0) != 0xf0, co -> {
                            // halla el desplazamiento del trazo
                            int newPosX = state.posX + (juego.gameData(co) & 0x0f);
                            int newPosy = state.posY + ((juego.gameData(co) >> 4) & 0x0f);
                            // dibuja el trazo del carácter
                            juego.cpc6128.setMode1Pixel(newPosX, newPosy, color);
                            // espera un poco para que se pueda apreciar como se traza el carácter
                            return juego.timer.sleep(8).map(co + 1);
                        }).andThen(co -> {
                            // avanza la state.posición hasta el siguiente carácter
                            state.posX += juego.gameData(co) & 0x0f;
                            return Promise.of(c);
                        });
                }
            }).andThen(c -> {
                // apunta al siguiente carácter a imprimir
                if (c != 0x1a) state.pos++;
                return Promise.of(true);
            });
        });
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
                    juego.cpc6128.setMode1Pixel(x + 4 * i + k, y + j, unpackPixelMode1(value, k));
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
                    juego.cpc6128.setMode1Pixel(x + 4 * i + k, y + j, unpackPixelMode1(value, k));
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
                juego.cpc6128.setMode1Pixel(x + k, y + j, unpackPixelMode1(value, k));
            }
            data++;
        }
    }

    private Promise<Void> pasaPagina() {
        return Promise.of(new Object() {
            int num;
            int x = 240;
            int y = 0;
            int dim = 3;
        }).andThen(state -> {
            // realiza el efecto del paso de página desde la esquina superior derecha hasta la mitad de la página
            state.num = 0;
            return Promise.doWhile(state, s -> s.num < 45, s -> {
                state.num++;
                dibujaTriangulo(s.x, s.y, s.dim);
                return juego.timer.sleep(20).andThen(() -> {
                    restauraParteSuperiorYDerecha(s.x, s.y, s.dim);
                    s.x = s.x - 4;
                    s.dim++;
                }).map(s);
            });
        }).andThen(state -> {
            restauraParteSuperiorYDerecha(state.x, state.y, state.dim);
            state.x = 64;
            state.y = 4;
            state.dim = 47;

            // realiza el efecto del paso de página desde la mitad de la página hasta terminar en la esquina inferior izquierda
            state.num = 0;
            return Promise.doWhile(state, s -> s.num < 46, s -> {
                state.num++;
                dibujaTriangulo(s.x, s.y, s.dim);
                return juego.timer.sleep(20).andThen(() -> {
                    s.y = s.y - 4;

                    // apunta a los datos borrados del borde izquierdo del pergamino
                    int data = SCROLL_LEFT + s.y * 2;

                    // dibuja un trozo de 8x4 de la parte izquierda del pergamino
                    for (int j = 0; j < 4; j++) {
                        for (int i = 0; i < 2; i++) {
                            int value = juego.gameData(data);
                            for (int k = 0; k < 4; k++) {
                                juego.cpc6128.setMode1Pixel(s.x + 4 * i + k, s.y + j, unpackPixelMode1(value, k));
                            }
                            data++;
                        }
                    }

                    // restaura un trozo de 4x8 pixels de la parte inferior del pergamino
                    restauraParteInferior(s.x, s.y, s.dim);

                    s.y = s.y + 8;
                    s.dim--;
                }).map(s);
            });
        }).andThen(state -> {
            restauraParteInferior(state.x, state.y, 1);
            restauraParteInferior(state.x, state.y, 0);
            return Promise.done();
        });
    }
}
