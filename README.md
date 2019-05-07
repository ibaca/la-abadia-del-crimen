# La abadía del crimen

Dedicado a la memoria de Paco Menendez.

Reingeniería inversa por <a href="mailto:vigasoco@gmail.com">Manuel Abadía</a>.

Traducción de C++ a Java por Pedro García-pego Catalá.

Adaptación GWT para transpilar a JS por Ignacio Baca Moreno-Torres.

## Notas de la conversión:
La conversión se ha hecho trabajando sobre la versión de Amstrad CPC 6128, que fue
el ordenador en el que se creó originalmente el juego, entendiendo el código del Z80
y creando una serie de objetos que interactuan entre si para que el resultado sea
equivalente al juego original.

El juego original tiene 2 capas de tiles para generar las pantallas y crear un efecto
de profundidad. Para una gran parte de las pantallas, estas 2 capas son suficiente,
aunque para algunas pantallas con gran cantidad de bloques que coinciden en los mismos
tiles, como la pantalla 0x34, esto provoca ligeras imperfecciones gráficas. Probablemente
en el juego original no se usaron más capas porque cada capa adicional supone 960 bytes,
una pasada más a la hora de dibujar la pantalla y una pasada más en el bucle interior del
dibujado de sprites, y este incremento en memoria y cálculos consigue una mejora gráfica
muy pequeña. Aprovechando que ahora los ordenadores son más potentes, he generalizado los
algoritmos que tratan con las capas de forma que el número de capas que se usan viene
determinado por la constante nivelesProfTiles de la clase GeneradorPantallas (que por
defecto vale 3, para que las pantallas se vean sin errores gráficos).

El juego posee un intérprete para la construcción de los bloques que forman las
pantallas. Actualmente se interpretan los bloques ya compilados (que están en los
datos originales), aunque estaría bien crear ficheros con los scripts de cada bloque
y un compilador de bloques, de forma que se interprete el código que genere el
compilador de bloques en vez del que viene en los datos originales.

El comportamiento de los personajes se basa en el intérprete de scripts que trae el
juego original. En la conversión, el comportamiento se ha pasado directamente a C++.

## Por hacer:
 - añadir sonido
 - cargar/grabar partidas
