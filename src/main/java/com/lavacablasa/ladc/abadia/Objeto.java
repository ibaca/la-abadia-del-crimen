package com.lavacablasa.ladc.abadia;

class Objeto extends EntidadJuego {

    // campos
    boolean seEstaCogiendo;        // indica si el objeto se está cogiendo o dejando
    boolean seHaCogido;            // indica si el objeto está disponible o ha sido cogido
    Personaje personaje;           // personaje que tiene el objeto (en el caso de que haya sido cogido)

    Objeto(Juego juego, Sprite sprite) {
        super(juego, sprite);
        seEstaCogiendo = false;
        seHaCogido = false;
        personaje = null;
    }

    /////////////////////////////////////////////////////////////////////////////
    // actualización del entorno cuando un objeto es visible en la pantalla actual
    /////////////////////////////////////////////////////////////////////////////

    // actualiza la posición del sprite dependiendo de su posición con respecto a la cámara
    @Override
    void notificaVisibleEnPantalla(int posXPant, int posYPant, int profundidad) {
        // si el objeto no se ha cogido
        if (!seHaCogido){
            // marca el sprite para dibujar
            sprite.haCambiado = true;
            sprite.esVisible = true;
            sprite.profundidad = profundidad;

            // ajusta la posición del sprite (-8, -8)
            sprite.posXPant = posXPant - 2;
            sprite.posYPant = posYPant - 8;
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos relacionados con coger/dejar los objetos
    /////////////////////////////////////////////////////////////////////////////

    // comprueba si el personaje puede coger el objeto
    boolean seHaCogidoPor(Personaje pers, int mascara) {
        // si el objeto se está cogiendo o dejando, no puede ser cogido
        if (seEstaCogiendo) return false;

        // guarda la posición del objeto
        int posXObj = posX;
        int posYObj = posY;
        int alturaObj = altura;

        // si el objeto está cogido, su posición viene dada por la del personaje que lo tiene
        if (seHaCogido){
            // si el personaje no puede quitar objetos, sale
            if (!pers.puedeQuitarObjetos) return false;

            // obtiene la posición del personaje
            posXObj = personaje.posX;
            posYObj = personaje.posY;
            alturaObj = personaje.altura;
        }

        // comprueba si el personaje está en una posición que permita coger el objeto
        int difAltura = alturaObj - pers.altura;
        if ((difAltura < 0) || (difAltura >= 5)) return false;

        int posXPers = pers.posX + 2*MotorGrafico.tablaDespOri[pers.orientacion][0];
        if (posXObj != posXPers) return false;

        int posYPers = pers.posY + 2*MotorGrafico.tablaDespOri[pers.orientacion][1];
        if (posYObj != posYPers) return false;

        // si el objeto está cogido por un personaje, se lo quita
        if (seHaCogido){
            personaje.objetos = personaje.objetos ^ mascara;
        }

        // si el sprite del objeto es visible, indica que va a desaparecer
        if (sprite.esVisible){
            sprite.haCambiado = true;
            sprite.desaparece = true;
        }

        // guarda el personaje que tiene el objeto, indica que el objeto se ha cogido e inicia el contador
        personaje = pers;
        seHaCogido = true;
        seEstaCogiendo = true;
        pers.contadorObjetos = 0x10;
        pers.objetos = pers.objetos | mascara;

        return true;
    }

    // deja el objeto que tenía el personaje en la posición indicada
    void dejar(Personaje pers, int mascara, int posXObj, int posYObj, int alturaObj) {
        // guarda la posición y orientación del objeto e indica que ya no está cogido
        posX = posXObj;
        posY = posYObj;
        altura = alturaObj;
        orientacion = pers.orientacion ^ 0x02;
        seHaCogido = false;
        personaje = null;

        // inicia el contador para coger/dejar objetos y le quita el objeto al personaje
        pers.contadorObjetos = 0x10;
        pers.objetos = pers.objetos & (~mascara);

        // salta a la rutina de redibujado de objetos para redibujar solo el objeto que se deja
        // actualiza la posición del sprite según la cámara
        if (!juego.motor.actualizaCoordCamara(this)) {
            sprite.esVisible = false;
        }

        sprite.oldPosXPant = sprite.posXPant;
        sprite.oldPosYPant = sprite.posYPant;
    }

}
