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
    private boolean simulacionTerminada = false;

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
    
    // Funciones sincronizadas
    public synchronized void moverVehiculo(int id, int nuevaFila, int nuevaColumna) {
        // Metodo MoveVehicle: Mueve un vehiculo a una nueva posicion
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

    public synchronized int atenderSinBateria() throws InterruptedException {
        // Atiende a los vehiculos varados y los recarga
        while(vehiculosSinBateria.isEmpty()) {
            wait(); // esperar hasta que haya un vehiculo sin bateria
        }
        return vehiculosSinBateria.remove(0); // retornar el ID del vehiculo que se va a recargar (el primero)
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

    @Override
    public void run() {
        while (!monitor.getSimulacionTerminada()) {
            try {
                if (this.bateria > 0) {
                    // Logica de movimiento del vehiculo
                }
                else {
                    System.out.println("Vehiculo " + id + " sin bateria. Esperando carga...");
                    monitor.esperarRecarga(this.id);
                    System.out.println("Vehiculo " + id + " recargado. Reanudando movimiento...");
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
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
                Thread.sleep(10000); // simula el tiempo de recarga

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

    public List<Vehiculo> leerArchivo(String rutaArchivo, MonitorEstacionamiento monitor) {
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

class RushHour {
    public static void main(String[] args) {
        // 1. Crear el Monitor vacio
        MonitorEstacionamiento monitor = new MonitorEstacionamiento();

        // 2. Leer el archivo y crear los hilos de los vehiculos 
        // pasandoles la referencia del monitor que acabamos de crear
        LectorVehiculos lector = new LectorVehiculos();
        List<Vehiculo> listaDeVehiculos = lector.leerArchivo(args[0], monitor);

        // for (Vehiculo v : listaDeVehiculos) {
        //     System.out.println("Vehiculo ID: " + v.getVehiculoId() + ", Orientacion: " + v.getOrientacion() + ", Fila: " + v.getFila() + ", Columna: " + v.getColumna() + ", Longitud: " + v.getLongitud() + ", Bateria: " + v.getBateria());
        // }

        monitor.setListaVehiculos(listaDeVehiculos);
        monitor.initialBoard();
        monitor.imprimirTablero();


        // 3. Inyectar la lista de vehiculos de vuelta al monitor 
        // para que este pueda llenar la matriz y saber donde esta cada uno
        // monitor.setVehiculos(listaDeVehiculos);

        // 4. Ahora que todos se conocen, inicias los hilos
        for (Vehiculo v : listaDeVehiculos) {
            v.start();
        }
    }
}

// para correr el programa, se debe pasar la ruta del archivo de texto como argumento
// java RushHour ruta_del_archivo.txt
// ejemplo de comando para compilar y correr el programa
// javac 30821017-Herrera-30407195-Araujo-Proyecto_3.java
// java RushHour pruebas.txt