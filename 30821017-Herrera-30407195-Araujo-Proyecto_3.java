// CONSIDERACIONES:
// 1. Nosotras establecemos cuanto de bateria poner como max.


import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

class MonitorEstacionamiento {
    private int[][] tablero = new int[6][6];
    private List<Integer> vehiculosSinBateria = new ArrayList<>();

    public MonitorEstacionamiento() {
        for(int i = 0; i < 6; i++){
            for(int j = 0; j < 6; j++){
                tablero[i][j] = -1;
            }
        }
    }
    

    // Funciones sincronizadas
    public synchronized void moverVehiculo(int id, int nuevaFila, int nuevaColumna) {
        // Metodo MoveVehicle: Mueve un vehiculo a una nueva posicion
    }

    public synchronized void reportarSinBateria(int id) {
        // Metodo ReportNoBattery: Agrega el ID del vehiculo a la lista de varados
    }

    public synchronized void atenderSinBateria() {
        // Metodo AttendNoBattery: Atiende a los vehiculos varados y los recarga
    }

    public synchronized void recargaCompleta(int id) {
        // Metodo RechargeComplete: Elimina el ID del vehiculo de la lista de los sin bateria 
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
            if(tablero[tempFila][tempColumna] != -1) return false; // solapamiento

            if(vehiculo.getOrientacion() == Vehiculo.Orientacion.h) {
                tempColumna++; 
            } else {
                tempFila++; 
            }
        }

        // ocupar casilla
        for (int i=0; i<vehiculo.getLongitud(); i++) {
            tablero[fila][columna] = vehiculo.getVehiculoId(); // llenando el tablero con el ID del vehículo

            if(vehiculo.getOrientacion() == Vehiculo.Orientacion.h) {
                columna++; 
            } else {
                fila++; 
            }
        }
        return true; 
    }

    private void initialBoard(List<Vehiculo> iniciales) {
        for (Vehiculo vehiculo : iniciales) {
            if(!esVehiculoValido(vehiculo)) {
                System.err.println("Vehiculo " + vehiculo.getVehiculoId() + " fuera de limites.");
                System.exit(1);
            }

            if(!casillaOcupada(vehiculo)) {
                System.err.println("Solapamiento detectado en vehiculo " + vehiculo.getVehiculoId());
                System.exit(1);
            }
        }
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
}

class Cargador extends Thread {
    private MonitorEstacionamiento monitor;

    // Constructor
    public Cargador(MonitorEstacionamiento monitor) {
        this.monitor = monitor;
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
                String[] tokens = linea.split("\\s+");

                // Validar que la linea tenga exactamente 6 campos
                if (tokens.length != 6) {
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
                    if (orientacion == 'h' && ((columna + longitud)-1) > 6) {
                        throw new IllegalArgumentException("El vehiculo se sale del tablero horizontalmente.");
                    }
                    if (orientacion == 'v' && ((fila + longitud)-1) > 6) {
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