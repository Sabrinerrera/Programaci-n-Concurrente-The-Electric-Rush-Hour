# Makefile para compilar y ejecutar el programa de Rush Hour
JC = javac
JVM = java
SOURCE = 30821017-Herrera-30407195-Araujo-Proyecto_3.java
MAIN = RushHour

# ejercuta escribiendo solo "make" en la terminal
all: compile

# regla para compilar el código fuente
compile:
	$(JC) $(SOURCE)

# regla para ejecutar el programa
run: compile
	$(JVM) $(MAIN) $(ARGS)

# regla para limpiar el directorio eliminando los archivos compilados (.class)
clean:
	rm -f *.class