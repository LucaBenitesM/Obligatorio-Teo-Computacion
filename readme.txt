OBLIGATORIO - ANALIZADOR DE PLANES DE ENTRENAMIENTO
====================================================

Grupo: [COMPLETAR NUMERO DE GRUPO]

PARTICIPANTES
-------------
- Luca Benites
- Felipe Lyonnet
- Felipe Contreras
- Juan Martin Sommer


SECCIONES COMPLETADAS
---------------------
Requisitos basicos: todos (1 al 10)
  1. Estructura completa del plan (encabezado y rutinas)
  2. Los 5 tipos de ejercicio (fuerza, al fallo, isometrico, cardio, superserie)
  3. Intensidad en 3 notaciones (texto, estrellas, n/5)
  4. Unidades: carga (kg, lb, %1RM), tiempo (h, m, s) y velocidad (km/h)
  5. Validacion de valores numericos (semanas 1-52, series/reps positivas, %1RM <= 100)
  6. Validacion de superserie (minimo 2 ejercicios, 2 vueltas, FIN SUPERSERIE)
  7. Reutilizacion "Igual a" con vez/veces, N% y EXCEPTO
  8. Extra: etiqueta=valor y observaciones
  9. Validacion de fecha real
 10. Errores lexicos, sintacticos y semanticos con linea; se continua con la
     rutina siguiente ante un error dentro de una rutina

Requisitos adicionales implementados:
  A. Variantes y referencias existentes
     (variantes existen; Igual a referencia una rutina existente, declarada antes,
      sin autorreferencia ni ciclos; los nombres de EXCEPTO existen)
  B. Conteo de ejercicios y promedio por dia
  E. Calendario semanal ("semana tipo")
  F. Resumen del plan (frecuencia por musculo, equipamiento, tiempo semanal,
     intensidad promedio)
  I. Formatos alternativos de fecha
     (20/08/2026, 22-8-2026, 22.8.2026, 22-oct-2026, 22 de enero de 2026)


DECISIONES DE DISENO
--------------------
- %1RM no se convierte a kg/lb: se deja como carga relativa, sin conversion.
- Porcentaje en Igual a: se aceptan solo multiplos de 100 (100% = x 1 vez,
  200% = x 2 veces). Otros valores se rechazan con error semantico.
- Herencia con Igual a: todo campo no redeclarado en el bloque se hereda de la
  rutina referenciada; los campos redeclarados reemplazan al heredado.
- Conteo de ejercicios con "Igual a ... x N veces": el total de la rutina
  referenciada (menos lo indicado en EXCEPTO) se multiplica por N, porque N son
  vueltas completas del circuito. Con x 1 vez coincide con la regla literal.
- EXCEPTO sobre un ejercicio dentro de una superserie resta 1 por cada vuelta
  de esa superserie. EXCEPTO sobre una superserie nombrada resta su total completo.
- Intensidad promedio: BAJA = 1, MEDIA = 3, ALTA = 5; estrellas y n/5 ya estan
  en escala 1 a 5. Se informa el promedio simple y el ponderado por duracion.


ARCHIVOS
--------
scanner.jflex          Analizador lexico (JFlex)
parser.cup             Analizador sintactico (CUP)
Main.java              Programa principal
PlanEntrenamiento.java AST (datos del plan)
Validador.java         Validaciones semanticas
Diagnosticos.java      Registro de errores
Fechas.java            Parseo y validacion de fechas (requisito I)
Analisis.java          Requisitos A, B, E y F
Lexer.java             GENERADO por JFlex a partir de scanner.jflex
parser.java, sym.java  GENERADOS por CUP a partir de parser.cup
java-cup-11b.jar, java-cup-11b-runtime.jar   Librerias de CUP
plan.txt y carpeta pruebas/   Archivos de prueba


REQUISITOS PARA EJECUTAR
------------------------
- JDK 9 o superior (se probo con JDK 21). El comando "java" y el comando
  "javac" deben ser de la misma version (verificar con java -version y
  javac -version).
- JFlex 1.9.x (variable de entorno JFLEX_HOME apuntando a su carpeta).
- Los comandos son para PowerShell (Windows), ejecutados dentro de la carpeta
  donde se descomprimio el zip. En Linux o Mac usar ":" en lugar de ";" en el
  classpath.
- Los nombres de los jar de CUP deben coincidir con los de los comandos. Si en
  la carpeta tienen otro nombre, ajustarlos en los comandos.


COMANDOS PASO A PASO
--------------------
Los archivos Lexer.java, parser.java y sym.java ya vienen generados, asi que
los pasos 1 y 2 solo son necesarios si se modifica scanner.jflex o parser.cup.

0. Forzar UTF-8 (necesario por los acentos en scanner.jflex):

   $env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

1. Generar el lexer con JFlex:

   & "$env:JFLEX_HOME\bin\jflex.bat" scanner.jflex

2. Generar el parser con CUP:

   java -jar "java-cup-11b (1).jar" -parser parser -symbols sym parser.cup

3. Compilar:

   javac -encoding UTF-8 -cp ".;java-cup-11b-runtime (1).jar" *.java

4. Ejecutar un plan (si no se indica archivo, usa plan.txt):

   java -cp ".;java-cup-11b-runtime (1).jar" Main plan.txt

   Si el comando "java" del sistema es de una version mas vieja que "javac",
   usar el java.exe que esta junto a javac:

   & "$((Get-Command javac).Source | Split-Path)\java.exe" -cp ".;java-cup-11b-runtime (1).jar" Main plan.txt


EJEMPLOS DE PRUEBA
------------------
- plan.txt: plan valido del enunciado (con fecha en formato largo y Variantes).
  Salida esperada: sin errores, "Plan de entrenamiento: estructura reconocida.",
  conteo (total 24 ejercicios, promedio 6.00), semana tipo y resumen.

- carpeta pruebas/: [COMPLETAR: listar cada archivo y que error o caso prueba]
  Ejemplo:
    java -cp ".;java-cup-11b-runtime (1).jar" Main pruebas\NOMBRE_ARCHIVO.txt

Casos de error cubiertos: fecha inexistente (31/11/2026), EXCEPTO con un
nombre inexistente, variante inexistente, Igual a con una rutina inexistente,
autorreferencia, referencia a una rutina declarada mas abajo.
