# Proyecto #3 – Programación Concurrente: The Electric Rush Hour

Este proyecto implementa una simulación concurrente basada en el modelo de Productores y Consumidores en Java para resolver (o simular el intento de solución de) escenarios del juego Rush Hour, con la variante de que los vehículos funcionan con baterías limitadas y requieren ser recargados, donde todo se simula en una matriz 6x6.

## 1. Procesos involucrados en la simulación

El sistema fue diseñado empleando múltiples hilos (Threads) para simular el entorno concurrente, separando las responsabilidades de la siguiente manera:

* **Vehículos (Consumidores):** Cada vehículo en el tablero es instanciado como un hilo independiente (`class Vehiculo extends Thread`). Su función es moverse por el tablero, consumiendo una unidad de batería por cada movimiento exitoso. Si su batería llega (o se inicializa) a 0, el proceso se bloquea solicitando asistencia y espera a ser recargado para continuar operando.
* **Cargadores (Productores):** Las unidades de carga se implementan como hilos independientes (`class Cargador extends Thread`). Su trabajo consiste en monitorear al recurso compartido en busca de vehículos varados. Si encuentran un vehículo sin batería, proceden a "recargarlo", lo que toma un tiempo simulado establecido (1 segundo), y posteriormente lo despiertan para que reanude su ejecución.
* **Guardián de Solución (Monitor de Tiempo):** Se introdujo una decisión de diseño creando un hilo adicional llamado `GuardianSolucion`. Este hilo actúa como un hilo demonio (Daemon) cuya única responsabilidad es contabilizar el tiempo global de ejecución. Si la simulación sobrepasa los 10 minutos (lo cual es un exceso de espera por la solución) sin que el vehículo objetivo (ID 0) logre salir, este hilo asume que el tablero no tiene solución debido al movimiento heurístico/aleatorio, marcando el fin de la simulación y abortando el programa de forma controlada.

## 2. Recursos Críticos y Operaciones de Acceso

Todos los recursos compartidos están encapsulados dentro de la clase `MonitorEstacionamiento`.

* **El Tablero (Matriz de 6x6):** Representa el espacio físico. Es el principal recurso crítico y se accede para verificar espacios libres y actualizar las posiciones mediante el método `moverVehiculo()`.
* **Lista de Vehículos Sin Batería (`vehiculosSinBateria`):** Una cola (ArrayList) que almacena los IDs de los vehículos que requieren carga. Se accede a través de:
* `reportarSinBateria(int id)`: Para encolar un vehículo.
* `atenderSinBateria()`: Extrae y retorna el primer vehículo varado de la lista para que el cargador lo procese.


* **Estado de la Simulación (`simulacionTerminada`):** Una variable booleana definida como `volatile` que indica si el vehículo objetivo ha salido del tablero o si se ha determinado que el juego es irresoluble. Es leída constantemente por todos los hilos en ejecución.
* **Lista de Todos los Vehículos (`todosLosVehiculos`):** Utilizada para mapear los IDs con los objetos a la hora de manipular la batería mediante el método `recargaCompleta(int id)`.

## 3. Condiciones de Sincronización para Exclusión Mutua

Para evitar condiciones de carrera y garantizar la exclusión mutua (por ejemplo, evitar que dos vehículos ocupen la misma celda o que dos cargadores atiendan al mismo vehículo a la vez), se establecieron métodos con el modificador `synchronized` dentro del monitor:

* **Sincronización de Movimiento:** El método `moverVehiculo()` está sincronizado. Esto garantiza que el cálculo del destino, la verificación de que la casilla esté libre y la posterior escritura de los nuevos valores en la matriz ocurran de forma atómica.
* **Coordinación Consumidor-Productor (Wait y NotifyAll):** * Cuando un vehículo se queda sin batería, llama a `esperarRecarga()`, donde se encola y entra en un estado de `wait()` iterativo (comprobando su estado de batería) liberando el candado del monitor.
* El cargador, al solicitar trabajo en `atenderSinBateria()`, ejecuta un `wait(500)` si la cola está vacía, liberando el monitor pero revisando periódicamente si la simulación ha terminado.
* Al finalizar una carga en `recargaCompleta()`, el cargador actualiza la batería a 10 e invoca `notifyAll()`, despertando al vehículo correspondiente para que prosiga.



## 4. Gestión de Interbloqueos (Deadlocks)

El diseño consideró los posibles atascos originados por el modelo de hilos. La lógica aplicada fue la siguiente:

* En el método `moverVehiculo()`, si el vehículo nota que la casilla destino está ocupada o fuera del tablero, devuelve `false` y **no se queda esperando bloqueando el monitor**.
* En su ciclo de ejecución (`run`), si el movimiento falla, el vehículo invierte su dirección (de atrás hacia adelante o viceversa) e invoca un `Thread.sleep(300)`. Esto simula el intento de "desatascar" la vía cediendo explícitamente el uso de la CPU a otros hilos (vehículos) para que puedan moverse e intentar despejar el camino.
* Al no existir retención estricta de bloqueos compartidos más allá de las operaciones atómicas del monitor, se evita el "abrazo mortal" entre los procesos de movimiento.

## 5. Decisiones de Diseño 

Se toma un conjunto de decisiones clave para asegurar la viabilidad del simulador:

* **Eficiencia vs. Aleatoriedad:** Dado que las especificaciones sugieren un algoritmo de movimiento heurístico simple (aleatorio) y no se toma en cuenta el tiempo que tomaría aquella lógica en hallar una solución (o en identificar que no hay una), se reconoce que los vehículos se mueven al azar (salvo el vehículo ID 0, que se le otorgó una preferencia hacia adelante para buscar la salida más rápido). Esto hace que la eficiencia para encontrar la solución no sea la óptima. Sin embargo, para evitar bucles de ejecución infinitos, se introdujo una serie de validaciones al momento de leer el archivo:
* Si hay otro vehículo horizontal en la misma fila del carro 0 que bloquee totalmente su paso.
* Si existe una columna completa (frente al carro 0) ocupada enteramente por vehículos verticales.
En dichos casos, o si se excede el tiempo límite (10 minutos gestionados por `GuardianSolucion`), el programa aborta y declara el tablero como sin solución.

* **Tiempos de Espera Seleccionados:** Para gestionar la concurrencia, se asignan intervalos de espera que reducen la contención del monitor. De esta forma, se facilita la alternancia de hilos y se asegura con ello que el sistema sea responsivo al obligar a los hilos a liberar el procesador temporalmente tras un movimiento exitoso o un intento fallido. Resulta vital para que los Cargadores (Productores) puedan detectar y procesar las soliciudes de energía de los Vehículos (Consumidores) de manera oportuna.  
* **Prioridad de Recarga (Coleada):** En la lista de vehículos varados, si el vehículo objetivo (ID 0) se queda sin batería, el algoritmo interviene dándole prioridad absoluta ("coleándolo" al principio de la lista de pendientes).
* **Niveles de Batería Fijos:** Se decide predefinir el monto de batería recuperada tras cada recarga exitosa en `10` unidades de movimiento.
* **Representación Visual:** El estacionamiento en la consola imprime casillas vacías con el carácter `_`, y si están ocupadas, refleja el número de ID del vehículo correspondiente.
* **Validaciones Previas:** Se separó la validación estática de formato (coordenadas erróneas, salida del tablero) en la clase lectora, mientras que la validación lógica del tablero (solapamiento de dos autos en una misma celda inicial) se hace en el monitor (`initialBoard`).

## 6. Instrucciones de Uso y Casos de Prueba

### Compilación y Ejecución

La solución fue desarrollada de manera independiente de cualquier IDE y puede ser compilada y ejecutada directamente desde una terminal.

1. Para compilar el código fuente:
```bash
javac 30821017-Herrera-30407195-Araujo-Proyecto_3.java

```


2. Para ejecutar el programa:
```bash
java RushHour <ruta_del_archivo.txt>

```



### Manejo de Casos de Prueba y Salida

* El programa recibe **un único archivo `.txt` por cada ejecución**. Se disponen de diferentes casos de prueba (`pruebas1.txt`, `pruebas2.txt`, etc.), los cuales deben ser suministrados individualmente como parámetro al momento de llamar al `main`.
* **Formato de Salida:** Tras ejecutar, el programa imprimirá primero el tablero en su estado inicial intacto. Posteriormente, por cada movimiento exitoso de cualquier vehículo, se refrescará y re-imprimirá la cuadrícula completa de 6x6, junto con un mensaje que indica qué vehículo se movió y la cantidad de batería que le resta en ese momento.
* **Cálculo de Tiempos:** Cuando la simulación concluye (ya sea por victoria del ID 0 al cruzar la columna 5 o por tablero sin solución detectado), el sistema mostrará en consola un resumen que detalla el **tiempo total de ejecución de la simulación**, expresado tanto en milisegundos (ms) como en segundos (s).