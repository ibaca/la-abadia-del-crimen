package com.lavacablasa.ladc.core;

import com.lavacablasa.ladc.abadia.CPC6128;
import com.lavacablasa.ladc.abadia.DskReader;
import com.lavacablasa.ladc.abadia.Juego;

/**
 * <h1>La abadía del crimen</h1>
 *
 * Dedicado a la memoria de Paco Menendez.
 * <p>
 * Reingeniería inversa por <a href="mailto:vigasoco@gmail.com">Manuel Abadía</a>.
 * <p>
 * Traducción de C++ a Java por Pedro García-pego Catalá.
 * <p>
 * Adaptación GWT para transpilar a JS por Ignacio Baca Moreno-Torres.
 *
 * <h3>Notas de la conversión:</h3>
 * La conversión se ha hecho trabajando sobre la versión de Amstrad CPC 6128, que fue
 * el ordenador en el que se creó originalmente el juego, entendiendo el código del Z80
 * y creando una serie de objetos que interactuan entre si para que el resultado sea
 * equivalente al juego original.
 * <p>
 * El juego original tiene 2 capas de tiles para generar las pantallas y crear un efecto
 * de profundidad. Para una gran parte de las pantallas, estas 2 capas son suficiente,
 * aunque para algunas pantallas con gran cantidad de bloques que coinciden en los mismos
 * tiles, como la pantalla 0x34, esto provoca ligeras imperfecciones gráficas. Probablemente
 * en el juego original no se usaron más capas porque cada capa adicional supone 960 bytes,
 * una pasada más a la hora de dibujar la pantalla y una pasada más en el bucle interior del
 * dibujado de sprites, y este incremento en memoria y cálculos consigue una mejora gráfica
 * muy pequeña. Aprovechando que ahora los ordenadores son más potentes, he generalizado los
 * algoritmos que tratan con las capas de forma que el número de capas que se usan viene
 * determinado por la constante nivelesProfTiles de la clase GeneradorPantallas (que por
 * defecto vale 3, para que las pantallas se vean sin errores gráficos).
 * <p>
 * El juego posee un intérprete para la construcción de los bloques que forman las
 * pantallas. Actualmente se interpretan los bloques ya compilados (que están en los
 * datos originales), aunque estaría bien crear ficheros con los scripts de cada bloque
 * y un compilador de bloques, de forma que se interprete el código que genere el
 * compilador de bloques en vez del que viene en los datos originales.
 * <p>
 * El comportamiento de los personajes se basa en el intérprete de scripts que trae el
 * juego original. En la conversión, el comportamiento se ha pasado directamente a C++.
 *
 * <h3>Por hacer:</h3>
 * <ul>
 * <li>añadir sonido</li>
 * <li>cargar/grabar partidas</li>
 * </ul>
 */
public class LaAbadiaDelCrimen {

    private final Juego abadiaGame;

    public LaAbadiaDelCrimen(GameContext context) {

        byte[] diskData = context.load("/abadia.dsk");
        byte[] memoryData = readDiskImageToMemory(diskData);

        abadiaGame = new Juego(memoryData, new CPC6128(context), context);
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
        return Promise.merge(abadiaGame.gameLogicLoop(), abadiaGame.mainSyncLoop());
    }
}
