# Analizador de planes de entrenamiento

Requiere un JDK instalado y los comandos `java` y `javac` disponibles en la terminal.

Desde la carpeta del proyecto, ejecutar en PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\probar.ps1
```

El script descarga JFlex 1.9.1 desde Maven Central la primera vez, genera el lexer y el parser, compila y ejecuta `Main` con `plan.txt`. Necesita Internet para esa primera descarga. Los archivos generados quedan en `build/` y la herramienta descargada en `.tools/`.

Para analizar otro archivo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\probar.ps1 -Archivo .\otro-plan.txt
```

Guardar los archivos de entrada en UTF-8 y terminar la ultima linea con un salto de linea.

`Main` usa `plan.txt` cuando no recibe argumentos. Ese archivo contiene el plan de prueba y debe cumplir la gramatica de `parser.cup`. Actualmente las listas solo admiten elementos de una palabra: el ejemplo oficial con `Bicicleta fija` requiere corregir esa regla antes de poder ser aceptado.

Para ejecutar desde un IDE, configurar un proyecto Java con un JDK, incluir `build/generated` como carpeta de fuentes y `java-cup-11b-runtime (1).jar` como biblioteca. La clase principal es `Main` y el directorio de trabajo debe ser la carpeta del proyecto. Ejecutar el script nuevamente cuando cambien `scanner.jflex` o `parser.cup`.
