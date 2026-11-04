import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Validador {
  private final Diagnosticos diagnosticos;
  private static final Pattern RESERVADA = Pattern.compile(
      "(?iu)(?<![\\p{L}\\p{N}_])(FIN|SUPERSERIE)(?![\\p{L}\\p{N}_])");

  public Validador(Diagnosticos diagnosticos) { this.diagnosticos = diagnosticos; }

  public void error(int linea, String mensaje) {
    diagnosticos.agregar("semántico", linea, mensaje);
  }

  public void nombre(String nombre, int linea) {
    if (nombre != null && RESERVADA.matcher(nombre).find()) {
      error(linea, "El nombre no puede contener las palabras FIN ni SUPERSERIE.");
    }
  }

  public BigInteger positivo(String texto, int linea, String campo) {
    BigInteger numero = new BigInteger(texto);
    if (numero.signum() <= 0) error(linea, campo + " debe ser un entero positivo.");
    return numero;
  }

  public BigInteger semanas(String texto, int linea) {
    BigInteger numero = new BigInteger(texto);
    if (numero.compareTo(BigInteger.ONE) < 0 || numero.compareTo(BigInteger.valueOf(52)) > 0) {
      error(linea, "Semanas debe estar entre 1 y 52.");
    }
    return numero;
  }

  public LocalDate fecha(String texto, int linea) {
    String[] partes = texto.split("/");
    try {
      int anio = Integer.parseInt(partes[2]);
      if (anio == 0) throw new DateTimeException("Año cero");
      return LocalDate.of(anio, Integer.parseInt(partes[1]), Integer.parseInt(partes[0]));
    } catch (DateTimeException e) {
      error(linea, "Fecha inexistente: " + texto + ". Use día/mes/año con un año de cuatro cifras.");
      return null;
    }
  }

  public PlanEntrenamiento.Medida carga(String texto, int linea) {
    PlanEntrenamiento.Medida medida = new PlanEntrenamiento.Medida(texto, linea);
    if (medida.unidad.equals("%1RM") && medida.cantidad.compareTo(BigInteger.valueOf(100)) > 0) {
      error(linea, "La carga en %1RM no puede superar 100.");
    }
    return medida;
  }

  public BigInteger vueltas(String texto, int linea) {
    BigInteger numero = new BigInteger(texto);
    if (numero.compareTo(BigInteger.valueOf(2)) < 0) error(linea, "Una superserie requiere al menos 2 vueltas.");
    return numero;
  }

  public BigInteger repeticion(String texto, boolean singular, int linea) {
    BigInteger numero = positivo(texto, linea, "La cantidad de vueltas del circuito");
    if (singular != numero.equals(BigInteger.ONE)) {
      error(linea, "Use 'x 1 vez' o 'x N veces' para N mayor que 1.");
    }
    return numero;
  }

  public BigInteger porcentaje(String texto, int linea) {
    BigInteger numero = new BigInteger(texto);
    BigInteger cien = BigInteger.valueOf(100);
    if (numero.signum() <= 0 || !numero.mod(cien).equals(BigInteger.ZERO)) {
      error(linea, "El porcentaje de reutilización debe ser un múltiplo positivo de 100.");
    }
    return numero.divide(cien);
  }

  /** Aplica la reutilización básica. No implementa las comprobaciones del adicional A. */
  public void completar(PlanEntrenamiento plan) {
    Map<String, PlanEntrenamiento.Rutina> anteriores = new LinkedHashMap<>();
    for (PlanEntrenamiento.Rutina rutina : plan.rutinas) {
      rutina.efectivos = new PlanEntrenamiento.Campos();
      rutina.ejercicios = new PlanEntrenamiento.Elementos();
      PlanEntrenamiento.Reutilizacion referencia = rutina.contenido.reutilizacion;
      if (referencia == null) {
        rutina.ejercicios.addAll(rutina.contenido.ejercicios);
        if (rutina.declarados.intensidad == null) error(rutina.linea, "Falta Intensidad en la rutina '" + rutina.nombre + "'.");
        if (rutina.declarados.duracion == null) error(rutina.linea, "Falta Duración en la rutina '" + rutina.nombre + "'.");
        if (rutina.declarados.musculos == null) error(rutina.linea, "Falta Músculos en la rutina '" + rutina.nombre + "'.");
      } else {
        rutina.vueltasCircuito = referencia.vueltas;
        PlanEntrenamiento.Rutina original = anteriores.get(referencia.nombre);
        if (original != null) {
          rutina.efectivos.aplicar(original.efectivos);
          rutina.vueltasCircuito = original.vueltasCircuito.multiply(referencia.vueltas);
          for (PlanEntrenamiento.Elemento elemento : original.ejercicios) {
            PlanEntrenamiento.Elemento copia = elemento.copiar(referencia.exclusiones);
            if (copia != null) rutina.ejercicios.add(copia);
          }
        }
      }
      rutina.efectivos.aplicar(rutina.declarados);
      anteriores.put(rutina.nombre, rutina);
    }
  }
}
