# Analizador de planes de entrenamiento

Requiere un JDK instalado y los comandos `java` y `javac` disponibles en la terminal.

Desde la carpeta del proyecto, preparar las carpetas y descargar JFlex una sola vez en PowerShell:

```powershell
New-Item -ItemType Directory -Force -Path .tools, build/generated, build/classes | Out-Null
Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/de/jflex/jflex/1.9.1/jflex-1.9.1.jar" -OutFile .tools/jflex-1.9.1.jar
```

Generar el parser y el lexer, compilar y ejecutar `Main` con `plan.txt`. Ejecutar cada comando solo si el anterior termino correctamente:

```powershell
java -jar "java-cup-11b (1).jar" -destdir build/generated parser.cup
java -cp ".tools/jflex-1.9.1.jar;java-cup-11b-runtime (1).jar" jflex.Main --encoding UTF-8 --nobak -d build/generated scanner.jflex
javac -encoding UTF-8 -cp "java-cup-11b-runtime (1).jar" -d build/classes Main.java build/generated/Lexer.java build/generated/parser.java build/generated/sym.java
java -cp "build/classes;java-cup-11b-runtime (1).jar" Main plan.txt
```

Los archivos generados quedan en `build/` y JFlex en `.tools/`.

Para analizar otro archivo:

```powershell
java -cp "build/classes;java-cup-11b-runtime (1).jar" Main otro-plan.txt
```

Guardar los archivos de entrada en UTF-8 y terminar la ultima linea con un salto de linea.

`Main` usa `plan.txt` cuando no recibe argumentos. Ese archivo contiene el plan de prueba y debe cumplir la gramatica de `parser.cup`. Actualmente las listas solo admiten elementos de una palabra: el ejemplo oficial con `Bicicleta fija` requiere corregir esa regla antes de poder ser aceptado.

Para ejecutar desde un IDE, configurar un proyecto Java con un JDK, incluir la carpeta del proyecto y `build/generated` como carpetas de fuentes y `java-cup-11b-runtime (1).jar` como biblioteca. La clase principal es `Main` y el directorio de trabajo debe ser la carpeta del proyecto. Volver a generar y compilar cuando cambien `scanner.jflex` o `parser.cup`.
