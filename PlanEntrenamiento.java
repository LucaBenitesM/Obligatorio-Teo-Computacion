import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Datos del lenguaje. No calcula estadísticas ni otros requisitos adicionales. */
public class PlanEntrenamiento {
  public String nombre, nivel, profesor, fechaTexto;
  public BigInteger semanas;
  public LocalDate fecha;
  public Nombres objetivos;
  public final List<Rutina> rutinas = new ArrayList<>();

  public static class Nombres extends ArrayList<String> { }
  public static class Elementos extends ArrayList<Elemento> { }
  public static class Rutinas extends ArrayList<Rutina> { }

  public static class Medida {
    public final BigInteger cantidad;
    public final String unidad;
    public final int linea;

    public Medida(String texto, int linea) {
      Matcher m = Pattern.compile("([0-9]+)(kg|lb|%1RM|km/h|h|m|s)").matcher(texto);
      if (!m.matches()) throw new IllegalArgumentException("Medida inválida: " + texto);
      cantidad = new BigInteger(m.group(1));
      unidad = m.group(2);
      this.linea = linea;
    }
  }

  public static class Especificacion {
    public final String tipo;
    public BigInteger series, repeticiones;
    public Medida tiempo, carga, descanso, velocidad;

    public Especificacion(String tipo) { this.tipo = tipo; }
  }

  public static class Elemento {
    public final String nombre;
    public final int linea;
    public Especificacion especificacion;
    public BigInteger vueltas;
    public Medida descanso;
    public Elementos ejercicios;

    public Elemento(String nombre, int linea, Especificacion especificacion) {
      this.nombre = nombre;
      this.linea = linea;
      this.especificacion = especificacion;
    }

    public boolean esSuperserie() { return ejercicios != null; }

    public Elemento copiar(Nombres exclusiones) {
      if (nombre != null && exclusiones.contains(nombre)) return null;
      Elemento copia = new Elemento(nombre, linea, especificacion);
      copia.vueltas = vueltas;
      copia.descanso = descanso;
      if (ejercicios != null) {
        copia.ejercicios = new Elementos();
        for (Elemento ejercicio : ejercicios) {
          Elemento incluido = ejercicio.copiar(exclusiones);
          if (incluido != null) copia.ejercicios.add(incluido);
        }
      }
      return copia;
    }
  }

  public static class Campos {
    public String calentamiento, intensidad, observaciones;
    public Medida duracion;
    public Nombres musculos, equipamiento, variantes;
    public final Map<String, String> extra = new LinkedHashMap<>();

    public void aplicar(Campos otros) {
      if (otros.calentamiento != null) calentamiento = otros.calentamiento;
      if (otros.intensidad != null) intensidad = otros.intensidad;
      if (otros.observaciones != null) observaciones = otros.observaciones;
      if (otros.duracion != null) duracion = otros.duracion;
      if (otros.musculos != null) {
        musculos = new Nombres();
        musculos.addAll(otros.musculos);
      }
      if (otros.equipamiento != null) {
        equipamiento = new Nombres();
        equipamiento.addAll(otros.equipamiento);
      }
      if (otros.variantes != null) {
        variantes = new Nombres();
        variantes.addAll(otros.variantes);
      }
      extra.putAll(otros.extra);
    }
  }

  public static class Reutilizacion {
    public String nombre;
    public BigInteger vueltas = BigInteger.ONE;
    public Nombres exclusiones = new Nombres();
    public int linea;
  }

  public static class Contenido {
    public Elementos ejercicios = new Elementos();
    public Reutilizacion reutilizacion;
  }

  public static class Rutina {
    public String nombre, dia;
    public int linea;
    public Contenido contenido;
    public Campos declarados, efectivos;
    public Elementos ejercicios;
    public BigInteger vueltasCircuito = BigInteger.ONE;
  }
}
