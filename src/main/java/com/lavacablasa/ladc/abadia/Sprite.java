package com.lavacablasa.ladc.abadia;

class Sprite {
    boolean esVisible;      // indica si el sprite es visible
    boolean haCambiado;     // indica si el sprite ha cambiado desde que se dibujó por última vez
    boolean seHaProcesado;  // indica si el sprite ha sido procesado por el mezclador
    int profundidad;        // profundidad del sprite (coordenada y de pantalla)

    int posXPant;           // coordenada x del sprite en la pantalla (en múltiplos de 4 pixels)
    int posYPant;           // coordenada y del sprite en la pantalla (en pixels)
    int oldPosXPant;        // anterior coordenada x del sprite en la pantalla (en múltiplos de 4 pixels)
    int oldPosYPant;        // anterior coordenada y del sprite en la pantalla (en pixels)

    boolean desaparece;     // indica si el sprite va a desaparecer de la pantalla
    int ancho;              // ancho del sprite (en múltiplos de 4 pixels)
    int alto;               // alto del sprite (en pixels)
    int despGfx;            // desplazamiento a los gráficos que forman el sprite
    int oldAncho;           // anterior ancho del sprite (en bytes)
    int oldAlto;            // anterior alto del sprite (en pixels)

    int posXTile;           // coordenada x del tile en el que se empieza a dibujar el sprite (en múltiplos de 4 pixels)
    int posYTile;           // coordenada y del tile en el que se empieza a dibujar el sprite (en pixels)
    int anchoFinal;         // ancho final del área a dibujar del sprite (en bytes)
    int altoFinal;          // alto final del área a dibujar del sprite (en pixels)
    int despBuffer;         // desplazamiento en el buffer para mezclar los sprites
    int posXLocal;          // coordenada x en la coordenadas locales de la cámara
    int posYLocal;          // coordenada y en la coordenadas locales de la cámara

    /////////////////////////////////////////////////////////////////////////////
    // ajuste de las dimensiones para el dibujado
    /////////////////////////////////////////////////////////////////////////////

    // dadas la posición y dimensiones del sprite, calcula la posición y dimensiones ampliados a los tiles que ocupa (x en bytes, y en tiles)
    void ajustaATiles()
    {
        // calcula la posición inicial del tile que contiene al sprite
        posXTile = posXPant & 0xfc;
        posYTile = posYPant & 0xf8;

        // calcula la posición del sprite dentro del tile
        int despXTile = posXPant & 0x03;
        int despYTile = posYPant & 0x07;

        // calcula la dimensión ampliada del sprite para que abarque todos los tiles en los que se va a dibujar
        anchoFinal = (ancho + despXTile + 3) & 0xfc;
        altoFinal = (alto + despYTile + 7) & 0xf8;
    }

    // amplia las dimensiones a dibujar para que se redibuje el área ocupada anteriormente por el sprite
    void ampliaDimViejo()
    {
        // ajusta en x

        // halla la distancia en x entre el origen del tile en el que empieza el sprite y el origen del sprite anterior
        int difX = posXTile - oldPosXPant;

        // si empieza primero el sprite antiguo
        if (difX >= 0){
            // obtiene la máxima anchura del sprite antiguo que se cubre con el ancho ampliado actual
            int anchoCubierto = difX + anchoFinal;

            // obtiene el mínimo ancho que debe cubrirse para limpiar el sprite antiguo
            int oldAnchoAmpliado = oldAncho;

            // si el sprite antiguo termina antes que el área cubierta, amplia el ancho del sprite antiguo
            if (anchoCubierto >= oldAnchoAmpliado){
                oldAnchoAmpliado = anchoCubierto;
            }

            // como empieza primero el sprite antiguo, cambia la posición inicial del tile y amplia su ancho
            posXTile = oldPosXPant & 0xfc;
            int oldDespXTile = oldPosXPant & 0x03;
            anchoFinal = (oldAnchoAmpliado + oldDespXTile + 3) & 0xfc;
        } else {
            // si empieza primero el sprite actual

            // obtiene la máxima anchura que ocupa el sprite antiguo dentro del sprite ampliado
            int anchoCubierto = -difX + oldAncho;

            // si el ancho ampliado no cubre el ancho del sprite viejo, amplía el ancho
            if (anchoFinal < anchoCubierto){
                anchoFinal = (anchoCubierto + 3) & 0xfc;
            }
        }

        // ajusta en y

        // halla la distancia en y entre el origen del tile en el que empieza el sprite y el origen del sprite anterior
        int difY = posYTile - oldPosYPant;

        // si empieza primero el sprite antiguo
        if (difY >= 0){
            // obtiene la máxima altura del sprite antiguo que se cubre con el alto ampliado actual
            int altoCubierto = difY + altoFinal;

            // obtiene el mínimo alto que debe cubrirse para limpiar el sprite antiguo
            int oldAltoAmpliado = oldAlto;

            // si el sprite antiguo termina antes que el área cubierta, amplia el alto del sprite antiguo
            if (altoCubierto >= oldAltoAmpliado){
                oldAltoAmpliado = altoCubierto;
            }

            // como empieza primero el sprite antiguo, cambia la posición inicial del tile y amplia su alto
            posYTile = oldPosYPant & 0xf8;
            int oldDespYTile = oldPosYPant & 0x07;
            altoFinal = (oldAltoAmpliado + oldDespYTile + 7) & 0xf8;
        } else {
            // si empieza primero el sprite actual

            // obtiene la máxima altura que ocupa el sprite antiguo dentro del sprite ampliado
            int altoCubierto = -difY + oldAlto;

            if (altoFinal < altoCubierto){
                // si el alto ampliado no cubre el alto del sprite viejo, amplía el alto
                altoFinal = (altoCubierto + 7) & 0xf8;
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // dibujado del sprite
    /////////////////////////////////////////////////////////////////////////////

    // dibuja la parte visible del sprite actual en el área ocupada por el sprite que se le pasa como parámetro
    void dibuja(Juego juego, Sprite spr, byte[] bufferMezclas, int lgtudClipX, int lgtudClipY, int dist1X, int dist2X, int dist1Y, int dist2Y)
    {
        // calcula la dirección de inicio de los gráficos visibles del sprite a mezclar en el área ocupada por el sprite que se está procesando
        int despSrc = despGfx + dist2Y*ancho + dist2X;

        // calcula la dirección de destino de los gráficos en el buffer de sprites
        int despDest = spr.despBuffer + (dist1Y*spr.anchoFinal + dist1X)*4;

        // recorre los pixels visibles en Y
        for (int lgtudY = 0; lgtudY < lgtudClipY; lgtudY++){
            int src = despSrc;
            int dest = despDest;

            // recorre los pixels visibles en X
            for (int lgtudX = 0; lgtudX < lgtudClipX; lgtudX++){
                // lee un byte del gráfico (4 pixels)
                int data = juego.gameData(src);

                // para cada pixel del byte leido
                for (int k = 0; k < 4; k++){
                    // obtiene el color del pixel
                    int color = juego.cpc6128.unpackPixelMode1(data, k);

                    // si no es un pixel transparente lo copia al destino
                    if (color != 0){
					    bufferMezclas[dest] = (byte) color;
                    }
                    dest++;
                }
                src++;
            }

            despSrc += ancho;
            despDest += spr.anchoFinal*4;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda
    /////////////////////////////////////////////////////////////////////////////

    // pone la posición y dimensiones actuales como posición y dimensiones antiguas
    void preparaParaCambio()
    {
        oldPosXPant = posXPant;
        oldPosYPant = posYPant;
        oldAncho = ancho;
        oldAlto = alto;
    }}
