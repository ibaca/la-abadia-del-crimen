package com.lavacablasa.ladc.abadia;

class RejillaPantalla {

    private static final int CALCULO_AVANCE_POSICION[][] = {
            {  0, +1,   -1,  0,   +1, -2,   +2, -1 },
            { +1,  0,    0, +1,   -2, -2,   -1, -2 },
            {  0, -1,   +1,  0,   -2, +1,   -2, +1 },
            { -1,  0,    0, -1,   +1, +1,   +1, +2 }
    };

    private static final int[][] INCREMENTOS_BLOQUE = {
        {  1,  0 },
        {  0, -1 },
        { -1,  0 },
        {  0,  1 }
    };

    private static final int[] DATOS_ALTURA_PLANTAS = {0x18a00, 0x18f00, 0x19080};
    private static final int GRID_SIZE = 24;

    Juego juego;
    byte[][] bufAlturas = new byte[GRID_SIZE][GRID_SIZE];     // buffer de alturas de la rejilla (24x24, 1 byte por entrada)

    int minPosX;                                // mínimo valor visible en x para recortar (en coordenadas de mundo)
    int minPosY;                                // mínimo valor visible en y para recortar (en coordenadas de mundo)
    int minAltura;                              // mínimo altura visible (en coordenadas de mundo)

    int[][] bufCalculoAvance = new int[4][4];   // buffer auxiliar para el cálculo del avance del personaje

    RejillaPantalla(Juego juego)
    {
        this.juego = juego;
    }

    // dada la posición de un personaje, calcula los mínimos valores visibles del área de juego
    void calculaMinimosValoresVisibles(Personaje pers)
    {
        minPosX = (pers.posX & 0xf0) - 4;
        minPosY = (pers.posY & 0xf0) - 4;
        minAltura = juego.motor.obtenerAlturaBasePlanta(pers.altura);
    }

    // dado un personaje, rellena la rejilla con la información de altura de la planta recortada para la pantalla
    void rellenaAlturasPantalla(Personaje pers)
    {
        // limpia la matriz de alturas
        for (int j = 0; j < GRID_SIZE; j++){
            for (int i = 0; i < GRID_SIZE; i++){
                bufAlturas[j][i] = 0;
            }
        }

        // obtiene los mínimos valores visibles para la pantalla en la que se encuentra el personaje
        calculaMinimosValoresVisibles(pers);

        // halla el desplazamiento a los datos de la altura para la planta en la que se encuentra el personaje
        int datosAltura = DATOS_ALTURA_PLANTAS[juego.motor.obtenerPlanta(minAltura)];

        // mientras queden datos de altura de la planta
        while (juego.gameData(datosAltura) != 0xff) {
            int tipoBloque = juego.gameData(datosAltura + 0);

            // si el bloque no es de un tipo conocido, sale
            if (((tipoBloque & 0x07) == 0) || ((tipoBloque & 0x07) >= 6)){
                break;
            }

            int lgtudX = juego.gameData(datosAltura + 3);
            int lgtudY = juego.gameData(datosAltura + 4);

            // si la entrada no es de 5 bytes, la longitud se codifica en 4 bits en vez de en 8
            if ((tipoBloque & 0x08) == 0){
                lgtudY = lgtudX & 0x0f;
                lgtudX = (lgtudX >> 4) & 0x0f;
            }

            int altura = (tipoBloque >> 4) & 0x0f;
            int posX = juego.gameData(datosAltura + 1);
            int posY = juego.gameData(datosAltura + 2);

            // avanza a la siguiente entrada
            if ((tipoBloque & 0x08) == 0){
                datosAltura += 4;
            } else {
                datosAltura += 5;
            }

            lgtudX++;
            lgtudY++;

            // rechaza los bloques que están completamente fuera de la zona de pantalla

            // halla la distancia en x entre las coordenadas
            int distX = posX - minPosX;

            // si el bloque empieza antes que el rectángulo de recorte
            if (distX < 0){
                // si el bloque termina antes de que empiece la zona visible
                if (-distX >= lgtudX){
                    continue;
                }
            } else if (distX >= GRID_SIZE){
                // si el bloque empieza después de que termine la zona visible
                continue;
            }

            // halla la distancia en y entre las coordenadas
            int distY = posY - minPosY;

            // si el bloque empieza antes que el rectángulo de recorte
            if (distY < 0){
                // si el bloque termina antes de que empiece la zona visible
                if (-distY >= lgtudY){
                    continue;
                }
            } else if (distY >= GRID_SIZE){
                // si el bloque empieza después de que termine la zona visible
                continue;
            }

            // si llega hasta aquí, alguna parte del bloque es visible, por lo que modifica el buffer de alturas

            // según el tipo de bloque, fija los datos de la altura
            if ((tipoBloque & 0x07) != 5) {
                // modifica la tabla de alturas con los datos del bloque
                for (int j = 0; j < lgtudY; j++) {
                    int oldAltura = altura;

                    for (int i = 0; i < lgtudX; i++) {
                        fijaAlturaRecortando(posX + i, posY + j, altura);
                        altura += INCREMENTOS_BLOQUE[(tipoBloque & 0x07) - 1][0];
                    }

                    altura = oldAltura + INCREMENTOS_BLOQUE[(tipoBloque & 0x07) - 1][1];
                }
            } else {
                // halla la distancia en x entre las coordenadas
                distX = posX - minPosX;

                // si el bloque empieza antes que el rectángulo de recorte
                if (distX < 0) {
                    posX = 0;

                    // si el bloque es más grande que la zona visible, se recorta en longitud
                    if ((distX + lgtudX) > GRID_SIZE) {
                        lgtudX = GRID_SIZE;
                    } else {
                        // en otro caso, recorta la longitud del bloque a la zona visible
                        lgtudX = lgtudX + distX;
                    }
                } else {
                    // si el bloque empieza después del inicio de la zona visible
                    posX = distX;

                    // si el bloque es más grande que la zona visible, recorta la longitud del bloque
                    if ((distX + lgtudX) > GRID_SIZE) {
                        lgtudX = lgtudX - (distX + lgtudX - GRID_SIZE);
                    }
                }

                // halla la distancia en y entre las coordenadas
                distY = posY - minPosY;

                // si el bloque empieza antes que el rectángulo de recorte
                if (distY < 0) {
                    posY = 0;

                    // si el bloque es más grande que la zona visible, se recorta en longitud
                    if ((distY + lgtudY) > GRID_SIZE) {
                        lgtudY = GRID_SIZE;
                    } else {
                        // en otro caso, recorta la longitud del bloque a la zona visible
                        lgtudY = lgtudY + distY;
                    }
                } else {
                    // si el bloque empieza después del inicio de la zona visible
                    posY = distY;

                    // si el bloque es más grande que la zona visible, recorta la longitud del bloque
                    if ((distY + lgtudY) > GRID_SIZE) {
                        lgtudY = lgtudY - (distY + lgtudY - GRID_SIZE);
                    }
                }

                // modifica la tabla de alturas con el bloque recortado
                for (int j = 0; j < lgtudY; j++) {
                    for (int i = 0; i < lgtudX; i++) {
                        bufAlturas[posY + j][posX + i] = (byte) altura;
                    }
                }
            }
        }
    }

    // comprueba si la posición que se le pasa (en coordenadas de mundo) está dentro de las 20x20 posiciones
    // centrales de la rejilla y si es así, devuelve la posición en el sistema de coordenadas de la rejilla
    boolean ajustaAPosRejilla(int posX, int posY, int[] posRejilla)
    {
        posRejilla[0] = posX - minPosX;

        // si está fuera del rango en las x, devuelve false
        if (posRejilla[0] < 2) return false;
        if (posRejilla[0] >= 22) return false;

        posRejilla[1] = posY - minPosY;

        // si está fuera del rango en las y, devuelve false
        if (posRejilla[1] < 2) return false;
        return posRejilla[1] < 22;
    }

    // comprueba si la posición que se le pasa está en las 20x20 posiciones centrales de la rejilla de la
    // pantalla actual, y de ser así, se devuelve su posición en el sistema de coordenadas de la rejilla
    boolean estaEnRejillaCentral(PosicionJuego pos, int[] posRejilla)
    {
        // si la posición no está en la misma planta que la de la rejilla actual, sale
        if (juego.motor.obtenerAlturaBasePlanta(pos.altura) != minAltura){
            return false;
        }

        // si la posición no está en las 20x20 posiciones centrales de la rejilla, sale
        return ajustaAPosRejilla(pos.posX, pos.posY, posRejilla);
    }

    // devuelve la diferencia de altura y posición del personaje si sigue avanzando hacia donde mira
    boolean obtenerAlturaPosicionesAvance(Personaje pers, int[] difAltura, int[] avance)
    {
        // si el personaje no está en la misma planta que la de la rejilla, sale
        if (juego.motor.obtenerAlturaBasePlanta(pers.altura) != minAltura) return false;

        // obtiene la altura relativa con respecto a esta planta
        int alturaLocal = pers.altura - juego.motor.obtenerAlturaBasePlanta(pers.altura);

        return obtenerAlturaPosicionesAvanceComun(pers, alturaLocal, difAltura, avance);
    }

    // devuelve la diferencia de altura y posición del personaje si sigue avanzando hacia donde mira
    boolean obtenerAlturaPosicionesAvance2(Personaje pers, int[] difAltura, int[] avance)
    {
        return obtenerAlturaPosicionesAvanceComun(pers, 0, difAltura, avance);
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda
    /////////////////////////////////////////////////////////////////////////////

    // devuelve la diferencia de altura y posición del personaje si sigue avanzando hacia donde mira
    boolean obtenerAlturaPosicionesAvanceComun(Personaje pers, int alturaLocal, int[] difAltura, int[] avance)
    {
        int[] posLocal = new int[2];

        // si la posición no está dentro de las 20x20 posiciones centrales de la pantalla que se muestra, sale
        if (!estaEnRejillaCentral(pers, posLocal)) return false;

        // calcula la primera posición de la rejilla a probar
        int despIni = pers.enDesnivel ? 6 : 4;
        posLocal[0] += CALCULO_AVANCE_POSICION[pers.orientacion][despIni];
        posLocal[1] += CALCULO_AVANCE_POSICION[pers.orientacion][despIni + 1];

        // rellena el buffer para el cálculo del avance con las posiciones relevantes según la orientación
        for (int j = 0; j < 4; j++){
            int oldPosXLocal = posLocal[0];
            int oldPosYLocal = posLocal[1];

            for (int i = 0; i < 4; i++){
                // obtiene la altura de la posición
                int alturaPos = bufAlturas[posLocal[1]][posLocal[0]] & 0xff;

                if (alturaPos < 0x10){
                    // si no hay un personaje en esa posición, obtiene la diferencia de altura entre la posición y el personaje
                    alturaPos = alturaPos - alturaLocal;
                } else {
                    alturaPos = alturaPos & 0x30;
                }
                bufCalculoAvance[j][i] = alturaPos;

                // apunta a la siguiente posición
                posLocal[0] += CALCULO_AVANCE_POSICION[pers.orientacion][0];
                posLocal[1] += CALCULO_AVANCE_POSICION[pers.orientacion][1];
            }

            // apunta a la siguiente posición
            posLocal[0] = oldPosXLocal + CALCULO_AVANCE_POSICION[pers.orientacion][2];
            posLocal[1] = oldPosYLocal + CALCULO_AVANCE_POSICION[pers.orientacion][3];
        }

        // si el personaje ocupa 4 posiciones en la rejilla
        if (!pers.enDesnivel){
            difAltura[0] = bufCalculoAvance[0][1];
            difAltura[1] = bufCalculoAvance[0][2];

            // si en las 2 posiciones hacia las que quiere avanzar el personaje no hay la misma altura
            if (difAltura[0] != difAltura[1]){
                // indica que hay una diferencia de altura > 1
                difAltura[0] = 2;
            }
        } else {
            // si el personaje ocupa una posición en la rejilla, guarda la diferencia de altura de las 2 posiciones hacia las que quiere avanzar
            difAltura[0] = bufCalculoAvance[1][1];
            difAltura[1] = bufCalculoAvance[0][1];
        }

        // guarda el avance en cada coordenada según la orientación en la que se quiere avanzar
        avance[0] = MotorGrafico.tablaDespOri[pers.orientacion][0];
        avance[1] = MotorGrafico.tablaDespOri[pers.orientacion][1];

        return true;
    }

    // si los datos de altura están dentro de la zona de la rejilla, los graba
    void fijaAlturaRecortando(int posX, int posY, int altura)
    {
        // recorta en y
        posY = posY - minPosY;

        // si la coordenada y está fuera de la zona visible en y, sale
        if ((posY < 0) || (posY >= GRID_SIZE)){
            return;
        }

        // recorta en x
        posX = posX - minPosX;

        // si la coordenada x está fuera de la zona visible en x, sale
        if ((posX < 0) || (posX >= GRID_SIZE)){
            return;
        }

        bufAlturas[posY][posX] = (byte) altura;
    }

}
