package com.lavacablasa.ladc.abadia;

class Marcador {

    private static final int CHARSET_ADDR = 0xb400;
    private static final int BLANK_ADDR = 0x38e7;
    private static final int TIMES_ADDR = 0x4fbc;
    private static final int DAY_BLANK_ADDR = 0x5581;
    private static final int DAY_I_ADDR = 0xab49;
    private static final int DAY_V_ADDR = 0xab39;
    private static final int DAYS_ADDR = 0x4fa7;

    Juego juego;
    int numPosScrollDia;        // número de posiciones para completar el scroll del nombre del día
    int nombreMomentoDia;       // apunta al nombre del momento actual del día

    public Marcador(Juego juego) {
        this.juego = juego;
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con los días y los momentos del día
    /////////////////////////////////////////////////////////////////////////////

    void dibujaMomentoDia(int momentoDia) {
        // obtiene un puntero a los caracteres que forman el momento del día
        nombreMomentoDia = TIMES_ADDR + 7 * momentoDia;

        // quedan 9 caracteres para completar el scroll del nombre del día
        numPosScrollDia = 9;
    }

    // dibuja el día en el marcador
    void dibujaDia(int numDia) {
        // indexa en la tabla de los días
        int data = DAYS_ADDR + (numDia - 1) * 3;

        // dibuja los 3 números romanos que forman el día en el que se está
        dibujaDigitoDia(juego.gameData(data + 0), 68, 165);
        dibujaDigitoDia(juego.gameData(data + 1), 68 + 8, 165);
        dibujaDigitoDia(juego.gameData(data + 2), 68 + 16, 165);
    }

    // dibuja un número romano que forma el día en la posición que se le pasa
    void dibujaDigitoDia(int digito, int x, int y) {
        // apunta a 8 pixels negros
        int despDigito = DAY_BLANK_ADDR;

        // si se le pasó una 'I'
        if (digito == 2) {
            despDigito = DAY_I_ADDR;
        } else if (digito == 1) {
            // si se le pasó una 'V'
            despDigito = DAY_V_ADDR;
        }

        // obtiene un puntero a los gráficos del dígito
        int data = despDigito;

        // rellena las 8 líneas que ocupa la letra
        for (int j = 0; j < 8; j++) {
            // cada dígito tiene 8 pixels de ancho
            for (int i = 0; i < 2; i++) {
                int value = juego.gameData(data);
                for (int k = 0; k < 4; k++) {
                    juego.cpc6128.setMode1Pixel(x + 4 * i + k, y + j, CPC6128.unpackPixelMode1(value, k));
                }
                data++;
            }

            // si no había que mostrar ningún dígito, mantiene el puntero en los pixels negros
            if (digito == 0) {
                data = data - 2;
            }
        }
    }

    // realiza el efecto de scroll en la parte del marcador que muestra el momento del día
    void realizaScrollMomentoDia() {
        // si todavía quedan posiciones para desplazar
        if (numPosScrollDia != 0) {
            numPosScrollDia--;

            int caracter = 0x20;

            // en las 2 primeras posiciones del scroll, se ponen caracteres de espacio
            if (numPosScrollDia < 7) {
                caracter = juego.gameData(nombreMomentoDia);
                nombreMomentoDia++;
            }

            // 8 líneas de alto
            juego.cpc6128.scrollLine(9, 180, 7);

            // imprime el caracter que toca
            imprimirCaracter(caracter, 84, 180, 3, 2);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con el obsequium
    /////////////////////////////////////////////////////////////////////////////

    void dibujaObsequium(int obsequium) {
        dibujaBarra(obsequium, 2, 240, 177);

        // dibuja la parte de la barra correspondiente a la vida que no tenemos
        dibujaBarra(31 - obsequium + 1, 3, 240 + obsequium, 177);
    }

    // dibuja una barra de la longitud y color especificados
    void dibujaBarra(int lgtud, int color, int x, int y) {
        if (lgtud != 0) {
            juego.cpc6128.fillMode1Rect(x, y, lgtud, 6, color);
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // dibujo del marcador
    /////////////////////////////////////////////////////////////////////////////

    // limpia el área que ocupa el marcador
    void limpiaAreaMarcador() {
        juego.cpc6128.fillMode1Rect(0, 160, 320, 40, 3);
    }

    // dibuja el marcador
    void dibujaMarcador() {
        // apunta a los datos gráficos del marcador
        int data = 0x1e328;

        // dibuja las 32 líneas que forman el marcador en la parte inferior de la pantalla
        for (int j = 0; j < 32; j++) {
            for (int i = 0; i < 256 / 4; i++) {
                int value = juego.gameData(data);
                for (int k = 0; k < 4; k++) {
                    juego.cpc6128.setMode1Pixel(32 + 4 * i + k, 160 + j, CPC6128.unpackPixelMode1(value, k));
                }
                data++;
            }
        }
    }

    // dibuja los objetos que tenemos en el marcador
    void dibujaObjetos(int objetos, int mascara) {
        int posX = 100;
        int posY = 176;
        Sprite[] sprites = juego.sprites;

        // recorre los 6 huecos posibles
        for (int numHuecos = 0; numHuecos < 6; numHuecos++) {
            // si se han procesado todos los objetos que había que actualizar, sale
            if (mascara == 0) {
                return;
            }

            // averigua si hay que comprobar el objeto actual
            if ((mascara & (1 << (Juego.numObjetos - 1))) != 0) {
                // si tenemos el objeto, lo dibuja
                if ((objetos & (1 << (Juego.numObjetos - 1))) != 0) {
                    Sprite spr = sprites[Juego.primerSpriteObjetos + numHuecos];

                    // obtiene un puntero a los gráficos del objeto
                    int data = spr.despGfx;

                    // dibuja el objeto
                    for (int j = 0; j < spr.alto; j++) {
                        for (int i = 0; i < spr.ancho; i++) {
                            int value = juego.gameData(data);
                            for (int k = 0; k < 4; k++) {
                                juego.cpc6128.setMode1Pixel(posX + 4 * i + k, posY + j,
                                        CPC6128.unpackPixelMode1(value, k));
                            }
                            data++;
                        }
                    }
                } else {
                    // en otro caso, limpia el hueco (12x16 pixels)
                    for (int j = 0; j < 12; j++) {
                        for (int i = 0; i < 16; i++) {
                            juego.cpc6128.setMode1Pixel(posX + i, posY + j, 0);
                        }
                    }
                }
            }

            // pasa al siguiente objeto
            mascara = mascara << 1;
            objetos = objetos << 1;

            // avanza la posición al siguiente hueco
            posX += 20;

            // al pasar del tercer al cuarto hueco, hay 4 pixels extra
            if (numHuecos == 2) {
                posX += 4;
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // dibujo de frases
    /////////////////////////////////////////////////////////////////////////////

    // limpia la zona del marcador en donde se muestran las frases
    void limpiaAreaFrases() {
        juego.cpc6128.fillMode1Rect(96, 164, 128, 8, 3);
    }

    // recorre los caracteres de la frase, mostrándolos por pantalla
    void imprimeFrase(String frase, int x, int y, int colorTexto, int colorFondo) {
        for (int i = 0; i < frase.length(); i++) {
            imprimirCaracter(frase.charAt(i), x + 8 * i, y, colorTexto, colorFondo);
        }
    }

    void imprimirCaracter(int caracter, int x, int y, int colorTexto, int colorFondo) {
        // se asegura de que el caracter esté entre 0 y 127
        caracter &= 0x7f;

        // si es un caracter no imprimible, sale
        if ((caracter != 0x20) && (caracter < 0x2d)) {
            return;
        }

        // inicialmente se apunta a los datos del espacio en blanco
        int data = BLANK_ADDR;

        // si el caracter no es un espacio en blanco, modifica el puntero a los datos del caracter
        if (caracter != 0x20) {
            data = CHARSET_ADDR + 8 * (caracter - 0x2d);
        }

        // cada caracter es de 8x8 pixels
        for (int j = 0; j < 8; j++) {
            int bit = 0x80;
            int valor = juego.gameData(data);

            for (int i = 0; i < 8; i++) {
                juego.cpc6128.setMode1Pixel(x + i, y + j, ((valor & bit) != 0) ? colorTexto : colorFondo);
                bit = bit >> 1;
            }
            data++;
        }
    }

    // genera el efecto de la espiral
    void dibujaEspiral() {
        dibujaEspiral(3);    // dibuja la espiral
        dibujaEspiral(0);    // borra la espiral
    }

    // dibuja una espiral cuadrada del color que se le pasa
    private void dibujaEspiral(int color) {
        // fija la posición inicial
        int posX = 0;
        int posY = 0;

        // fija la longitud de las tiras
        int horizontal = 0x3f;
        int vertical = 0x4f;

        int colorAUsar = 0;

        // milisegundos que esperar para ver el efecto
        int retardo = 4;

        // repite 32 veces
        for (int i = 0; i < 32; i++) {
            int num = horizontal;
            if (i != 0) horizontal--;

            // dibuja una tira (de izquierda a derecha) del ancho indicado por derecha
            for (int j = 0; j < num; j++) {
                dibujaBloque(posX, posY, colorAUsar);
                posX++;
            }

            // espera un poco para que se vea el resultado
            juego.timer.sleep(retardo);

            num = vertical;
            vertical--;

            // dibuja una tira (de arriba a abajo) del alto indicado por abajo
            for (int j = 0; j < num; j++) {
                dibujaBloque(posX, posY, colorAUsar);
                posY += 2;
            }

            // espera un poco para que se vea el resultado
            juego.timer.sleep(retardo);

            num = horizontal;
            horizontal--;

            // dibuja una tira (de derecha a izquierda) del ancho indicado por izquierda
            for (int j = 0; j < num; j++) {
                dibujaBloque(posX, posY, colorAUsar);
                posX--;
            }

            // espera un poco para que se vea el resultado
            juego.timer.sleep(retardo);

            num = vertical;
            vertical--;

            // dibuja una tira (de abajo a arriba) del alto indicado por arriba
            for (int j = 0; j < num; j++) {
                dibujaBloque(posX, posY, colorAUsar);
                posY -= 2;
            }

            // espera un poco para que se vea el resultado
            juego.timer.sleep(retardo);

            // invierte el color a usar
            colorAUsar ^= color;

            if ((i != 0) && ((i % 8) == 0)) {
                retardo--;
            }
        }

        dibujaBloque(posX, posY, colorAUsar);
    }

    // dibuja un bloque de 4x8 del color que se le pasa
    private void dibujaBloque(int posX, int posY, int color) {
        for (int i = 0; i < 4; i++) {
            juego.cpc6128.setMode1Pixel(32 + posX * 4 + i, posY, color);
            juego.cpc6128.setMode1Pixel(32 + posX * 4 + i, posY + 1, color);
        }
    }

}
