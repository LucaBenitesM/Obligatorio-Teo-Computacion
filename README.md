# Analizador de planes de entrenamiento

Implementación de los requisitos básicos de la consigna, con JFlex y CUP. No implementa los requisitos adicionales.

## Preparación

Se necesita un JDK 11 o posterior, con `java` y `javac` disponibles en la terminal. Fue verificado con JDK 25.0.2, JFlex 1.9.1 y CUP 0.11b. Los dos JAR de CUP están incluidos en el repositorio.

Desde la carpeta del proyecto, preparar los directorios y descargar JFlex una sola vez en PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path .tools, build/generated, build/classes | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/de/jflex/jflex/1.9.1/jflex-1.9.1.jar" -OutFile .tools/jflex-1.9.1.jar
```

## Generar, compilar y ejecutar

Ejecutar cada comando solo si el anterior terminó correctamente:

```powershell
java -jar "java-cup-11b (1).jar" -destdir build/generated parser.cup
java -cp ".tools/jflex-1.9.1.jar;java-cup-11b-runtime (1).jar" jflex.Main --encoding UTF-8 --nobak -d build/generated scanner.jflex
javac -encoding UTF-8 -cp "java-cup-11b-runtime (1).jar" -d build/classes Main.java Diagnosticos.java PlanEntrenamiento.java Validador.java build/generated/Lexer.java build/generated/parser.java build/generated/sym.java
java -cp "build/classes;java-cup-11b-runtime (1).jar" Main plan.txt
```

El lexer, el parser y las clases compiladas quedan en `build/`. JFlex queda en `.tools/`. Esas carpetas no se versionan. La biblioteca de CUP puede producir un aviso de API obsoleta durante la compilación; no es un error.

Para analizar otro archivo:

```powershell
java -cp "build/classes;java-cup-11b-runtime (1).jar" Main otro-plan.txt
```

Guardar las entradas en UTF-8. Se admiten saltos de línea de Windows o Unix y una última línea sin salto final. `Main` usa `plan.txt` cuando no recibe argumentos.

Si solo cambia el archivo de entrada, basta con repetir el comando `java`. Si cambia código Java, volver a compilar. Si cambian `scanner.jflex` o `parser.cup`, volver a generar y compilar.

Para ejecutar desde un IDE, configurar un módulo Java con un JDK y añadir los fuentes Java del proyecto y de `build/generated`, excluyendo `build/classes` y `out`. Añadir `java-cup-11b-runtime (1).jar` como biblioteca. La clase principal es `Main` y el directorio de trabajo es la carpeta del proyecto. La generación inicial con JFlex y CUP sigue siendo necesaria.

## Archivos y responsabilidades

| Archivo | Responsabilidad |
| --- | --- |
| `scanner.jflex` | Reconoce los tokens, conserva sus valores y líneas, y registra errores léxicos. |
| `parser.cup` | Reconoce el encabezado, los ejercicios y las rutinas; recupera errores en el siguiente bloque `RUTINA`. |
| `PlanEntrenamiento.java` | Representa los datos del plan, las especificaciones, las superseries y las rutinas reutilizadas. |
| `Validador.java` | Valida números, fechas y nombres; aplica la reutilización, las exclusiones y la herencia. |
| `Diagnosticos.java` | Almacena errores léxicos, sintácticos y semánticos con su línea. |
| `Main.java` | Abre la entrada, ejecuta el análisis y comunica el resultado. |
| `plan.txt` | Ejemplo completo de cuatro rutinas de la consigna. |
| `pruebas/errores.txt` | Entrada con errores de las tres categorías y una rutina correcta posterior. |
| `pruebas/Pruebas.java` | Pruebas automáticas de los requisitos básicos, sin dependencias externas de testing. |

## Criterios de los requisitos básicos

- El encabezado exige `PLAN`, `Nivel`, `Objetivos`, `Semanas`, `Profesor` y `Fecha`, en ese orden.
- Cada rutina exige nombre y día. Una rutina nueva declara ejercicios, intensidad, duración y músculos. Calentamiento, equipamiento, variantes, extras y observaciones son opcionales.
- Se reconocen fuerza, fallo, isométricos y cardio, además de bloques de superserie. Dentro de los ejercicios también se admite la forma `12 reps`, usada en el ejemplo oficial.
- Las unidades básicas se escriben pegadas a enteros: `kg`, `lb`, `%1RM`, `h`, `m`, `s` y `km/h`. No se implementan tiempos compuestos, decimales ni formatos alternativos.
- `Semanas` está entre 1 y 52; series y repeticiones son positivas; la carga porcentual no supera `100%1RM`. Los enteros se conservan sin desbordamiento con `BigInteger`.
- La fecha usa `d/M/aaaa` o `dd/MM/aaaa`, con cuatro cifras para el año, y se comprueba que exista, incluidos los años bisiestos. No se admite año cero.
- La intensidad admite `BAJA`, `MEDIA`, `ALTA`, de una a cinco estrellas o de `1/5` a `5/5`.
- Las superseries tienen nombre opcional, al menos dos ejercicios y dos vueltas, y cierre explícito. `SUPERSERIE` y `FIN SUPERSERIE` no distinguen mayúsculas. `FIN` y `SUPERSERIE` no pueden aparecer como palabras completas en los nombres de rutinas, ejercicios o superseries.
- `Igual a` utiliza una vuelta por defecto. `x 1 vez` y `x N veces` deben concordar y las cantidades son positivas. `100%` equivale a una vuelta y `200%` a dos. Se rechazan porcentajes que no sean múltiplos positivos de 100.
- La reutilización de una rutina anterior copia sus ejercicios, aplica `EXCEPTO` a ejercicios puntuales o superseries completas, y conserva el factor de vueltas del circuito. Las copias no modifican el original. En una cadena de reutilizaciones los factores de vueltas se multiplican.
- Se heredan calentamiento, intensidad, duración, músculos, equipamiento, variantes, extras y observaciones. Los campos redeclarados reemplazan el valor heredado; los extras se combinan por etiqueta, y una etiqueta redeclarada reemplaza su valor. Nombre y día corresponden siempre a la nueva rutina. Las exclusiones no recalculan duración ni músculos.
- Calentamiento y observaciones admiten texto con o sin comillas. `Extra: etiqueta=valor` conserva el valor como texto, sin exigir un catálogo de etiquetas.
- Un error léxico deja el archivo marcado como incorrecto. Un error sintáctico dentro de una rutina descarta ese bloque y continúa en la siguiente `RUTINA`. Los errores semánticos se acumulan sin detener las rutinas siguientes. Si el encabezado no puede reconocerse, el archivo se rechaza.
- El proceso devuelve código `0` para un plan aceptado, `1` para errores en el contenido o lectura y `2` para argumentos incorrectos. No imprime aceptación si hubo errores.

Las cargas en `%1RM` se conservan con su unidad; no se convierten a kg/lb ni se calcula volumen.

## Límites de alcance

El adicional A no está implementado: no se comprueba existencia de variantes, referencias o exclusiones, ni se diagnostican autorreferencias o ciclos. Una referencia que no se encuentre entre las rutinas anteriores se conserva sin resolver; su comprobación queda para ese adicional. No se generan estadísticas, conteos por día, calendario, volumen, resúmenes, progresiones ni formatos alternativos de fecha o tiempo.

## Pruebas

Después de generar y compilar el proyecto:

```powershell
javac -encoding UTF-8 -cp "build/classes;java-cup-11b-runtime (1).jar" -d build/classes pruebas/Pruebas.java
java -cp "build/classes;java-cup-11b-runtime (1).jar" Pruebas
```

Las pruebas verifican el ejemplo completo, datos almacenados, límites numéricos, fechas bisiestas, las tres notaciones de intensidad, unidades, superseries con y sin nombre, herencia, exclusiones y continuación después de errores. Deben ejecutarse desde la carpeta del proyecto.

Para ver los mensajes de error por línea:

```powershell
java -cp "build/classes;java-cup-11b-runtime (1).jar" Main pruebas/errores.txt
```

Este último archivo debe rechazarse: contiene errores deliberados. La prueba automática comprueba que también se analiza su rutina correcta posterior.
