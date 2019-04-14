package com.lavacablasa.ladc.core;

import com.lavacablasa.ladc.abadia.CPC6128;
import com.lavacablasa.ladc.abadia.DskReader;
import com.lavacablasa.ladc.abadia.Juego;

//	"La abadía del crimen"
//
//	Dedicado a la memoria de Paco Menendez
//
//	Reingeniería inversa por Manuel Abadía <vigasoco@gmail.com>
//
//	Notas de la conversión:
//	=======================
//		* la conversión se ha hecho trabajando sobre la versión de Amstrad CPC 6128, que fue
//		el ordenador en el que se creó originalmente el juego, entendiendo el código del Z80
//		y creando una serie de objetos que interactuan entre si para que el resultado sea
//		equivalente al juego original.
//
//		* el juego original tiene 2 capas de tiles para generar las pantallas y crear un efecto
//		de profundidad. Para una gran parte de las pantallas, estas 2 capas son suficiente,
//		aunque para algunas pantallas con gran cantidad de bloques que coinciden en los mismos
//		tiles, como la pantalla 0x34, esto provoca ligeras imperfecciones gráficas. Probablemente
//		en el juego original no se usaron más capas porque cada capa adicional supone 960 bytes,
//		una pasada más a la hora de dibujar la pantalla y una pasada más en el bucle interior del
//		dibujado de sprites, y este incremento en memoria y cálculos consigue una mejora gráfica
//		muy pequeña. Aprovechando que ahora los ordenadores son más potentes, he generalizado los
//		algoritmos que tratan con las capas de forma que el número de capas que se usan viene
//		determinado por la constante nivelesProfTiles de la clase GeneradorPantallas (que por
//		defecto vale 3, para que las pantallas se vean sin errores gráficos).
//
//		* el juego posee un intérprete para la construcción de los bloques que forman las
//		pantallas. Actualmente se interpretan los bloques ya compilados (que están en los
//		datos originales), aunque estaría bien crear ficheros con los scripts de cada bloque
//		y un compilador de bloques, de forma que se interprete el código que genere el
//		compilador de bloques en vez del que viene en los datos originales.
//
//		* el comportamiento de los personajes se basa en el intérprete de scripts que trae el
//		juego original. En la conversión, el comportamiento se ha pasado directamente a C++.
//
//	Por hacer:
//	==========
//		* añadir sonido
//
//		* cargar/grabar partidas
//
/////////////////////////////////////////////////////////////////////////////
public class LaAbadiaDelCrimen {
    private static final int NUM_INTERRUPTS_PER_SECOND = 300;
    private static final int NUM_INTERRUPTS_PER_VIDEO_UPDATE = 6;
    private static final int NUM_INTERRUPTS_PER_LOGIC_UPDATE = 1;

    // fields
    private final GameContext context;
    private final Thread asyncThread;
    private final TimingHandler timingHandler;
    private final Juego abadiaGame;
    private final CPC6128 cpc6128;

    public LaAbadiaDelCrimen(GameContext context) {
        this.context = context;
        this.cpc6128 = new CPC6128(context.getGfxOutput());

        byte[] diskData = context.load("/abadia.dsk");
        byte[] memoryData = readDiskImageToMemory(diskData);

        // creates the timing handler
        timingHandler = new TimingHandler(new Timer(),
                NUM_INTERRUPTS_PER_SECOND,
                NUM_INTERRUPTS_PER_VIDEO_UPDATE,
                NUM_INTERRUPTS_PER_LOGIC_UPDATE);

        // crea el objeto del juego
        abadiaGame = new Juego(memoryData, cpc6128, context, timingHandler);

        // creates the async thread
        asyncThread = new Thread(abadiaGame::run);
    }

    private byte[] readDiskImageToMemory(byte[] diskImageData) {
        byte[] auxBuffer = new byte[0xff00];

        // reserva espacio para los datos del juego
        byte[] memoryData = new byte[0x24000];

        // extrae los datos del juego de la imagen del disco
        DskReader dsk = new DskReader(diskImageData);

        // obtiene los datos de las pistas 0x01-0x11
        for (int i = 0x01; i <= 0x11; i++) {
            dsk.getTrackData(i, auxBuffer, (i - 0x01) * 0x0f00, 0x0f00);
        }

        // reordena los datos y los copia al destino
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x00000, 0x4000);    // abadia0.bin
        reOrderAndCopy(auxBuffer, 0x4000, memoryData, 0x0c000, 0x4000);    // abadia3.bin
        reOrderAndCopy(auxBuffer, 0x8000, memoryData, 0x20000, 0x4000);    // abadia8.bin
        reOrderAndCopy(auxBuffer, 0xc000, memoryData, 0x04100, 0x3f00);    // abadia1.bin

        // obtiene los datos de las pistas 0x12-0x16
        for (int i = 0x12; i <= 0x16; i++) {
            dsk.getTrackData(i, auxBuffer, (i - 0x12) * 0x0f00, 0x0f00);
        }

        // reordena los datos y los copia al destino
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x1c000, 0x4000);    // abadia7.bin

        // obtiene los datos de las pistas 0x17-0x1b
        for (int i = 0x17; i <= 0x1b; i++) {
            dsk.getTrackData(i, auxBuffer, (i - 0x17) * 0x0f00, 0x0f00);
        }

        // reordena los datos y los copia al destino
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x18000, 0x4000);    // abadia6.bin

        // obtiene los datos de las pistas 0x1c-0x21
        for (int i = 0x1c; i <= 0x21; i++) {
            dsk.getTrackData(i, auxBuffer, (i - 0x1c) * 0x0f00, 0x0f00);
        }

        // reordena los datos y los copia al destino
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x14000, 0x4000);    // abadia5.bin

        // obtiene los datos de las pistas 0x21-0x25
        for (int i = 0x21; i <= 0x25; i++) {
            dsk.getTrackData(i, auxBuffer, (i - 0x21) * 0x0f00, 0x0f00);
        }

        // reordena los datos y los copia al destino
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x08000, 0x4000);    // abadia2.bin

        return memoryData;
    }

    private void reOrderAndCopy(byte[] src, int srcPos, byte[] dst, int dstPos, int size) {
        for (int i = 0; i < size; i++) {
            dst[dstPos + size - i - 1] = src[srcPos + i];
        }
    }

    public void end() {
        asyncThread.interrupt();
    }

    public void run() {
        // start async game logic
        asyncThread.start();

        // main sync loop
        while (true) {
            // waits if necessary before processing this interrupt
            timingHandler.waitThisInterrupt();
            // if we have to process game logic
            if (timingHandler.processLogicThisInterrupt()) {
                // execute sync game logic
                abadiaGame.runSync();
            }

            // if we have to process video
            if (timingHandler.processVideoThisInterrupt()) {
                boolean skipVideo = timingHandler.skipVideoThisInterrupt();
                if (!skipVideo) {
                    // render game screen
                    GfxOutput gfxOutput = context.getGfxOutput();
                    cpc6128.render();
                    gfxOutput.render();
                }
            }

            // end this interrupt processing
            timingHandler.endThisInterrupt();
        }
    }
}
