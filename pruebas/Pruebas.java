import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Pruebas de los requisitos básicos, sin dependencias de testing ni scripts. */
public class Pruebas {
  private static int comprobaciones;
  private static final String ENCABEZADO = String.join("\n",
      "PLAN \"Prueba\"", "Nivel: INTERMEDIO", "Objetivos: [Fuerza, Salud general]",
      "Semanas: 8", "Profesor: \"Docente\"", "Fecha: 20/08/2026", "");
  private static final String CAMPOS = "Intensidad: 4/5\nDuración: 60m\nMúsculos: [Pecho, Core]\n";
  private static final String RUTINA = "RUTINA \"A\"\nDía: Lunes\nEJERCICIOS:\npress banca: 4x8 @ 60kg, descanso 2m\n" + CAMPOS;
  private static final String BASE = ENCABEZADO + RUTINA;
  private static final String SUPER = "brazos y core SUPERSERIE 3 vueltas\n"
      + "curl bíceps: 12 reps @ 15kg\nplancha: 30s\nFIN SUPERSERIE, descanso 90s\n";

  private static void comprobar(boolean condicion, String caso) {
    comprobaciones++;
    if (!condicion) throw new AssertionError(caso);
  }

  private static Main.Resultado analizar(String texto) throws Exception {
    return Main.analizar(new StringReader(texto));
  }

  private static Main.Resultado valido(String texto, String caso) throws Exception {
    Main.Resultado resultado = analizar(texto);
    comprobar(resultado.esValido(), caso + ": " + resultado.diagnosticos.errores());
    return resultado;
  }

  private static Main.Resultado invalido(String texto, String tipo, String caso) throws Exception {
    Main.Resultado resultado = analizar(texto);
    comprobar(!resultado.esValido(), caso + ": se aceptó una entrada incorrecta");
    comprobar(resultado.diagnosticos.errores().stream().anyMatch(e -> e.tipo.equals(tipo)),
        caso + ": falta el diagnóstico " + tipo + ": " + resultado.diagnosticos.errores());
    comprobar(resultado.diagnosticos.errores().stream().allMatch(e -> e.linea >= 1),
        caso + ": un error no tiene línea");
    return resultado;
  }

  private static String conEjercicios(String ejercicios) {
    return ENCABEZADO + "RUTINA \"A\"\nDía: Lunes\nEJERCICIOS:\n" + ejercicios + CAMPOS;
  }

  private static String copia(String modificador) {
    return "RUTINA \"Copia\"\nDía: Martes\nIgual a \"A\"" + modificador + "\n";
  }

  private static void recupera(String rutinaIncorrecta, String tipo, String caso) throws Exception {
    String texto = ENCABEZADO + rutinaIncorrecta + RUTINA.replace("\"A\"", "\"Siguiente\"");
    Main.Resultado resultado = invalido(texto, tipo, caso);
    comprobar(resultado.plan != null && resultado.plan.rutinas.stream().anyMatch(r -> r.nombre.equals("Siguiente")),
        caso + ": no se analizó la rutina siguiente");
  }

  public static void main(String[] args) throws Exception {
    Main.Resultado base = valido(BASE, "Plan básico");
    comprobar(base.plan.semanas.intValue() == 8 && base.plan.fecha.getYear() == 2026, "Datos del encabezado");
    comprobar(base.plan.rutinas.get(0).dia.equals("Lunes"), "Conservar el día");
    comprobar(base.plan.rutinas.get(0).ejercicios.get(0).especificacion.carga.cantidad.intValue() == 60, "Conservar la carga");
    valido(BASE.stripTrailing(), "Fin de archivo sin salto de línea");
    valido(BASE.replace("\n", "\r\n"), "Saltos de línea Windows");
    valido("\n\n" + BASE, "Líneas en blanco");

    String oficial = Files.readString(Paths.get("plan.txt"), StandardCharsets.UTF_8);
    Main.Resultado ejemplo = valido(oficial, "Ejemplo completo de la consigna");
    comprobar(ejemplo.plan.rutinas.size() == 4, "Cuatro rutinas del ejemplo");
    comprobar(ejemplo.plan.rutinas.get(3).ejercicios.size() == 3, "EXCEPTO del ejemplo oficial");
    comprobar(ejemplo.plan.rutinas.get(3).efectivos.musculos.contains("Pecho"), "Herencia del ejemplo oficial");
    String errores = Files.readString(Paths.get("pruebas/errores.txt"), StandardCharsets.UTF_8);
    Main.Resultado conErrores = invalido(errores, "léxico", "Ejemplo de errores recuperables");
    comprobar(conErrores.diagnosticos.errores().stream().anyMatch(e -> e.tipo.equals("sintáctico")), "Errores sintácticos del ejemplo");
    comprobar(conErrores.diagnosticos.errores().stream().anyMatch(e -> e.tipo.equals("semántico")), "Errores semánticos del ejemplo");
    comprobar(conErrores.plan.rutinas.stream().anyMatch(r -> r.nombre.equals("Rutina correcta posterior")), "Continuar tras varias rutinas incorrectas");

    for (String semanas : new String[] {"1", "52"}) valido(BASE.replace("Semanas: 8", "Semanas: " + semanas), "Semanas " + semanas);
    for (String semanas : new String[] {"0", "53", "999999999999999999999"}) {
      invalido(BASE.replace("Semanas: 8", "Semanas: " + semanas), "semántico", "Semanas fuera de rango");
    }
    for (String ejercicio : new String[] {"press: 0x8\n", "press: 4x0\n", "dominadas: 0xMAX\n", "plancha: 0x30s\n", "curl: 0 reps\n"}) {
      invalido(conEjercicios(ejercicio), "semántico", "Series o repeticiones cero");
    }
    valido(conEjercicios("press: 999999999999999999999x8\n"), "Enteros grandes sin desbordamiento");

    for (String carga : new String[] {"28kg", "9lb", "23%1RM", "100%1RM"}) {
      valido(conEjercicios("press: 4x8 @ " + carga + "\n"), "Carga " + carga);
    }
    invalido(conEjercicios("press: 4x8 @ 101%1RM\n"), "semántico", "Porcentaje de 1RM mayor a 100");
    for (String tiempo : new String[] {"1h", "25m", "30s"}) valido(conEjercicios("bicicleta: " + tiempo + "\n"), "Tiempo " + tiempo);
    valido(conEjercicios("dominadas: 3xMAX, descanso 2m\nplancha: 3x30s, descanso 30s\nbicicleta: 15m @ 22km/h\n"), "Fallo, isométrico y cardio");
    invalido(conEjercicios("press: 4x8 @ 60 kg\n"), "léxico", "Unidad separada del número");

    for (String intensidad : new String[] {"BAJA", "MEDIA", "ALTA", "*", "**", "***", "****", "*****", "1/5", "2/5", "3/5", "4/5", "5/5"}) {
      valido(BASE.replace("Intensidad: 4/5", "Intensidad: " + intensidad), "Intensidad " + intensidad);
    }
    invalido(BASE.replace("4/5", "6/5"), "sintáctico", "Intensidad fuera de rango");
    invalido(BASE.replace("4/5", "******"), "sintáctico", "Demasiadas estrellas");

    valido(conEjercicios(SUPER), "Superserie con nombre");
    valido(conEjercicios(SUPER.replace("brazos y core SUPERSERIE", "SUPERSERIE")), "Superserie sin nombre");
    valido(conEjercicios(SUPER.replace("SUPERSERIE", "sUpErSeRiE").replace("FIN", "fIn")), "Reservadas sin distinguir mayúsculas");
    invalido(conEjercicios(SUPER.replace("3 vueltas", "1 vueltas")), "semántico", "Una sola vuelta");
    invalido(conEjercicios(SUPER.replace("plancha: 30s\n", "")), "sintáctico", "Un solo ejercicio en superserie");
    invalido(conEjercicios(SUPER.replace("FIN SUPERSERIE, descanso 90s\n", "")), "sintáctico", "Superserie sin cierre");

    for (String fecha : new String[] {"29/02/2024", "1/1/2026", "31/12/2026"}) valido(BASE.replace("20/08/2026", fecha), "Fecha real " + fecha);
    for (String fecha : new String[] {"29/02/2025", "30/02/2026", "31/11/2026", "1/18/2026", "0/1/2026", "1/1/0000"}) {
      Main.Resultado resultado = invalido(BASE.replace("20/08/2026", fecha), "semántico", "Fecha inexistente " + fecha);
      comprobar(resultado.diagnosticos.errores().stream().anyMatch(e -> e.linea == 6), "Línea exacta de la fecha");
    }
    invalido(BASE.replace("20/08/2026", "20-08-2026"), "sintáctico", "Sin formatos alternativos de fecha");

    String original = conEjercicios("press banca: 4x8\n" + SUPER)
        + "Calentamiento: Movilidad de hombros\nEquipamiento: [Bicicleta fija, Banco plano]\n"
        + "Variantes: [press inclinado, Rutina alternativa]\nExtra: Objetivo=Fuerza\n"
        + "Extra: Apto=Personas sin experiencia\nObs: Progresar de a poco: sin dolor.\n";
    Main.Resultado reutilizado = valido(original + copia(" x 2 veces EXCEPTO [curl bíceps]")
        + "Intensidad: BAJA\nExtra: Objetivo=Salud\n", "Herencia, exclusión puntual y campos redeclarados");
    PlanEntrenamiento.Rutina a = reutilizado.plan.rutinas.get(0), b = reutilizado.plan.rutinas.get(1);
    comprobar(b.vueltasCircuito.equals(BigInteger.valueOf(2)), "Vueltas completas del circuito");
    comprobar(b.ejercicios.get(1).ejercicios.size() == 1, "Excluir ejercicio dentro de superserie");
    comprobar(a.ejercicios.get(1).ejercicios.size() == 2, "La copia no modifica el original");
    comprobar(b.efectivos.intensidad.equals("BAJA") && a.efectivos.intensidad.equals("4/5"), "Redefinir intensidad");
    comprobar(b.efectivos.calentamiento.equals("Movilidad de hombros") && b.efectivos.duracion.cantidad.intValue() == 60, "Heredar calentamiento y duración");
    comprobar(b.efectivos.equipamiento.contains("Bicicleta fija") && b.efectivos.variantes.contains("press inclinado"), "Listas de varias palabras y herencia");
    comprobar(b.efectivos.extra.get("Objetivo").equals("Salud") && b.efectivos.extra.get("Apto").equals("Personas sin experiencia"), "Herencia de Extra por etiqueta");
    comprobar(b.efectivos.observaciones.equals("Progresar de a poco: sin dolor."), "Observaciones libres");
    Main.Resultado excluida = valido(original + copia(" EXCEPTO [brazos y core]"), "Excluir superserie completa");
    comprobar(excluida.plan.rutinas.get(1).ejercicios.size() == 1, "Superserie excluida");
    comprobar(excluida.plan.rutinas.get(1).vueltasCircuito.equals(BigInteger.ONE), "Una vuelta por defecto");
    Main.Resultado redefinida = valido(original + copia("") + "Calentamiento: Nuevo calentamiento\n"
        + "Duración: 35m\nMúsculos: [Piernas]\nEquipamiento: [Soga]\nVariantes: [Otra]\n"
        + "Obs: Nueva observación\n", "Redefinir todos los campos heredables");
    PlanEntrenamiento.Campos nuevos = redefinida.plan.rutinas.get(1).efectivos;
    comprobar(nuevos.calentamiento.equals("Nuevo calentamiento") && nuevos.duracion.cantidad.intValue() == 35,
        "Redefinir calentamiento y duración");
    comprobar(nuevos.musculos.size() == 1 && nuevos.musculos.contains("Piernas")
        && nuevos.equipamiento.size() == 1 && nuevos.equipamiento.contains("Soga")
        && nuevos.variantes.size() == 1 && nuevos.variantes.contains("Otra"), "Reemplazar listas heredadas");
    comprobar(nuevos.observaciones.equals("Nueva observación"), "Redefinir observaciones");
    Main.Resultado cadena = valido(BASE + copia(" x 2 veces")
        + "RUTINA \"Cadena\"\nDía: Viernes\nIgual a \"Copia\" x 3 veces\n", "Cadena de reutilización");
    comprobar(cadena.plan.rutinas.get(2).vueltasCircuito.intValue() == 6, "Multiplicar vueltas de una cadena");
    for (String modificador : new String[] {" x 1 vez", " x 3 veces", " 100%", " 200%"}) valido(BASE + copia(modificador), "Reutilización " + modificador);
    comprobar(valido(BASE + copia(" 200%"), "Porcentaje equivalente").plan.rutinas.get(1).vueltasCircuito.intValue() == 2, "200% equivale a dos vueltas");
    for (String modificador : new String[] {" x 0 veces", " x 2 vez", " x 1 veces", " 0%", " 50%", " 150%"}) {
      invalido(BASE + copia(modificador), "semántico", "Reutilización inválida " + modificador);
    }
    // El adicional A queda fuera: no se comprueban referencias, variantes o exclusiones inexistentes.
    valido(BASE + copia("").replace("Igual a \"A\"", "Igual a \"No declarada\""), "Referencia conservada sin validar existencia");
    valido(BASE + copia(" EXCEPTO [No existe]"), "Sin validación adicional de exclusiones existentes");

    for (String nombre : new String[] {"FIN", "press fin sentado", "SUPERSERIE"}) {
      invalido(BASE.replace("RUTINA \"A\"", "RUTINA \"" + nombre + "\""), "semántico", "Nombre de rutina reservado");
    }
    invalido(conEjercicios("press FIN sentado: 4x8\n"), "semántico", "Nombre de ejercicio reservado");
    valido(conEjercicios("curl con mancuernas (alternado) 21: 3x8\n"), "Nombre libre con números y puntuación");
    valido(BASE.replace("\"A\"", "\"Finanzas\""), "Reservada solo como palabra completa");

    invalido(BASE.replace("Nivel: INTERMEDIO\n", ""), "sintáctico", "Nivel obligatorio");
    invalido(BASE.replace("Objetivos: [Fuerza, Salud general]\n", ""), "sintáctico", "Objetivos obligatorios");
    for (String campo : new String[] {"Intensidad: 4/5\n", "Duración: 60m\n", "Músculos: [Pecho, Core]\n"}) {
      invalido(BASE.replace(campo, ""), "semántico", "Campo obligatorio de rutina nueva");
    }
    invalido(ENCABEZADO, "sintáctico", "Plan sin rutinas");

    recupera(RUTINA.replace("4x8 @ 60kg, descanso 2m", "4x"), "sintáctico", "Recuperar ejercicio incompleto");
    recupera(RUTINA.replace("60kg", "?60kg"), "léxico", "Recuperar carácter inválido");
    recupera(RUTINA.replace("4x8", "0x8"), "semántico", "Continuar tras error semántico");
    recupera(RUTINA.replace("Día: Lunes", "Día:"), "sintáctico", "Recuperar día ausente");
    recupera(RUTINA.replace("[Pecho, Core]", "[Pecho, Core"), "sintáctico", "Recuperar lista sin cierre");
    recupera("RUTINA \"Mala\"\nDía: Lunes\nEJERCICIOS:\n" + SUPER.replace("FIN SUPERSERIE, descanso 90s\n", ""), "sintáctico", "Recuperar superserie sin cierre");
    Main.Resultado finales = invalido(BASE + RUTINA.replace("4x8", "4x"), "sintáctico", "Última rutina incorrecta");
    comprobar(finales.plan != null && finales.plan.rutinas.size() == 1, "Conservar las rutinas previas");
    Main.Resultado linea = invalido(BASE.replace("4x8", "0x8"), "semántico", "Línea del ejercicio");
    comprobar(linea.diagnosticos.errores().stream().anyMatch(e -> e.linea == 10), "Línea exacta del ejercicio");
    Main.Resultado sintaxis = invalido(BASE.replace("4x8 @ 60kg, descanso 2m", "4x"), "sintáctico", "Línea sintáctica");
    comprobar(sintaxis.diagnosticos.errores().stream().anyMatch(e -> e.tipo.equals("sintáctico") && e.linea == 10), "Línea exacta del error sintáctico");
    Main.Resultado lexico = invalido(BASE.replace("60kg", "?60kg"), "léxico", "Línea léxica");
    comprobar(lexico.diagnosticos.errores().stream().anyMatch(e -> e.tipo.equals("léxico") && e.linea == 10), "Línea exacta del error léxico");

    System.out.println("Pruebas correctas: " + comprobaciones + " comprobaciones de requisitos básicos.");
  }
}
