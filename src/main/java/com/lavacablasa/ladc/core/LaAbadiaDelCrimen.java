package com.lavacablasa.ladc.core;

import static com.lavacablasa.ladc.core.Promise.doWhile;

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
    private static final int INTERRUPTS_PER_SECOND = 300;
    private static final int INTERRUPTS_PER_VIDEO_UPDATE = 2;
    private static final int INTERRUPTS_PER_LOGIC_UPDATE = 1;

    // fields
    private final GameContext context;
    private final TimingHandler timingHandler;
    private final Juego abadiaGame;
    private final CPC6128 cpc6128;

    public LaAbadiaDelCrimen(GameContext context) {
        this.context = context;
        this.cpc6128 = new CPC6128(context.getGfxOutput());

        byte[] diskData = context.load("/abadia.dsk");
        byte[] memoryData = readDiskImageToMemory(diskData);

        timingHandler = new TimingHandler(INTERRUPTS_PER_VIDEO_UPDATE, INTERRUPTS_PER_LOGIC_UPDATE);
        abadiaGame = new Juego(memoryData, cpc6128, context, timingHandler);
    }

    private byte[] readDiskImageToMemory(byte[] diskImageData) {
        byte[] auxBuffer = new byte[0xff00];
        byte[] memoryData = new byte[0x24000];
        DskReader dsk = new DskReader(diskImageData);

        for (int i = 0; i <= 16; i++) dsk.getTrackData(i + 0x01, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x00000, 0x4000);    // abadia0.bin
        reOrderAndCopy(auxBuffer, 0x4000, memoryData, 0x0c000, 0x4000);    // abadia3.bin
        reOrderAndCopy(auxBuffer, 0x8000, memoryData, 0x20000, 0x4000);    // abadia8.bin
        reOrderAndCopy(auxBuffer, 0xc000, memoryData, 0x04100, 0x3f00);    // abadia1.bin
        for (int i = 0; i <= 4; i++) dsk.getTrackData(i + 0x12, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x1c000, 0x4000);    // abadia7.bin
        for (int i = 0; i <= 4; i++) dsk.getTrackData(i + 0x17, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x18000, 0x4000);    // abadia6.bin
        for (int i = 0; i <= 5; i++) dsk.getTrackData(i + 0x1c, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x14000, 0x4000);    // abadia5.bin
        for (int i = 0; i <= 4; i++) dsk.getTrackData(i + 0x21, auxBuffer, i * 0x0f00, 0x0f00);
        reOrderAndCopy(auxBuffer, 0x0000, memoryData, 0x08000, 0x4000);    // abadia2.bin

        return memoryData;
    }

    private void reOrderAndCopy(byte[] src, int srcPos, byte[] dst, int dstPos, int size) {
        for (int i = 0; i < size; i++) dst[dstPos + size - i - 1] = src[srcPos + i];
    }

    public Promise<?> run() {
        // start async game logic
        Promise<?> gameLogic = abadiaGame.run();

        // main sync loop
        Promise<?> mainLoop = doWhile(() -> timingHandler.sleep((int) ((1. / INTERRUPTS_PER_SECOND) * 1000.))
                .andThen(n -> {
                    timingHandler.interrupt();
                    if (timingHandler.processLogicInterrupt()) abadiaGame.runSync();
                    if (timingHandler.processVideoInterrupt()) {
                        cpc6128.render();
                        context.getGfxOutput().render();
                    }
                    return Promise.of(true);
                }));

        return Promise.merge(gameLogic, mainLoop);
    }
}
