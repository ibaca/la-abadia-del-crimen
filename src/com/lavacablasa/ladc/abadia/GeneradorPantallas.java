package com.lavacablasa.ladc.abadia;

import com.lavacablasa.ladc.core.Promise;

//    Clase que se encarga de generar los bloques que forman las pantallas, calcular la zona que
//    tapa el bloque y grabar toda esa información en la capas que forman el buffer de tiles.
//    Además, esta clase se encarga de dibujar las pantallas una vez que se ha rellenado el buffer
//    de tiles.
//
/////////////////////////////////////////////////////////////////////////////
class GeneradorPantallas {

    static final int NIVELES_PROF_TILES = 6;

    // tabla con los manejadores para cada comando
    private static final int TILES_BASE = 0x8300;
    private static final int BLOCK_TABLE = 0x156d;
    private static final int DRAW_DELAY = 100;

    private static final int PARAM_1 = 0;
    private static final int PARAM_2 = 1;
    private static final int HEIGHT = 2;
    private static final int DEPTH_X = 3;
    private static final int DEPTH_Y = 4;

    static class TileInfo {
        int[] profX = new int[NIVELES_PROF_TILES];    // profundidad del tile en x (en coordenadas locales)
        int[] profY = new int[NIVELES_PROF_TILES];    // profundidad del tile en y (en coordenadas locales)
        int[] tile = new int[NIVELES_PROF_TILES];     // número de tile
    }

    final Juego juego;

    TileInfo[][] bufferTiles = new TileInfo[20][16];    // buffer de tiles (16x20 tiles)
    int[][] mascaras = new int[4][4];                   // tablas de máscaras and y or para cada uno de los colores

    int comandosBloque;                                 // desplazamiento a los datos de los comandos que forman un bloque
    int[] datosBloque = new int[5];                     // buffer donde guarda los datos para construir el bloque actual
    int tilesBloque;
    int tilePosX, tilePosY;                             // posición actual en el buffer de tiles
    boolean cambioSistemaCoord;                         // indica si se ha cambiado el sistema de coordenadas
    int[] estadoOpsX = new int[2];                      // usado para cambiar el sentido de las x en algunas operaciones

    int[] pila = new int[64];                            // pila para evaluar los comandos de generación de bloques
    int posPila;                                         // posición actual de la pila

    GeneradorPantallas(Juego juego) {
        this.juego = juego;
        cambioSistemaCoord = false;
        estadoOpsX[0] = 1;
        estadoOpsX[1] = 0;

        for (int i = 0; i < bufferTiles.length; i++) {
            for (int j = 0; j < bufferTiles[i].length; j++) {
                bufferTiles[i][j] = new TileInfo();
            }
        }

        // genera las máscaras para combinar los pixels
        generaMascaras();
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de generación de bloques
    //
    // Los bloques puede tener 3 o 4 bytes de longitud. El formato del bloque es:
    //        byte 0:
    //            bits 7-1: tipo del bloque a construir
    //            bit 0: si es 1 indica que el bloque puede ocultar a los sprites
    //        byte 1:
    //            bits 7-5: parámetro 1 (su función depende del tipo de bloque a construir)
    //            bits 4-0: posición inicial en x (sistema de coordenadas del buffer de tiles)
    //        byte 2:
    //            bits 7-5: parámetro 2 (su función depende del tipo de bloque a construir)
    //            bits 4-0: posición inicial en y (sistema de coordenadas del buffer de tiles)
    //        byte 3: altura inicial del bloque
    //
    /////////////////////////////////////////////////////////////////////////////

    // genera los capas de tiles que forman una pantalla dados los datos de los bloques que la forman
    void genera(int datosPantalla) {
        // inicia la pila
        posPila = 0;

        // repite el proceso de generación de bloques hasta que no se encuentre el marcador de fin de datos
        while (juego.gameData(datosPantalla) != 0xff) {

            // los 7 bits más significativos del primer byte de los datos indican el tipo de bloque a construir
            int despTipoBloque = juego.gameDataW(BLOCK_TABLE + (juego.gameData(datosPantalla) & 0xfe));

            // lee la posición desde donde se dibujará el bloque
            tilePosX = juego.gameData(datosPantalla + 1) & 0x1f;
            tilePosY = juego.gameData(datosPantalla + 2) & 0x1f;

            // lee los parámetros del bloque
            datosBloque[PARAM_1] = (juego.gameData(datosPantalla + 1) >> 5) & 0x07;
            datosBloque[PARAM_2] = (juego.gameData(datosPantalla + 2) >> 5) & 0x07;

            // inicia la profundidad del bloque en la rejilla a (0, 0)
            datosBloque[DEPTH_X] = datosBloque[DEPTH_Y] = 0;

            int altura = 0xff;

            // si la entrada es de 4 bytes, el cuarto byte indica la altura del bloque, y también implica
            // que se calcularán los datos de profundidad a lo largo del proceso de generación del bloque
            if ((juego.gameData(datosPantalla) & 0x01) != 0) {
                altura = juego.gameData(datosPantalla + 3);
                datosPantalla++;
            }

            // avanza a la siguiente entrada
            datosPantalla += 3;

            // guarda la altura para después
            datosBloque[HEIGHT] = altura;

            // inicia la evaluación del bloque
            iniciaInterpretacionBloque(despTipoBloque, true);
        }
    }

    // realiza la iniciación necesaria para interpretar los datos de un bloque
    void iniciaInterpretacionBloque(int despTipoBloque, boolean modificaTiles) {

        if (modificaTiles) {
            // obtiene un puntero a los tiles que forman el bloque
            this.tilesBloque = juego.gameDataW(despTipoBloque);
        }

        // avanza el desplazamiento hasta los comandos que forman el bloque
        comandosBloque = despTipoBloque + 2;

        int altura = datosBloque[HEIGHT];
        // solo realiza la transformación si el bloque puede ocultar a los sprites
        if (altura != 0xff) {
            // transforma la posición del bloque en el buffer de tiles al sistema de coordenadas de la rejilla
            // las ecuaciones de cambio de sistema de coordenadas son:
            // mapa de tiles . rejilla:		Xrejilla = Ymapa + Xmapa - 15
            //							    Yrejilla = Ymapa - Xmapa + 16
            datosBloque[DEPTH_X] = (tilePosY + altura / 2) + tilePosX - 15;
            datosBloque[DEPTH_Y] = (tilePosY + altura / 2) - tilePosX + 16;
        }

        // comienza a interpretar los comandos
        int comando;
        while ((comando = juego.gameData(comandosBloque)) != 0xff) {
            comandosBloque++;
            switch (comando) {
                case 0xfe:
                    whileReg(PARAM_1);
                    break;
                case 0xfd:
                    whileReg(PARAM_2);
                    break;
                case 0xfc:
                    pushTilePos();
                    break;
                case 0xfb:
                    popTilePos();
                    break;
                case 0xfa:
                    endWhile();
                    break;
                case 0xf9:
                    dibujaTileYMueve(0, -1);
                    break;
                case 0xf8:
                    dibujaTileYMueve(estadoOpsX[0], 0);
                    break;
                case 0xf7:
                    updateReg();
                    break;
                case 0xf6:
                    tilePosY++;
                    break;
                case 0xf5:
                    tilePosX += estadoOpsX[0];
                    break;
                case 0xf4:
                    tilePosY--;
                    break;
                case 0xf3:
                    tilePosX -= estadoOpsX[0];
                    break;
                case 0xf2:
                    tilePosY += evaluaExpresion();
                    break;
                case 0xf1:
                    tilePosX += evaluaExpresion();
                    break;
                case 0xf0:
                    datosBloque[PARAM_1]++;
                    break;
                case 0xef:
                    datosBloque[PARAM_2]++;
                    break;
                case 0xee:
                    datosBloque[PARAM_1]--;
                    break;
                case 0xed:
                    datosBloque[PARAM_2]--;
                    break;
                case 0xec:
                    call();
                    break;
                case 0xeb:
                    dibujaTileYMueve(-1, 0);
                    break;
                case 0xea:
                    comandosBloque = juego.gameDataW(comandosBloque);
                    break;
                case 0xe9:
                case 0xe8:
                case 0xe7:
                case 0xe6:
                case 0xe5:
                    flipX();
                    break;
                case 0xe4:
                    callPreserve();
                    break;
            }
        }
        endBlock();
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda para los comandos de dibujo de los tiles
    /////////////////////////////////////////////////////////////////////////////

    // actualiza los datos de un tile del buffer de tiles
    void actualizaTile(int tile, TileInfo tileDesc) {
        assert ((tile >= 0) && (tile < 0x100));

        int newProfX = datosBloque[DEPTH_X];
        int newProfY = datosBloque[DEPTH_Y];

        for (int i = 1; i < NIVELES_PROF_TILES; i++) {
            // obtiene los valores de mayor profundidad de esa entrada
            int oldProfX = tileDesc.profX[i];
            int oldProfY = tileDesc.profY[i];
            int oldTile = tileDesc.tile[i];

            // si el nuevo elemento que se dibuja en este tile tiene menor profundidad que el elemento que estaba
            // antes en este tile, ajusta la profundidad del elemento anterior a la profundidad del elemento
            if (newProfX < oldProfX && newProfY < oldProfY) {
                oldProfX = newProfX;
                oldProfY = newProfY;
            }

            // pasa los datos anteriores de mayor profundidad a la capa de menor profundidad
            tileDesc.profX[i - 1] = oldProfX;
            tileDesc.profY[i - 1] = oldProfY;
            tileDesc.tile[i - 1] = oldTile;
        }

        // graba los nuevos datos de mayor profundidad
        tileDesc.profX[NIVELES_PROF_TILES - 1] = newProfX;
        tileDesc.profY[NIVELES_PROF_TILES - 1] = newProfY;
        tileDesc.tile[NIVELES_PROF_TILES - 1] = tile;
    }

    // si el tile es visible, actualiza el buffer de tiles
    void grabaTile(int tile) {
        // comprueba si el tile es de la parte central, trasladando la parte central al origen
        int posX = tilePosX - 8;
        if (posX < 0 || posX >= 16) return;

        int posY = tilePosY - 8;
        if (posY < 0 || posY >= 20) return;

        // actualiza la información de esa entrada del tile en el buffer de tiles
        actualizaTile(tile, bufferTiles[posY][posX]);
    }

    /////////////////////////////////////////////////////////////////////////////
    // operaciones sobre la pila
    /////////////////////////////////////////////////////////////////////////////

    // mete un dato en la pila
    void push(int data) {
        pila[posPila] = data;
        posPila++;
    }

    // saca un dato en la pila
    int pop() {
        posPila--;
        return pila[posPila];
    }

    /////////////////////////////////////////////////////////////////////////////
    // operaciones sobre registros y expresiones del generador de bloques
    /////////////////////////////////////////////////////////////////////////////

    int obtenerPosRegistro() {
        // lee un dato del buffer
        int dato = juego.gameData(comandosBloque++);
        return obtenerPosRegistro(dato);
    }

    private int obtenerPosRegistro(int dato) {
        // si se cambió el sentido de las x, intercambia los registros 0x70 y 0x71
        if (dato >= 0x70) {
            dato = dato ^ estadoOpsX[1];
        }

        // devuelve la posicion del registro
        return dato - 0x6d;
    }

    // obtiene un valor inmediato o el contenido de un registro
    int leeDatoORegistro() {
        // lee un dato del buffer
        int dato = juego.gameData(comandosBloque++);

        if (dato <= 0x60) {
            // si el dato es menor o igual que 0x60, es un valor inmediato
            return dato;
        } else if (dato == 0x82) {
            // el 0x82 es un marcador que indica que hay que devolver el siguiente byte
            return juego.gameData(comandosBloque++);
        } else if (dato >= 0x6d) {
            // es un registro de bloque
            return datosBloque[obtenerPosRegistro(dato)];
        } else {
            // devuelve un no. de tile del bloque
            return juego.gameData(tilesBloque + dato - 0x61);
        }
    }

    // evalua una ristra de bytes calculando la expresión generada
    private int evaluaExpresion() {

        // obtiene el valor inicial de la expresión
        int rdo = leeDatoORegistro();

        // evalua una expresión
        while (true) {
            // lee un byte de datos
            int op = juego.gameData(comandosBloque);

            // si se ha terminado la expresión, sale
            if (op >= 0xc8) {
                return rdo;
            }

            // 0x84 indica el cambio de signo de la expresión calculada
            if (op == 0x84) {
                rdo = -rdo;
                comandosBloque++;
            } else {
                // en otro caso, suma un registro o valor inmediato
                rdo += (byte) leeDatoORegistro();
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de dibujo del buffer de tiles
    /////////////////////////////////////////////////////////////////////////////

    // dibuja en pantalla el contenido del buffer de tiles desde el centro hacia fuera
    Promise<Void> dibujaBufferTiles() {
        // posición inicial en el buffer de tiles
        int[] pos = { 7, 8 };

        // fija las variables de recorrido
        var state = new Object() {
            int vertical = 4;
            int horizontal = 1;
        };

        // repite mientras no se complete toda la pantalla visible
        return Promise.doWhile(state, s -> s.vertical < 20, s -> {
            // dibuja 4 tiras: una hacia abajo, otra a la derecha, otra hacia arriba y la otra a la izquierda
            dibujaTira(pos, 0, 1, state.vertical); state.vertical++;
            dibujaTira(pos, 1, 0, state.horizontal); state.horizontal++;
            dibujaTira(pos, 0, -1, state.vertical); state.vertical++;
            dibujaTira(pos, -1, 0, state.horizontal); state.horizontal++;

            // espera un poco para que se vea el resultado
            return juego.context.sleep(DRAW_DELAY).map(s);
        }).map((Void) null);
    }

    // dibuja una tira de tiles
    void dibujaTira(int[] pos, int deltaX, int deltaY, int veces) {
        // para cada tile de la tira
        for (int i = 0; i < veces; i++) {
            // por cada capa de profundidad
            for (int k = 0; k < NIVELES_PROF_TILES; k++) {
                // obtiene el número de tile asociado a esta profundidad del buffer de tiles
                int tile = bufferTiles[pos[1]][pos[0]].tile[k];

                // si hay algún tile, lo dibuja
                if (tile != 0) {
                    dibujaTile(32 + pos[0] * 16, pos[1] * 8, tile);
                }
            }

            // pasa a la siguiente posición
            pos[0] += deltaX;
            pos[1] += deltaY;
        }
    }

    // dibuja un tile de 16x8 en la posición indicada
    void dibujaTile(int x, int y, int num) {
        assert ((num >= 0x00) && (num < 0x100));

        // halla el desplazamiento del tile (cada tile ocupa 32 bytes)
        int tileData = TILES_BASE + num * 32;

        int numTabla = ((num & 0x80) != 0) ? 2 : 0;

        // dibuja cada linea del tile
        for (int j = 0; j < 8; j++) {
            // repite para 4 bytes (16 pixels)
            for (int i = 0; i < 4; i++) {
                // lee un byte del gráfico (4 pixels)
                int data = juego.gameData(tileData);

                // para cada pixel del byte leido
                for (int k = 0; k < 4; k++) {
                    // obtiene el color del pixel
                    int color = CPC6128.unpackPixelMode1(data, k);

                    // obtiene el color del pixel en pantalla
                    int oldColor = juego.cpc6128.getMode1Pixel(x, y);

                    // combina el color del pixel de pantalla con el nuevo
                    color = (oldColor & mascaras[numTabla + 1][color]) | mascaras[numTabla][color];

                    // pinta el color resultante
                    juego.cpc6128.setMode1Pixel(x, y, color);

                    // avanza al siguiente pixel
                    x++;
                }

                // avanza la posición del gráfico
                tileData++;
            }
            // pasa a la siguiente línea de pantalla
            x -= 16;
            y++;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda
    /////////////////////////////////////////////////////////////////////////////

    // genera las máscaras necesarias para combinar los gráficos
    void generaMascaras() {
        // rellena las tablas de las máscaras
        for (int i = 0; i < 4; i++) {
            int bit0 = (i >> 0) & 0x01;
            int bit1 = (i >> 1) & 0x01;

            // tabla de máscaras or (0.0, 1.1, 2.0, 3.3)
            mascaras[0][i] = ((bit1 & bit0) << 1) | bit0;

            // tabla de máscaras and (0.0, 1.0, 2.3, 3.0)
            mascaras[1][i] = (((bit1 ^ bit0) & bit1) << 1) | ((bit1 ^ bit0) & bit1);

            // tabla de máscaras or (0.0, 1.0, 2.2, 3.3)
            mascaras[2][i] = ((bit1) << 1) | (bit1 & bit0);

            // tabla de máscaras and (0.0, 1.3, 2.0, 3.0)
            mascaras[3][i] = (((bit1 ^ bit0) & bit0) << 1) | ((bit1 ^ bit0) & bit0);
        }
    }

    // prepara el buffer de tiles y limpia el área de juego
    void limpiaPantalla(int color) {
        // limpia el buffer de tiles
        for (int j = 0; j < 20; j++) {
            for (int i = 0; i < 16; i++) {
                for (int k = 0; k < NIVELES_PROF_TILES; k++) {
                    bufferTiles[j][i].profX[k] = 0;
                    bufferTiles[j][i].profY[k] = 0;
                    bufferTiles[j][i].tile[k] = 0;
                }
            }
        }

        // limpia el área de juego
        juego.cpc6128.fillMode1Rect(32, 0, 256, 160, color);
    }

    /////////////////////////////////////////////////////////////////////////////
    // comandos
    /////////////////////////////////////////////////////////////////////////////

    private void call(boolean modificaTiles) {
        // obtiene un puntero a las características del bloque
        int despTipoBloque = juego.gameDataW(comandosBloque);

        // guarda el estado necesario para reanudar la interpretación del bloque
        push(tilePosX);
        push(tilePosY);

        push(estadoOpsX[0]);
        push(estadoOpsX[1]);

        push(datosBloque[PARAM_1]);
        push(datosBloque[PARAM_2]);
        push(datosBloque[HEIGHT]);
        push(datosBloque[DEPTH_X]);
        push(datosBloque[DEPTH_Y]);

        push(comandosBloque + 2);

        // interpreta otro bloque
        iniciaInterpretacionBloque(despTipoBloque, modificaTiles);

        // recupera los valores introducidos en la pila
        comandosBloque = pop();

        datosBloque[DEPTH_Y] = pop();
        datosBloque[DEPTH_X] = pop();
        datosBloque[HEIGHT] = pop();
        datosBloque[PARAM_2] = pop();
        datosBloque[PARAM_1] = pop();

        estadoOpsX[1] = pop();
        estadoOpsX[0] = pop();

        tilePosY = pop();
        tilePosX = pop();
    }

    private void call() {
        // interpreta otro bloque modificando los tiles que se usan
        call(true);
    }

    private void callPreserve() {
        // indica que se ha cambiado el sentido de las x
        cambioSistemaCoord = true;

        // interpreta otro bloque sin modificar los tiles que se usan
        call(false);
    }

    // método de ayuda para los bucles
    private void avanzaHastaFinDeWhile() {
        int profWhile = 1;

        // mientras no se hayan pasado las instrucciones del while
        while (profWhile > 0) {
            int dato = juego.gameData(comandosBloque);

            // si encuentra un marcador, avanza 2 bytes
            if (dato == 0x82) {
                comandosBloque += 2;
            } else {
                // en otro caso, sigue pasando y contando los while a los que entra y a los que sale
                if ((dato == 0xfe) || (dato == 0xfd)) {
                    profWhile++;
                } else if (dato == 0xfa) {
                    profWhile--;
                }

                comandosBloque++;
            }
        }
    }

    private void whileReg(int reg) {
        // devuelve el dato que había en el registro
        int aux = datosBloque[reg];

        // si el bucle se va a ejecutar alguna vez, inserta en la pila la dirección de retorno y el valor actual del parámetro 1
        if (aux > 0) {
            push(comandosBloque);
            push(aux);
        } else {
            // en otro caso, salta las instrucciones hasta el fín del while
            avanzaHastaFinDeWhile();
        }
    }

    private void endWhile() {
        // recupera el contador del bucle
        int contador = pop();
        contador--;

        // si no se ha terminado todavía
        if (contador > 0) {
            // recupera la dirección de inicio del while
            comandosBloque = pop();

            // inserta en la pila la dirección de retorno y el contador
            push(comandosBloque);
            push(contador);
        } else {
            // en otro caso se limpia la pila
            pop();
        }
    }

    private void endBlock() {
        boolean seCambioSistemaCoord = cambioSistemaCoord;

        cambioSistemaCoord = false;

        // si se empezó a trabajar con respecto al nuevo sistema de coordenadas
        if (!seCambioSistemaCoord) {
            estadoOpsX[0] = 1;
            estadoOpsX[1] = 0;
        }
    }

    private void pushTilePos() {
        push(tilePosX);
        push(tilePosY);
    }

    private void popTilePos() {
        tilePosY = pop();
        tilePosX = pop();
    }

    // dibuja un tile en el buffer de tiles (si es visible), cambiando la posición actual en el buffer
    private void dibujaTileYMueve(int deltax, int deltay) {
        while (true) {
            // lee el siguiente operando del buffer de construcción del bloque
            int num = leeDatoORegistro();

            // lee el próximo byte a procesar
            int dato = juego.gameData(comandosBloque);

            // si se encuentra una nueva orden, pinta, actualiza la posición y sale
            if (dato >= 0xc8) {
                grabaTile(num);
                tilePosX += deltax;
                tilePosY += deltay;
                break;
            }

            comandosBloque++;

            // si se encuentra un 0x80, pinta, actualiza la posición y continúa
            if (dato == 0x80) {
                grabaTile(num);
                tilePosX += deltax;
                tilePosY += deltay;
            } else if (dato == 0x81) {
                // si lee 0x81, pinta y continúa
                grabaTile(num);
            } else {
                // lee el número de veces que ha de repetir la operación
                int numVeces = leeDatoORegistro();

                // repite la misma operación las veces leidas
                for (int i = 0; i < numVeces; i++) {
                    grabaTile(num);
                    tilePosX += deltax;
                    tilePosY += deltay;
                }

                // lee el próximo byte a procesar
                dato = juego.gameData(comandosBloque);

                // si se encuentra una nueva orden, sale
                if (dato >= 0xc8) {
                    break;
                } else {
                    // en otro caso se salta algo y continúa
                    comandosBloque++;
                }
            }
        }
    }

    public void flipX() {
        estadoOpsX[0] = -estadoOpsX[0];
        estadoOpsX[1] ^= 0x01;
    }

    private void updateReg() {
        // obtiene la posición del registro que se va a modificar
        int posReg = obtenerPosRegistro();

        int rdo = evaluaExpresion();

        // si se modifica un registro de coordenadas locales de la rejilla, ajusta el resultado entre 0 y 100
        if (posReg >= DEPTH_X) {
            // si no se estaban calculando las coordenadsa locales de la rejilla para este bloque, sale
            if (datosBloque[posReg] == 0) {
                return;
            }

            // si hay desbordamiento, modifica el resultado
            if (rdo > 100) {
                rdo = 0;
            }
        }

        // actualiza el registro
        datosBloque[posReg] = rdo;
    }
}
