# Makefile para compilar y ejecutar el programa de Rush Hour
JC = javac
JVM = java
SOURCE = 30821017-Herrera-30407195-Araujo-Proyecto_3.java
MAIN = RushHour

# ejecutar escribiendo solo "make" en la terminal
all: compile

# regla para compilar el código fuente
compile:
	$(JC) $(SOURCE)

# regla para ejecutar el programa
run: compile
	$(JVM) $(MAIN) $(ARGS)

# Permite ejecutar con: make test1, make test2, etc.
test1: compile
	$(JVM) $(MAIN) pruebas1.txt

test2: compile
	$(JVM) $(MAIN) pruebas2.txt

test3: compile
	$(JVM) $(MAIN) pruebas3.txt

test4: compile
	$(JVM) $(MAIN) pruebas4.txt

test5: compile
	$(JVM) $(MAIN) pruebas5.txt

test6: compile
	$(JVM) $(MAIN) pruebas6.txt

test7: compile
	$(JVM) $(MAIN) pruebas7.txt

test8: compile
	$(JVM) $(MAIN) pruebas8.txt

# regla para limpiar el directorio eliminando los archivos compilados (.class)
clean:
	rm -f *.class