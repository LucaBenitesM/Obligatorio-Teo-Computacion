import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Requisito adicional I: formatos alternativos de fecha.
 *
 * Formatos aceptados (todos validados como fecha REAL):
 *   20/08/2026   20-8-2026   20.08.2026
 *   22-oct-2026  22/octubre/2026
 *   22 de enero de 2026
 *
 * Uso desde Validador.fecha(...):
 *   try { return Fechas.parsear(texto); }
 *   catch (IllegalArgumentException e) { diagnosticos.agregar("semántico", linea, e.getMessage()); return null; }
 */
public final class Fechas {
  private Fechas() {}

  private static final Pattern NUMERICA = Pattern.compile("^(\\d{1,2})([/\\-.])(\\d{1,2})\\2(\\d{4})$");
  private static final Pattern CON_MES = Pattern.compile("^(\\d{1,2})([/\\-])([^/\\-\\s]+)\\2(\\d{4})$");
  private static final Pattern LARGA = Pattern.compile("^(\\d{1,2})\\s+de\\s+(\\S+)\\s+de\\s+(\\d{4})$", Pattern.CASE_INSENSITIVE);

  private static final Map<String, Integer> MESES = Map.ofEntries(
      Map.entry("enero", 1), Map.entry("ene", 1),
      Map.entry("febrero", 2), Map.entry("feb", 2),
      Map.entry("marzo", 3), Map.entry("mar", 3),
      Map.entry("abril", 4), Map.entry("abr", 4),
      Map.entry("mayo", 5), Map.entry("may", 5),
      Map.entry("junio", 6), Map.entry("jun", 6),
      Map.entry("julio", 7), Map.entry("jul", 7),
      Map.entry("agosto", 8), Map.entry("ago", 8),
      Map.entry("septiembre", 9), Map.entry("setiembre", 9), Map.entry("sep", 9), Map.entry("set", 9),
      Map.entry("octubre", 10), Map.entry("oct", 10),
      Map.entry("noviembre", 11), Map.entry("nov", 11),
      Map.entry("diciembre", 12), Map.entry("dic", 12));

  private static final String[] NOMBRES_MES = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
      "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

  /** Devuelve la fecha o lanza IllegalArgumentException con un mensaje listo para el diagnóstico. */
  public static LocalDate parsear(String texto) {
    String t = texto.trim().replaceAll("\\s+", " ");
    Matcher m;
    int dia, mes, anio;
    if ((m = NUMERICA.matcher(t)).matches()) {
      dia = Integer.parseInt(m.group(1));
      mes = Integer.parseInt(m.group(3));
      anio = Integer.parseInt(m.group(4));
    } else if ((m = CON_MES.matcher(t)).matches()) {
      dia = Integer.parseInt(m.group(1));
      mes = mesPorNombre(m.group(3));
      anio = Integer.parseInt(m.group(4));
    } else if ((m = LARGA.matcher(t)).matches()) {
      dia = Integer.parseInt(m.group(1));
      mes = mesPorNombre(m.group(2));
      anio = Integer.parseInt(m.group(3));
    } else {
      throw new IllegalArgumentException("Formato de fecha no reconocido: '" + t + "'.");
    }
    if (mes < 1 || mes > 12)
      throw new IllegalArgumentException("El mes " + mes + " no existe (debe estar entre 1 y 12).");
    if (anio < 1)
      throw new IllegalArgumentException("El año " + anio + " no es válido.");
    int maximo = YearMonth.of(anio, mes).lengthOfMonth();
    if (dia < 1 || dia > maximo)
      throw new IllegalArgumentException("El día " + dia + " no existe en " + NOMBRES_MES[mes - 1]
          + " de " + anio + " (ese mes tiene " + maximo + " días).");
    return LocalDate.of(anio, mes, dia);
  }

  /** Forma normalizada dd/MM/yyyy. */
  public static String normalizar(LocalDate f) {
    return String.format(Locale.ROOT, "%02d/%02d/%04d", f.getDayOfMonth(), f.getMonthValue(), f.getYear());
  }

  private static int mesPorNombre(String nombre) {
    String limpio = Normalizer.normalize(nombre, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    Integer mes = MESES.get(limpio);
    if (mes == null) throw new IllegalArgumentException("Mes desconocido: '" + nombre + "'.");
    return mes;
  }
}
