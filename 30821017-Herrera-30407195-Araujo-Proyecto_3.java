// CONSIDERACIONES:
// 1. Nosotras establecemos cuanto de bateria poner como max.

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

class MonitorEstacionamiento {
    private String[][] tablero = new String[6][6];
    private List<Integer> vehiculosSinBateria = new ArrayList<>();
    private List<Vehiculo> todosLosVehiculos = new ArrayList<>();
    private volatile boolean simulacionTerminada = false;

    public MonitorEstacionamiento() {
        for(int i = 0; i < 6; i++){
            for(int j = 0; j < 6; j++){
                tablero[i][j] = "_";
            }
        }
    }

    public void imprimirTablero() {
        for(int i = 0; i < 6; i++){
            for(int j = 0; j < 6; j++){
                System.out.print(tablero[i][j]);
            }
            System.out.println();
        }
    }

    // Funciones synchronized
    public synchronized boolean moverVehiculo(Vehiculo v, int direccion) throws InterruptedException {
        if (this.simulacionTerminada) return false;

        int filaActual = v.getFila();
        int colActual = v.getColumna();
        int longitud = v.getLongitud();
        int filaDestino = filaActual;
        int colDestino = colActual;
        
        // calcular movimiento segun direccion
        if (v.getOrientacion() == Vehiculo.Orientacion.h) {
            if (direccion == 1) colDestino = colActual + longitud;
            else colDestino = colActual - 1;
        } else {
            if (direccion == 1) filaDestino = filaActual + longitud;
            else filaDestino = filaActual - 1;
        }

        // verificar que se mantenga en el tablero
        if (!estaEnTablero(filaDestino, colDestino)) {
            return false;
        }

        // verificar que la casilla esta libre
        if (!tablero[filaDestino][colDestino].equals("_")) {
            return false;
        }

        // actualizar matriz
        String idStr = String.valueOf(v.getVehiculoId());

        if (v.getOrientacion() == Vehiculo.Orientacion.h) {
            if (direccion == 1) {
                tablero[filaActual][colActual] = "_";
                tablero[filaActual][colDestino] = idStr;
                v.setColumna(colActual + 1);
            } else {
                tablero[filaActual][colActual + longitud - 1] = "_";
                tablero[filaActual][colDestino] = idStr;
                v.setColumna(colActual - 1);
            }
        } else {
            if (direccion == 1) {
                tablero[filaActual][colActual] = "_";
                tablero[filaDestino][colActual] = idStr;
                v.setFila(filaActual + 1);
            } else {
                tablero[filaActual + longitud - 1][colActual] = "_";
                tablero[filaDestino][colActual] = idStr;
                v.setFila(filaActual - 1);
            }
        }

        // 5. Mostrar progreso
        imprimirTablero();
        System.out.println("Vehiculo " + v.getVehiculoId() + " se movio. Bateria: " + (v.getBateria() - 1));
        System.out.println("-------------------------");

        // 6. Verificar condición de salida (Vehículo 0 llega con su parte frontal a la col 5)
        if (v.getVehiculoId() == 0 && (v.getColumna() + v.getLongitud() - 1) == 5) {
            this.simulacionTerminada = true;
            System.out.println("¡EL VEHICULO 0 HA SALIDO!");
            notifyAll();
        }

        notifyAll(); // Despierta a otros (incluyendo cargadores o autos esperando)
        return true;
    }

    public synchronized void reportarSinBateria(int id) {
        // Agrega el ID del vehiculo a la lista de varados
        if (!vehiculosSinBateria.contains(id)) {
            // NOTA: la prioridad de recarga la tiene el ID 0
            if (id == 0) {
                vehiculosSinBateria.add(0, id);
            }
            else {
                vehiculosSinBateria.add(id);
            }

            System.out.println("Vehiculo " + id + " reportado sin bateria.");
            notifyAll();
        }
    }

    // public synchronized int atenderSinBateria() throws InterruptedException {
    //     // Atiende a los vehiculos varados y los recarga
    //     while(vehiculosSinBateria.isEmpty()) {
    //         wait(); // esperar hasta que haya un vehiculo sin bateria
    //     }
    //     if (simulacionTerminada) return -1;
    //     return vehiculosSinBateria.remove(0); // retornar el ID del vehiculo que se va a recargar (el primero)
    // }

    public synchronized int atenderSinBateria() throws InterruptedException {
        // El bucle debe verificar la terminación para no quedarse bloqueado eternamente
        while(vehiculosSinBateria.isEmpty() && !simulacionTerminada) {
            wait(500);
        }
        // Si despertamos porque terminó la simulación, salimos
        if (simulacionTerminada) return -1;

        return vehiculosSinBateria.remove(0);
    }

    public synchronized void recargaCompleta(int id) {
        // Elimina el ID del vehiculo de la lista de los sin bateria
        for (Vehiculo vehiculo : todosLosVehiculos) {
            if (vehiculo.getVehiculoId() == id) {
                vehiculo.setBateria(10); // definimos 10 como la bateria maxima
                break;
            }
        }
        notifyAll();
    }

    public synchronized void esperarRecarga(int id) throws InterruptedException {
        // Vehiculos se bloquea a si mismo hasta que su bateria sea recargada
        reportarSinBateria(id);

        Vehiculo vehiculoActual = null;
        for (Vehiculo vehiculo : todosLosVehiculos) {
            if(vehiculo.getVehiculoId() == id) {
                vehiculoActual = vehiculo;
            }
        }

        while (vehiculoActual != null && vehiculoActual.getBateria() == 0) {
            wait();
        }
    }


    // Funciones de utilidad
    // 1. Inicializacion del tablero
    private boolean estaEnTablero(int fila, int columna) {
        return fila >= 0 && fila < 6 && columna >= 0 && columna < 6;
    }

    private boolean esVehiculoValido(Vehiculo vehiculo) {

        if (vehiculo.getLongitud() <= 0 || vehiculo.getBateria() < 0) return false;

        // tener el extremo del vehiculo
        int filaInicial = vehiculo.getFila();
        int columnaInicial = vehiculo.getColumna();
        int filaFinal = filaInicial;
        int columnaFinal = columnaInicial;

        if (vehiculo.getOrientacion() == Vehiculo.Orientacion.h) {
            columnaFinal += vehiculo.getLongitud() - 1; // extremo vehiculo horizontal
        } else {
            filaFinal += vehiculo.getLongitud() - 1; // extremo vehiculo vertical
        }

        return estaEnTablero(filaInicial, columnaInicial) && estaEnTablero(filaFinal, columnaFinal);
    }

    private boolean casillaOcupada(Vehiculo vehiculo) {
        int fila = vehiculo.getFila();
        int columna = vehiculo.getColumna();

        // verificar cada casilla que ocupa el vehículo
        int tempFila = fila;
        int tempColumna = columna;

        for (int i=0; i<vehiculo.getLongitud(); i++) {
            if(tablero[tempFila][tempColumna] != "_") return false; // solapamiento

            if(vehiculo.getOrientacion() == Vehiculo.Orientacion.h) {
                tempColumna++;
            } else {
                tempFila++;
            }
        }

        // ocupar casilla
        for (int i=0; i<vehiculo.getLongitud(); i++) {
            tablero[fila][columna] = String.valueOf(vehiculo.getVehiculoId()); // llenando el tablero con el ID del vehiculo 🚧

            if(vehiculo.getOrientacion() == Vehiculo.Orientacion.h) {
                columna++;
            } else {
                fila++;
            }
        }
        return true;
    }

    public void initialBoard() {
        for (Vehiculo vehiculo : todosLosVehiculos) {
            if(!esVehiculoValido(vehiculo)) {
                System.err.println("Vehiculo " + vehiculo.getVehiculoId() + " fuera de limites."); //🚧
                System.exit(1);
            }

            if(!casillaOcupada(vehiculo)) {
                System.err.println("Solapamiento detectado en vehiculo " + vehiculo.getVehiculoId());
                System.exit(1);
            }
        }
    }

    // getters y setters

    public boolean getSimulacionTerminada() {
        return this.simulacionTerminada;
    }

    public List<Integer> getListaSinBateria() {
        return this.vehiculosSinBateria;
    }

    public List<Vehiculo> getListaVehiculos() {
        return this.todosLosVehiculos;
    }

    public void setSimulacionTerminada(boolean estado) {
        this.simulacionTerminada = estado;
    }

    public void setListaSinBAteria(List<Integer> listaSinBateria) {
        this.vehiculosSinBateria = listaSinBateria;
    }

    public void setListaVehiculos(List<Vehiculo> listaVehiculos) {
        this.todosLosVehiculos = listaVehiculos;
    }

}


class Vehiculo extends Thread {
    private int id;
    public enum Orientacion {h, v};
    private Orientacion orientacion;
    private int fila;
    private int columna;
    private int longitud;
    private int bateria;
    private MonitorEstacionamiento monitor;

    // Constructor
    public Vehiculo(int id, Orientacion orientacion, int fila, int columna, int longitud, int bateria, MonitorEstacionamiento monitor) {
        this.id = id;
        this.orientacion = orientacion;
        this.fila = fila;
        this.columna = columna;
        this.longitud = longitud;
        this.bateria = bateria;
        this.monitor = monitor;
    }

    public int getVehiculoId(){
        return this.id;
    }

    public Orientacion getOrientacion(){
        return this.orientacion;
    }

    public int getFila(){
        return this.fila;
    }

    public int getColumna(){
        return this.columna;
    }

    public int getLongitud(){
        return this.longitud;
    }

    public int getBateria(){
        return this.bateria;
    }

    public void setBateria(int recarga){
        this.bateria = recarga;
    }

    public void setFila(int newFila){
        this.fila = newFila;
    }

    public void setColumna(int newColumna){
        this.columna = newColumna;
    }


    @Override
    public void run() {
        int direccion = 1;

        // preferencia direccion adelante (1) sobre atras (-1) en id 0
        if (this.id != 0) direccion = (Math.random() < 0.5) ? 1 : -1;

        while (!monitor.getSimulacionTerminada()) {
            try {
                if (this.bateria > 0) {
                    boolean movimientoExitoso = monitor.moverVehiculo(this, direccion);

                    if (movimientoExitoso) {
                        this.bateria--;
                        Thread.sleep(100);
                        if(this.id == 0) direccion = 1;
                    }
                    else { // desatascar
                        direccion = (direccion == 1) ? -1 : 1; // cambiar de direccion

                        // dejar respirar al procesaodr un momento para que otros hilos puedan moverse
                        // Thread.yield();
                        Thread.sleep(300);
                    }
                }
                else {
                    System.out.println("Vehiculo " + id + " sin bateria. Esperando carga...");
                    monitor.esperarRecarga(this.id);
                    System.out.println("Vehiculo " + id + " recargado. Reanudando movimiento...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

}

class Cargador extends Thread {
    private MonitorEstacionamiento monitor;

    // Constructor
    public Cargador(MonitorEstacionamiento monitor) {
        this.monitor = monitor;

        // PRUEBA: confirmar en consola que el cargador fue creado
        //System.out.println("[PRUEBA] Cargador creado: " + this.getName());
    }

    @Override
    public void run() {
        while (!monitor.getSimulacionTerminada()) {
            try {
                int idVarado = monitor.atenderSinBateria(); // si la lista esta vacia, se bloquea el hilo hasta que se llene

                if (idVarado == -1) break; // si se retorna -1, significa que la simulacion ha terminado

                // NOTA: this.getName() es un metodo de Thread que retorna el nombre del hilo por defecto

                System.out.println("Cargador " + this.getName() + " atendiendo vehiculo " + idVarado);
                Thread.sleep(1000); // simula el tiempo de recarga

                monitor.recargaCompleta(idVarado);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Cargador " + this.getName() + " termino su ejecucion.");
        // PRUEBA: confirmar en consola que el hilo cargador inició
        //System.out.println("[PRUEBA] Cargador iniciado: " + this.getName());
    }

}

class LectorVehiculos {

    public List<Vehiculo> leerArchivo(String rutaArchivo, MonitorEstacionamiento monitor, int[] datosCarro0) {
        List<Vehiculo> listaVehiculos = new ArrayList<>();
        File archivo = new File(rutaArchivo);

        try (Scanner lector = new Scanner(archivo)) {
            int numeroLinea = 0;

            while (lector.hasNextLine()) {
                numeroLinea++;
                String linea = lector.nextLine().trim();

                // Ignorar lineas vacias
                if (linea.isEmpty()) continue;

                // Tokenizacion por uno o mas espacios en blanco
                String[] tokens = linea.split("\\s*,\\s*");

                // Validar que la linea tenga exactamente 6 campos o es la linea cantidad de cargadores
                if (tokens.length == 2 && tokens[0].equalsIgnoreCase("cargadores")) {
                    // PRUEBA: informar cuántos cargadores se van a crear
                    //System.out.println("[PRUEBA] Cantidad de cargadores a crear: " + tokens[1]);

                    // Si es un cargador, se crea un hilo por cada cargador y se inicia
                    for (int i = 0; i < Integer.parseInt(tokens[1]); i++) {
                        Cargador cargador = new Cargador(monitor);
                        cargador.start();
                    }
                    return listaVehiculos; // No se esperan mas vehiculos despues de la linea de cargadores
                }
                else if (tokens.length != 6) {
                    System.err.println("Error en linea " + numeroLinea + ": Formato incorrecto (se esperan 6 datos).");
                    System.exit(1);
                }
                try {
                    // Extraccion de datos
                    int id = Integer.parseInt(tokens[0]);
                    char orientacion = tokens[1].toLowerCase().charAt(0);
                    int fila = Integer.parseInt(tokens[2]);
                    int columna = Integer.parseInt(tokens[3]);
                    int longitud = Integer.parseInt(tokens[4]);
                    int bateria = Integer.parseInt(tokens[5]);

                    if (orientacion != 'h' && orientacion != 'v') {
                        throw new IllegalArgumentException("Orientacion '" + orientacion + "' no permitida.");
                    }
                    if (fila < 0 || fila > 5 || columna < 0 || columna > 5) {
                        throw new IllegalArgumentException("Coordenadas fuera de rango (0-5).");
                    }
                    if (orientacion == 'h' && ((columna + longitud)-1) > 5) {
                        throw new IllegalArgumentException("El vehiculo se sale del tablero horizontalmente.");
                    }
                    if (orientacion == 'v' && ((fila + longitud)-1) > 5) {
                        throw new IllegalArgumentException("El vehiculo se sale del tablero verticalmente.");
                    }

                    Vehiculo.Orientacion orientacionEnum;
                    if (orientacion == 'h') {
                        orientacionEnum = Vehiculo.Orientacion.h;
                    } else {
                        orientacionEnum = Vehiculo.Orientacion.v;
                    }

                    if(id == 0) {
                        datosCarro0[0] = fila;
                        datosCarro0[1] = columna;
                        datosCarro0[2] = longitud;
                    }

                    // Crear el vehiculo y añadirlo a la lista
                    Vehiculo v = new Vehiculo(id, orientacionEnum, fila, columna, longitud, bateria, monitor);
                    listaVehiculos.add(v);

                } catch (NumberFormatException e) {
                    System.err.println("FATAL: Error de formato numerico en linea " + numeroLinea);
                    System.exit(1);
                } catch (IllegalArgumentException e) {
                    System.err.println("FATAL: Dato invalido en linea " + numeroLinea + ": " + e.getMessage());
                    System.exit(1);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error: No se pudo encontrar el archivo '" + rutaArchivo + "'.");
            System.exit(1);
        }

        return listaVehiculos;
    }
}

class GuardianSolucion extends Thread {
    private MonitorEstacionamiento monitor;
    private long tiempoLimite;

    public GuardianSolucion(MonitorEstacionamiento monitor, long tiempoLimite) {
        this.monitor = monitor;
        this.tiempoLimite = tiempoLimite;

        // es un hilo demonio
        this.setDaemon(true);
    }

    @Override
    public void run() {
        try {
            Thread.sleep(tiempoLimite);

            if (!monitor.getSimulacionTerminada()) {
                System.err.println("Tablero sin solucion: se excedio el tiempo limite de ejecucion (" + (tiempoLimite / 60000) + " min).");
                monitor.setSimulacionTerminada(true);
                System.exit(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }    
}

class RushHour {
    public static void main(String[] args) {
        // para medir el tiempo
        long startTime = System.currentTimeMillis();
        // crear el Monitor vacio
        MonitorEstacionamiento monitor = new MonitorEstacionamiento();

        int[] datosCarro0 = new int[3]; // [fila, columna, longitud]

        // leer el archivo y crear los hilos de los vehiculos
        // pasandoles la referencia del monitor que acabamos de crear
        LectorVehiculos lector = new LectorVehiculos();
        List<Vehiculo> listaDeVehiculos = lector.leerArchivo(args[0], monitor, datosCarro0);

        int filaCarro0 = datosCarro0[0];
        int colCarro0 = datosCarro0[1];
        int longitudCarro0 = datosCarro0[2];

        // Comprobacion de posible solucion de tablero:
        // 1. Caso en el que hay otro carro H en la misma fila que el carro 0, no seguir
        int cabezaColumnaCarro0 = colCarro0 + longitudCarro0; // recalculo cabeza carro 0
        for (Vehiculo vehiculo : listaDeVehiculos) {
            if (vehiculo.getId() != 0 && vehiculo.getFila() == filaCarro0 && vehiculo.getOrientacion() == Vehiculo.Orientacion.h && vehiculo.getColumna() >= cabezaColumnaCarro0) {
                System.err.println("Tablero sin solucion: el vehiculo " + vehiculo.getVehiculoId() + " bloquea horizontalmente al carro 0 en la fila " + filaCarro0 + ".");
                System.exit(1);
            }
        }

        // 2. Si hay toda una linea de carros de la misma direccion que ocupa toda la columna delande del id 0, no seguir
        // solo revisamos las columnas que estan delante del carro 0
        for (int col = cabezaColumnaCarro0; col < 6; col++) {
            int celdasOcupadasEnColumna = 0;
            for (Vehiculo vehiculo : listaDeVehiculos) {
                if (vehiculo.getOrientacion() == Vehiculo.Orientacion.v && vehiculo.getColumna() == col) {
                    celdasOcupadasEnColumna += vehiculo.getLongitud();
                }
            }

            // Si la suma de las longitudes de los carros verticales en esa columna es 6 (todo el tablero)
            if (celdasOcupadasEnColumna == 6) {
                System.err.println("Tablero sin solucion: La columna " + col + " esta totalmente bloqueada por vehiculos verticales.");
                System.exit(1);
            }
        }

        monitor.setListaVehiculos(listaDeVehiculos);
        monitor.initialBoard();

        // 3. En caso de que no se encuentre solucion en 10 min, asumimos que el tablero no tiene solucion y se termina la simulacion
        GuardianSolucion guardian = new GuardianSolucion(monitor, 10 * 60000);
        guardian.start();

        for (Vehiculo v : listaDeVehiculos) {
            v.start();
        }

        // Bucle de espera hasta que el monitor marque el fin
        while (!monitor.getSimulacionTerminada()) {
            try {
                Thread.sleep(100); // Pequeña pausa para no saturar la CPU
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n========================================");
        System.out.println("SIMULACIÓN FINALIZADA");
        System.out.println("Tiempo total de ejecución: " + duration + " ms");
        System.out.println("En segundos: " + (duration / 1000.0) + " s");
        System.out.println("========================================");
    }
}

// para correr el programa, se debe pasar la ruta del archivo de texto como argumento
// java RushHour ruta_del_archivo.txt
// ejemplo de comando para compilar y correr el programa
// javac 30821017-Herrera-30407195-Araujo-Proyecto_3.java
// java RushHour pruebas.txt
