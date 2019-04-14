package com.lavacablasa.ladc.abadia;

class GestorFrases {

    static final String[] frases = {
            "SECRETUM FINIS AFRICAE, MANUS SUPRA AAA IDOLUM AGE PRIMUM ET SEPTIMUM DE QUATUOR",
            "BIENVENIDO A ESTA ABADIA, HERMANO. OS RUEGO QUE ME SIGAIS. HA SUCEDIDO ALGO TERRIBLE",
            "TEMO QUE UNO DE LOS MONJES HA COMETIDO UN CRIMEN. OS RUEGO QUE LO ENCONTREIS ANTES DE QUE LLEGUE BERNARDO GUI, PUES NO DESEO QUE SE MANCHE EL NOMBRE DE ESTA ABADIA",
            "DEBEIS RESPETAR MIS ORDENES Y LAS DE LA ABADIA. ASISTIR A LOS OFICIOS Y A LA COMIDA. DE NOCHE DEBEIS ESTAR EN VUESTRA CELDA",
            "DEJAD EL MANUSCRITO DE VENANCIO O ADVERTIRE AL ABAD",
            "DADME EL MANUSCRITO, FRAY GUILLERMO",
            "LLEGAIS TARDE, FRAY GUILLERMO",
            "ESTA ES VUESTRA CELDA. DEBO IRME",
            "OS ORDENO QUE VENGAIS",
            "DEBEIS ABANDONAR EL EDIFICIO, HERMANO",
            "ADVERTIRE AL ABAD",
            "DEBEMOS IR A LA IGLESIA, MAESTRO",
            "DEBEMOS IR AL REFECTORIO, MAESTRO",
            "PODEIS IR A VUESTRAS CELDAS",
            "NO HABEIS RESPETADO MIS ORDENES. ABANDONAD PARA SIEMPRE ESTA ABADIA",
            "ESCUCHAD HERMANO, HE ENCONTRADO UN EXTRAÑO LIBRO EN MI CELDA",
            "ENTRAD EN VUESTRA CELDA, FRAY GUILLERMO",
            "HA LLEGADO BERNARDO, DEBEIS ABANDONAR LA INVESTIGACION",
            "¿DORMIMOS?, MAESTRO",
            "DEBEMOS ENCONTRAR UNA LAMPARA, MAESTRO",
            "VENID AQUI, FRAY GUILLERMO",
            "HERMANOS, VENANCIO HA SIDO ASESINADO",
            "DEBEIS SABER QUE LA BIBLIOTECA ES UN LUGAR SECRETO. SOLO MALAQUIAS PUEDE ENTRAR. PODEIS IROS",
            "OREMOS",
            "HERMANOS, BERENGARIO HA DESAPARECIDO. TEMO QUE SE HAYA COMETIDO OTRO CRIMEN",
            "PODEIS COMER, HERMANOS",
            "HERMANOS, HAN ENCONTRADO A BERENGARIO ASESINADO",
            "VENID, FRAY GUILLERMO, DEBEMOS ENCONTRAR A SEVERINO",
            "DIOS SANTO... HAN ASESINADO A SEVERINO Y LE HAN ENCERRADO",
            "BERNARDO ABANDONARA HOY LA ABADIA",
            "MAÑANA ABANDONAREIS LA ABADIA",
            "ERA VERDAD, TENIA EL PODER DE MIL ESCORPIONES",
            "MALAQUIAS HA MUERTO",
            "SOIS VOS, GUILLERMO... PASAD, OS ESTABA ESPERANDO. TOMAD, AQUI ESTA VUESTRO PREMIO",
            "ESTAIS MUERTO, FRAY GUILLERMO, HABEIS CAIDO EN LA TRAMPA",
            "VENERABLE JORGE, VOS NO PODEIS VERLO, PERO MI MAESTRO LLEVA GUANTES. PARA SEPARAR LOS FOLIOS TENDRIA QUE HUMEDECER LOS DEDOS EN LA LENGUA, HASTA QUE HUBIERA RECIBIDO SUFICIENTE VENENO",
            "SE ESTA COMIENDO EL LIBRO, MAESTRO",
            "DEBEIS ABANDONAR YA LA ABADIA",
            "ES MUY EXTRAÑO, HERMANO GUILLERMO. BERENGARIO TENIA MANCHAS NEGRAS EN LA LENGUA Y EN LOS DEDOS",
            "PRONTO AMANECERA, MAESTRO",
            "LA LAMPARA SE AGOTA",
            "HABEIS ENTRADO EN MI CELDA",
            "SE HA AGOTADO LA LAMPARA",
            "JAMAS CONSEGUIREMOS SALIR DE AQUI",
            "ESPERAD, HERMANO",
            "OCUPAD VUESTRO SITIO, FRAY GUILLERMO",
            "ES EL COENA CIPRIANI DE ARISTOTELES. AHORA COMPRENDEREIS POR QUE TENIA QUE PROTEGERLO. CADA PALABRA ESCRITA POR EL FILOSOFO HA DESTRUIDO UNA PARTE DEL SABER DE LA CRISTIANDAD. SE QUE HE ACTUADO SIGUIENDO LA VOLUNTAD DEL SEÑOR... LEEDLO, PUES, FRAY GUILLERMO. DESPUES TE LO MOSTRARE A TI MUCHACHO",
            "FUE UNA BUENA IDEA¿ VERDAD?; PERO YA ES TARDE",
            "QUIERO QUE CONOZCAIS AL HOMBRE MAS VIEJO Y SABIO DE LA ABADIA",
            "VENERABLE JORGE, EL QUE ESTA ANTE VOS ES FRAY GUILLERMO, NUESTRO HUESPED",
            "SED BIENVENIDO, VENERABLE HERMANO; Y ESCUCHAD LO QUE OS DIGO. LAS VIAS DEL ANTICRISTO SON LENTAS Y TORTUOSAS. LLEGA CUANDO MENOS LO ESPERAS. NO DESPERDICIEIS LOS ULTIMOS DIAS",
            "LO SIENTO, VENERABLE HERMANO, NO PODEIS SUBIR A LA BIBLIOTECA",
            "SI LO DESEAIS, BERENGARIO OS MOSTRARA EL SCRIPTORIUM",
            "AQUI TRABAJAN LOS MEJORES COPISTAS DE OCCIDENTE",
            "AQUI TRABAJABA VENANCIO",
            "VENERABLE HERMANO, SOY SEVERINO, EL ENCARGADO DEL HOSPITAL. QUIERO ADVERTIROS QUE EN ESTA ABADIA SUCEDEN COSAS MUY EXTRAÑAS. ALGUIEN NO QUIERE QUE LOS MONJES DECIDAN POR SI SOLOS LO QUE DEBEN SABER"
    };

    volatile boolean mostrandoFrase;// copia de reproduciendoFrase para que el otro thread consulte el estado de las frases
    Juego juego;                    // objeto que presta ayuda para realizar operaciones gráficas del juego.cpc6128
    int contadorActualizacion;      // contador para actualizar la frase que se está poniendo en el marcador
    int espaciosParaFin;            // número de espacios para que la frase haya salido completamente del marcador
    boolean fraseTerminada;         // indica si se terminó una frase
    boolean reproduciendoFrase;     // indica que se está mostrando una frase en el marcador
    String frase;                   // apunta a la frase que se está poniendo en el marcador
    int frasePos;                   // posicion en la frase actual

    GestorFrases(Juego juego) {
        this.juego = juego;
        contadorActualizacion = 0;
        reproduciendoFrase = mostrandoFrase = false;
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos para mostrar una frase
    /////////////////////////////////////////////////////////////////////////////

    // muestra una frase por el marcador siempre y cuando no hubiera otra puesta
    void muestraFrase(int numFrase) {
        // si ya está reproduciendo una frase, sale
        if (mostrandoFrase) {
            return;
        }

        // prepara todo para que se muestre la frase
        dibujaFrase(numFrase);
    }

    // muestra una frase por el marcador. Si había otra, la para
    void muestraFraseYa(int numFrase) {
        // para la frase actual
        reproduciendoFrase = mostrandoFrase = false;

        // limpia la parte del marcador donde se muestran las frases 
        juego.marcador.limpiaAreaFrases();

        // prepara todo para que se muestre la frase
        dibujaFrase(numFrase);
    }

    // actualiza el estado de la reproducción de las frases
    void actualizaEstado() {
        mostrandoFrase = reproduciendoFrase;
    }

    /////////////////////////////////////////////////////////////////////////////
    // dibujo de la frase
    /////////////////////////////////////////////////////////////////////////////

    // inicia el proceso para mostrar una frase por el marcador 
    void dibujaFrase(int numFrase) {
        // inicia la frase
        fraseTerminada = false;
        reproduciendoFrase = mostrandoFrase = true;

        // guarda un puntero a la frase que se va a mostrar
        frase = frases[numFrase];
        frasePos = 0;

        // indica que aún no se ha terminado la frase
        espaciosParaFin = 0;
    }

    void procesaFraseActual() {
        contadorActualizacion++;

        // sólo actualiza las frases 1 vez cada 45 llamadas
        if (contadorActualizacion != 45) {
            return;
        } else {
            contadorActualizacion = 0;
        }

        // si no se está mostrando una frase en el marcador, sale
        if (!reproduciendoFrase) {
            return;
        }

        // si no se ha terminado la frase actual, muestra otro caracter en el marcador
        if (!fraseTerminada) {
            int caracter = frase.charAt(frasePos);

            // cambia los caracteres que no coinciden con el código ASCII
            switch (caracter) {
                case ',':
                    caracter = 0x3c;
                    break;
                case '.':
                    caracter = 0x3d;
                    break;
                case '¿':
                    caracter = 0x40;
                    break;
                case 'Ñ':
                    caracter = 0x57;
                    break;
            }

            frasePos++;

            if (frasePos == frase.length()) {
                fraseTerminada = true;
                espaciosParaFin = 17;
            }

            // realiza el scroll de la frase que se muestra en el marcador e imprime el nuevo carácter
            scrollFrase();
            juego.marcador.imprimirCaracter(caracter, 216, 164, 2, 3);
        } else if (espaciosParaFin != 0) {
            // si la frase se ha terminado y solo se están poniendo espacios para que se borre del marcador
            espaciosParaFin--;

            if (espaciosParaFin != 0) {
                // si el marcador está limpiándose, realiza el scroll y muestra espacios en blanco
                scrollFrase();
                juego.marcador.imprimirCaracter(0x20, 216, 164, 2, 3);
            } else {
                // si llega aquí, la frase ha terminado
                reproduciendoFrase = false;
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////
    // métodos de ayuda
    /////////////////////////////////////////////////////////////////////////////

    void scrollFrase() {
        juego.cpc6128.scrollLine(2 * 12, 164, 16);
    }
}
