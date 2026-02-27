// CONSIDERACIONES:
// 1. Nosotras establecimos la recarga de bateria en 10 
// 2. Carro 0 tiene preferencia por direccion 1
// 3. Nuevo hilo GuardianSolucion para detectar tableros sin solucion despues de 10 min de ejecucion
// 4. Si hay otro carro H en la misma fila que el carro 0 que bloquea su camino hacia la salida, se asume que el tablero no tiene solucion
// 5. Si hay toda una linea de carros V que ocupa toda la columna delande del carro 0, se asume que el tablero no tiene solucion
// 6. Se agregaron mensajes de error detallados para casos de formato incorrecto, datos invalidos, y tableros sin solucion
// 7. Se busca prioridad de recarga al vehiculo 0 en la lista de varados, 'coleandolo'
// 8. Se decide imprimir el tablero en cada cambio para mostrar la ejecucion del programa
// 9. La solucion del tablero se resuelve a traves de un algoritmo de aleatoriedad
// 10. Se asume casilla vacia como "_", y se llena con el id del vehiculo cuando esta ocupada
// 11. Se inicia la simulacion imprimiendo el tablero original y luego se muestra cada movimiento de los vehiculos con su hitorial de bateria
// 12. initialBoard valida que no hayan solapamientos; en cambio, en la lectura se valida que no hayan vehiculos fuera de limites

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

class MonitorEstacionamiento {
    private String[][] tablero = new String[6][6];
    private List<Integer> vehiculosSinBateria = new ArrayList<>();
    private List<Vehiculo> todosLosVehiculos = new ArrayList<>();
    private List<Cargador> todosLosCargadores = new ArrayList<>();
    private volatile boolean simulacionTerminada = false;
    private boolean victoria = false;

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

        // imprimir tablero y vehiculo que se movio con su bateria restante
        imprimirTablero();
        System.out.println("Vehiculo " + v.getVehiculoId() + " se movio. Bateria: " + (v.getBateria() - 1) + "\n");

        // verificar si el vehiculo 0 llega con su parte frontal a la columna 5
        if (v.getVehiculoId() == 0 && (v.getColumna() + v.getLongitud() - 1) == 5) {
            this.simulacionTerminada = true;
            this.victoria = true;
            notifyAll();
        }

        notifyAll(); // despierta a otros (incluyendo cargadores o vehiculos esperando)
        return true;
    }

    public synchronized void reportarSinBateria(int id) {
        // agrega el id del vehiculo a la lista de varados
        if (!vehiculosSinBateria.contains(id)) {
            // prioridad de recarga al vehiculo 0
            if (id == 0) {
                vehiculosSinBateria.add(0, id);
            }
            else {
                vehiculosSinBateria.add(id);
            }

            System.out.println("Vehiculo " + id + " reportado sin bateria");
            notifyAll();
        }
    }

    public synchronized int atenderSinBateria() throws InterruptedException {

        while(vehiculosSinBateria.isEmpty() && !simulacionTerminada) {
            wait(500);
        }

        if (simulacionTerminada) return -1;

        return vehiculosSinBateria.remove(0);
    }

    public synchronized void recargaCompleta(int id) {
        for (Vehiculo vehiculo : todosLosVehiculos) {
            if (vehiculo.getVehiculoId() == id) {
                vehiculo.setBateria(10); // definimos 10 como la cantidad de bateria a recargar
                break;
            }
        }
        notifyAll();
    }

    public synchronized void esperarRecarga(int id) throws InterruptedException {
        // los vehiculos se bloquean a si mismo hasta que su bateria sea recargada
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

    // funciones de utilidad
    // inicializacion del tablero
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
            columnaFinal += vehiculo.getLongitud() - 1; 
        } else {
            filaFinal += vehiculo.getLongitud() - 1;
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
            tablero[fila][columna] = String.valueOf(vehiculo.getVehiculoId()); // llenar el tablero con el id del vehiculo

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
            if(!casillaOcupada(vehiculo)) {
                System.err.println("Solapamiento detectado en vehiculo " + vehiculo.getVehiculoId());
                System.exit(1);
            }
        }
    }

    public void agregarCargador(Cargador cargador) {
        this.todosLosCargadores.add(cargador);
    }

    public List<Cargador> getListaCargadores() {
        return this.todosLosCargadores;
    }

    public boolean getVictoria() {
        return this.victoria;
    }

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
                        Thread.sleep(300);
                    }
                }
                else {
                    System.out.println("Vehiculo " + id + " esperando recarga");
                    monitor.esperarRecarga(this.id);
                    System.out.println("Vehiculo " + id + " recargado");
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

    public Cargador(MonitorEstacionamiento monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run() {
        while (!monitor.getSimulacionTerminada()) {
            try {
                int idVarado = monitor.atenderSinBateria(); 
                if (idVarado == -1) break; 

                // NOTA: this.getName() es un metodo de Thread que retorna el nombre del hilo por defecto
                System.out.println("Cargador " + this.getName() + " atendiendo vehiculo " + idVarado);
                Thread.sleep(1000); 
                monitor.recargaCompleta(idVarado);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Cargador " + this.getName() + " termino su ejecucion");
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

                if (linea.isEmpty()) continue;

                String[] tokens = linea.split("\\s*,\\s*");

                if (tokens.length == 2 && tokens[0].equalsIgnoreCase("cargadores")) {

                    for (int i = 0; i < Integer.parseInt(tokens[1]); i++) {
                        Cargador cargador = new Cargador(monitor);
                        monitor.agregarCargador(cargador);
                        cargador.start();
                    }
                    return listaVehiculos; 
                }
                else if (tokens.length != 6) {
                    System.err.println("Error en linea " + numeroLinea + ": Formato incorrecto (se esperan 6 datos)");
                    System.exit(1);
                }
                try {
                    // extraccion de datos
                    int id = Integer.parseInt(tokens[0]);
                    char orientacion = tokens[1].toLowerCase().charAt(0);
                    int fila = Integer.parseInt(tokens[2]);
                    int columna = Integer.parseInt(tokens[3]);
                    int longitud = Integer.parseInt(tokens[4]);
                    int bateria = Integer.parseInt(tokens[5]);

                    if (orientacion != 'h' && orientacion != 'v') {
                        throw new IllegalArgumentException("Orientacion '" + orientacion + "' no permitida");
                    }
                    if (fila < 0 || fila > 5 || columna < 0 || columna > 5) {
                        throw new IllegalArgumentException("Coordenadas fuera de rango (0-5)");
                    }
                    if (orientacion == 'h' && ((columna + longitud)-1) > 5) {
                        throw new IllegalArgumentException("El vehiculo se sale del tablero horizontalmente");
                    }
                    if (orientacion == 'v' && ((fila + longitud)-1) > 5) {
                        throw new IllegalArgumentException("El vehiculo se sale del tablero verticalmente");
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

                    // crear el vehiculo y agregarlo a la lista
                    Vehiculo v = new Vehiculo(id, orientacionEnum, fila, columna, longitud, bateria, monitor);
                    listaVehiculos.add(v);

                } catch (NumberFormatException e) {
                    System.err.println("Error de formato numerico en linea " + numeroLinea);
                    System.exit(1);
                } catch (IllegalArgumentException e) {
                    System.err.println("Dato invalido en linea " + numeroLinea + ": " + e.getMessage());
                    System.exit(1);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("No se pudo encontrar el archivo '" + rutaArchivo + "'");
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
                System.err.println("Tablero sin solucion: se excedio el tiempo limite de ejecucion (" + (tiempoLimite / 60000) + " min)");
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
        long startTime = System.currentTimeMillis(); // para medir el tiempo
        MonitorEstacionamiento monitor = new MonitorEstacionamiento();
        int[] datosCarro0 = new int[3]; // [fila, columna, longitud]

        // Lectura archivos
        LectorVehiculos lector = new LectorVehiculos();
        List<Vehiculo> listaDeVehiculos = lector.leerArchivo(args[0], monitor, datosCarro0);

        int filaCarro0 = datosCarro0[0];
        int colCarro0 = datosCarro0[1];
        int longitudCarro0 = datosCarro0[2];

        // Identificar tablero sin solucion
        // 1. Hay otro carro H en la misma fila que el carro 0 que bloquea su camino hacia la salida
        int cabezaColumnaCarro0 = colCarro0 + longitudCarro0; // recalculo cabeza carro 0

        for (Vehiculo vehiculo : listaDeVehiculos) {
            if (vehiculo.getId() != 0 && vehiculo.getFila() == filaCarro0 && vehiculo.getOrientacion() == Vehiculo.Orientacion.h && vehiculo.getColumna() >= cabezaColumnaCarro0) {
                System.err.println("Tablero sin solucion: el vehiculo " + vehiculo.getVehiculoId() + " bloquea horizontalmente al carro 0 en la fila " + filaCarro0);
                System.exit(1);
            }
        }

        // 2. Hay toda una linea de carros V que ocupa toda la columna delande del carro 0
        for (int col = cabezaColumnaCarro0; col < 6; col++) {
            int celdasOcupadasEnColumna = 0;
            for (Vehiculo vehiculo : listaDeVehiculos) {
                if (vehiculo.getOrientacion() == Vehiculo.Orientacion.v && vehiculo.getColumna() == col) {
                    celdasOcupadasEnColumna += vehiculo.getLongitud();
                }
            }

            if (celdasOcupadasEnColumna == 6) {
                System.err.println("Tablero sin solucion: La columna " + col + " esta totalmente bloqueada por vehiculos verticales");
                System.exit(1);
            }
        }

        monitor.setListaVehiculos(listaDeVehiculos);
        monitor.initialBoard();

        // imprimir tablero inicial (original)
        monitor.imprimirTablero();
        System.out.println("Tablero inicial\n");

        // 3. En caso de que no se encuentre solucion en 10 min, asumimos que el tablero no tiene solucion y se termina la simulacion
        GuardianSolucion guardian = new GuardianSolucion(monitor, 10 * 60000);
        guardian.start();

        for (Vehiculo v : listaDeVehiculos) {
            v.start();
        }

        // Bucle de espera hasta que el monitor marque el fin
        while (!monitor.getSimulacionTerminada()) {
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        synchronized (monitor) {
            monitor.notifyAll(); // despertar a todos los hilos para que terminen su ejecucion
        }

        // main espera que todos los cargadores se despidan
        for (Cargador cargador : monitor.getListaCargadores()) {
            try {
                cargador.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // Calculo del tiempo de ejecución
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // imprimir en orden (asegurando llegada de vehiculo 0)
        if (monitor.getVictoria()) {
            System.out.println("\nVehiculo 0 llego a la salida");
        }
        System.out.println("\nSimulacion terminada");
        System.out.println("Tiempo total de ejecucion: " + duration + " ms");
        System.out.println("En segundos: " + (duration / 1000.0) + " s\n");

        System.exit(0); 
    }
}

// ejemplo de comando para compilar y correr el programa sin el Makefile
// javac 30821017-Herrera-30407195-Araujo-Proyecto_3.java
// java RushHour <ruta_del_archivo.txt>