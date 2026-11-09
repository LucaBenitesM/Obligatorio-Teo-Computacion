import java.io.PrintStream;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Requisitos adicionales sobre el AST:
 *   A. validarReferencias : variantes, Igual a (existe / declarada antes / sin ciclos) y EXCEPTO
 *   B. conteo             : total de ejercicios, promedio por día y desglose por día
 *   E. calendario         : semana tipo
 *   F. resumen            : frecuencia por músculo, equipamiento, tiempo semanal, intensidad promedio
 *
 * Solo lee el AST. Las decisiones de diseño están en las constantes y comentarios de abajo.
 */
public final class Analisis {
  private Analisis() {}

  // ===================== DECISIONES DE DISEÑO (documentarlas en el informe) =====================

  /**
   * "Igual a X x N veces": N son vueltas completas al circuito, así que cada ejercicio de la rutina
   * referenciada cuenta N veces (igual que un ejercicio dentro de una superserie cuenta 1 por vuelta).
   * Con N = 1 (el caso del enunciado) da lo mismo. Poner en false si el docente quiere el conteo literal.
   */
  static final boolean MULTIPLICAR_POR_VUELTAS = true;

  /** Intensidad textual -> escala 1..5 (extremos y centro). Estrellas y n/5 ya vienen en esa escala. */
  static double nivelTexto(String t) {
    switch (t) {
      case "BAJA": return 1;
      case "MEDIA": return 3;
      case "ALTA": return 5;
      default: return Double.NaN;
    }
  }

  // ===================== ADAPTADORES (único punto que depende de nombres de campos no vistos) =====================

  /** Texto original de una Medida, por ejemplo "60m". ADAPTAR al nombre real del campo en PlanEntrenamiento.Medida. */
  private static String textoMedida(PlanEntrenamiento.Medida m) { return m == null ? null : m.valor; }

  /** Convierte int / Integer / BigInteger a int (null -> 1). */
  private static int entero(Object o) { return o == null ? 1 : ((Number) o).intValue(); }

  // ===================== Utilidades =====================

  static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

  private static String sinTildes(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
  }

  /** Normaliza un nombre para comparar: sin espacios sobrantes y sin distinguir mayúsculas. */
  static String norm(String s) {
    return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  static int indiceDia(String dia) {
    String d = sinTildes(norm(dia));
    for (int i = 0; i < DIAS.length; i++) if (sinTildes(DIAS[i].toLowerCase(Locale.ROOT)).equals(d)) return i;
    return -1;
  }

  // ===================== Modelo intermedio: ejercicios "efectivos" de cada rutina =====================

  /** Un elemento de primer nivel: ejercicio simple o superserie (con sus ejercicios y vueltas). */
  static final class Item {
    final String nombre; final boolean superserie; final int vueltas; final List<String> hijos; int mult = 1;
    Item(String nombre, boolean superserie, int vueltas, List<String> hijos) {
      this.nombre = nombre; this.superserie = superserie; this.vueltas = vueltas; this.hijos = hijos;
    }
    Item copia() { Item c = new Item(nombre, superserie, vueltas, new ArrayList<>(hijos)); c.mult = mult; return c; }
    /** Simple = 1; superserie = ejercicios x vueltas. Todo multiplicado por las vueltas del Igual a. */
    int total() { return (superserie ? hijos.size() * vueltas : 1) * mult; }
  }

  /** Campos efectivos (declarados o heredados de la rutina referenciada). */
  static final class Ef {
    String intensidad, duracion;
    List<String> musculos = new ArrayList<>(), equipamiento = new ArrayList<>();
  }

  static List<Item> aItems(Iterable<PlanEntrenamiento.Elemento> elementos) {
    List<Item> lista = new ArrayList<>();
    for (PlanEntrenamiento.Elemento e : elementos) {
      if (e == null) continue;
      if (e.ejercicios != null) {
        List<String> hijos = new ArrayList<>();
        for (PlanEntrenamiento.Elemento h : e.ejercicios) hijos.add(h.nombre);
        lista.add(new Item(e.nombre, true, entero(e.vueltas), hijos));
      } else {
        lista.add(new Item(e.nombre, false, 1, new ArrayList<>()));
      }
    }
    return lista;
  }

  static boolean contiene(List<Item> lista, String nombre) {
    String n = norm(nombre);
    for (Item it : lista) {
      if (it.nombre != null && norm(it.nombre).equals(n)) return true;
      for (String h : it.hijos) if (norm(h).equals(n)) return true;
    }
    return false;
  }

  /**
   * Quita de la lista el elemento nombrado. Primero busca un elemento de primer nivel (ejercicio o superserie
   * completa); si no, un ejercicio dentro de una superserie (resta 1 por vuelta, porque cuenta 1 por vuelta).
   */
  static void excluir(List<Item> lista, String nombre) {
    String n = norm(nombre);
    for (Iterator<Item> it = lista.iterator(); it.hasNext(); ) {
      Item x = it.next();
      if (x.nombre != null && norm(x.nombre).equals(n)) { it.remove(); return; }
    }
    for (Item x : lista)
      for (int k = 0; k < x.hijos.size(); k++)
        if (norm(x.hijos.get(k)).equals(n)) { x.hijos.remove(k); return; }
  }

  /** Expande una sola vez todas las rutinas, resolviendo Igual a hacia atrás (las inválidas quedan vacías). */
  static final class Contexto {
    final List<PlanEntrenamiento.Rutina> rutinas = new ArrayList<>();
    final List<List<Item>> items = new ArrayList<>();
    final List<Ef> efectivos = new ArrayList<>();
    final Map<String, Integer> indice = new HashMap<>(); // primera rutina con ese nombre

    Contexto(PlanEntrenamiento plan) {
      for (PlanEntrenamiento.Rutina r : plan.rutinas) if (r != null) rutinas.add(r);
      Map<String, Integer> previas = new HashMap<>();
      for (int i = 0; i < rutinas.size(); i++) {
        PlanEntrenamiento.Rutina r = rutinas.get(i);
        List<Item> lista = new ArrayList<>();
        Ef ef = new Ef();
        PlanEntrenamiento.Contenido c = r.contenido;
        if (c != null && c.reutilizacion != null) {
          PlanEntrenamiento.Reutilizacion ru = c.reutilizacion;
          Integer j = previas.get(norm(ru.nombre));
          if (j != null) {
            for (Item it : items.get(j)) lista.add(it.copia());
            if (ru.exclusiones != null) for (String ex : ru.exclusiones) excluir(lista, ex);
            int v = MULTIPLICAR_POR_VUELTAS ? entero(ru.vueltas) : 1;
            for (Item it : lista) it.mult *= v;
            Ef base = efectivos.get(j);
            ef.intensidad = base.intensidad; ef.duracion = base.duracion;
            ef.musculos = new ArrayList<>(base.musculos); ef.equipamiento = new ArrayList<>(base.equipamiento);
          }
        } else if (c != null && c.ejercicios != null) {
          lista = aItems(c.ejercicios);
        }
        PlanEntrenamiento.Campos d = r.declarados;
        if (d != null) {
          if (d.intensidad != null) ef.intensidad = d.intensidad;
          if (d.duracion != null) ef.duracion = textoMedida(d.duracion);
          if (d.musculos != null) { ef.musculos = new ArrayList<>(); for (String m : d.musculos) ef.musculos.add(m); }
          if (d.equipamiento != null) { ef.equipamiento = new ArrayList<>(); for (String m : d.equipamiento) ef.equipamiento.add(m); }
        }
        items.add(lista);
        efectivos.add(ef);
        previas.putIfAbsent(norm(r.nombre), i);
      }
      indice.putAll(previas);
    }
  }

  static int suma(List<Item> lista) { int s = 0; for (Item it : lista) s += it.total(); return s; }

  // ===================== A. Variantes y referencias =====================

  public static void validarReferencias(PlanEntrenamiento p, Diagnosticos d) {
    Contexto cx = new Contexto(p);
    Set<String> conocidos = new HashSet<>();
    for (PlanEntrenamiento.Rutina r : cx.rutinas) conocidos.add(norm(r.nombre));
    for (List<Item> lista : cx.items)
      for (Item it : lista) {
        if (it.nombre != null) conocidos.add(norm(it.nombre));
        for (String h : it.hijos) conocidos.add(norm(h));
      }

    for (int i = 0; i < cx.rutinas.size(); i++) {
      PlanEntrenamiento.Rutina r = cx.rutinas.get(i);
      PlanEntrenamiento.Reutilizacion ru = r.contenido == null ? null : r.contenido.reutilizacion;
      if (ru != null) {
        String ref = norm(ru.nombre);
        Integer j = cx.indice.get(ref);
        if (ref.equals(norm(r.nombre))) {
          d.agregar("semántico", ru.linea, "La rutina '" + r.nombre + "' se referencia a sí misma con Igual a.");
        } else if (j == null) {
          d.agregar("semántico", ru.linea, "La rutina '" + ru.nombre + "' referenciada por Igual a no existe.");
        } else if (j > i) {
          String ciclo = llegaA(cx, j, i) ? " Además, las referencias forman un ciclo." : "";
          d.agregar("semántico", ru.linea, "La rutina '" + ru.nombre + "' se declara más abajo en el archivo; "
              + "Igual a solo puede referenciar rutinas ya declaradas." + ciclo);
        } else if (ru.exclusiones != null) {
          for (String ex : ru.exclusiones)
            if (!contiene(cx.items.get(j), ex))
              d.agregar("semántico", ru.linea, "EXCEPTO: '" + ex + "' no existe en la rutina '" + ru.nombre + "'.");
        }
      }
      if (r.declarados != null && r.declarados.variantes != null)
        for (String v : r.declarados.variantes)
          if (!conocidos.contains(norm(v)))
            d.agregar("semántico", r.linea, "La variante '" + v + "' de la rutina '" + r.nombre
                + "' no existe como ejercicio, superserie ni rutina del plan.");
    }
  }

  /** ¿Siguiendo Igual a desde 'desde' se llega a 'objetivo'? (para avisar de ciclos). */
  private static boolean llegaA(Contexto cx, int desde, int objetivo) {
    int actual = desde;
    for (int paso = 0; paso <= cx.rutinas.size(); paso++) {
      PlanEntrenamiento.Rutina r = cx.rutinas.get(actual);
      if (r.contenido == null || r.contenido.reutilizacion == null) return false;
      Integer sig = cx.indice.get(norm(r.contenido.reutilizacion.nombre));
      if (sig == null) return false;
      if (sig == objetivo) return true;
      actual = sig;
    }
    return false;
  }

  // ===================== B. Conteo de ejercicios =====================

  public static String conteo(PlanEntrenamiento p) {
    Contexto cx = new Contexto(p);
    int total = 0;
    int[] porDia = new int[7];
    boolean[] hay = new boolean[7];
    for (int i = 0; i < cx.rutinas.size(); i++) {
      int n = suma(cx.items.get(i));
      total += n;
      int d = indiceDia(cx.rutinas.get(i).dia);
      if (d >= 0) { porDia[d] += n; hay[d] = true; }
    }
    int dias = 0;
    for (boolean b : hay) if (b) dias++;
    StringBuilder sb = new StringBuilder("CONTEO DE EJERCICIOS\n");
    sb.append("Total de ejercicios del plan: ").append(total).append('\n');
    sb.append("Promedio por día con rutina: ")
      .append(dias == 0 ? "-" : String.format(Locale.ROOT, "%.2f", (double) total / dias)).append('\n');
    for (int d = 0; d < 7; d++)
      if (hay[d]) sb.append(DIAS[d]).append(" - ").append(porDia[d]).append(porDia[d] == 1 ? " ejercicio\n" : " ejercicios\n");
    return sb.toString();
  }

  // ===================== E. Calendario semanal =====================

  public static String calendario(PlanEntrenamiento p) {
    Contexto cx = new Contexto(p);
    StringBuilder sb = new StringBuilder("SEMANA TIPO\n");
    for (int d = 0; d < 7; d++) {
      List<String> lineas = new ArrayList<>();
      for (int i = 0; i < cx.rutinas.size(); i++) {
        PlanEntrenamiento.Rutina r = cx.rutinas.get(i);
        if (indiceDia(r.dia) != d) continue;
        Ef ef = cx.efectivos.get(i);
        StringBuilder l = new StringBuilder(r.nombre);
        if (ef.duracion != null) l.append("  [").append(ef.duracion).append(']');
        if (ef.intensidad != null) l.append("  intensidad ").append(ef.intensidad);
        lineas.add(l.toString());
      }
      sb.append(String.format("%-10s", DIAS[d]));
      if (lineas.isEmpty()) sb.append("descanso\n");
      else {
        sb.append(lineas.get(0)).append('\n');
        for (int k = 1; k < lineas.size(); k++) sb.append(String.format("%-10s", "")).append(lineas.get(k)).append('\n');
        if (lineas.size() > 1) sb.append(String.format("%-10s", "")).append("(aviso: hay ").append(lineas.size()).append(" rutinas el mismo día)\n");
      }
    }
    return sb.toString();
  }

  // ===================== F. Resumen =====================

  private static final Pattern TIEMPO = Pattern.compile("(\\d+)(h|m|s)");

  static double minutos(String t) {
    if (t == null) return Double.NaN;
    Matcher m = TIEMPO.matcher(t.trim());
    if (!m.matches()) return Double.NaN;
    double v = Double.parseDouble(m.group(1));
    switch (m.group(2)) { case "h": return v * 60; case "m": return v; default: return v / 60; }
  }

  static double nivel(String i) {
    if (i == null) return Double.NaN;
    String t = i.trim().toUpperCase(Locale.ROOT);
    if (t.matches("\\*{1,5}")) return t.length();
    if (t.matches("[1-5]/5")) return t.charAt(0) - '0';
    return nivelTexto(t);
  }

  static String formatoMinutos(double min) {
    long m = Math.round(min);
    return m < 60 ? m + "m" : (m / 60) + "h " + String.format("%02d", m % 60) + "m";
  }

  public static String resumen(PlanEntrenamiento p) {
    Contexto cx = new Contexto(p);
    Map<String, String> nombreMusculo = new LinkedHashMap<>();
    Map<String, List<String>> diasMusculo = new HashMap<>();
    Map<String, String> equipo = new LinkedHashMap<>();
    double totalMin = 0, sumaNivel = 0, sumaPond = 0, sumaPeso = 0;
    int conNivel = 0;

    for (int i = 0; i < cx.rutinas.size(); i++) {
      PlanEntrenamiento.Rutina r = cx.rutinas.get(i);
      Ef ef = cx.efectivos.get(i);
      int d = indiceDia(r.dia);
      String dia = d >= 0 ? DIAS[d] : "?";
      Set<String> vistos = new HashSet<>();
      for (String m : ef.musculos) {
        String k = norm(m);
        if (!vistos.add(k)) continue;
        nombreMusculo.putIfAbsent(k, m.trim());
        diasMusculo.computeIfAbsent(k, x -> new ArrayList<>()).add(dia);
      }
      for (String e : ef.equipamiento) equipo.putIfAbsent(norm(e), e.trim());
      double min = minutos(ef.duracion);
      if (!Double.isNaN(min)) totalMin += min;
      double nv = nivel(ef.intensidad);
      if (!Double.isNaN(nv)) {
        sumaNivel += nv; conNivel++;
        if (!Double.isNaN(min)) { sumaPond += nv * min; sumaPeso += min; }
      }
    }

    StringBuilder sb = new StringBuilder("RESUMEN DEL PLAN\n");
    sb.append("Frecuencia de estímulo por grupo muscular (sesiones por semana):\n");
    List<String> claves = new ArrayList<>(nombreMusculo.keySet());
    claves.sort((a, b) -> diasMusculo.get(b).size() != diasMusculo.get(a).size()
        ? diasMusculo.get(b).size() - diasMusculo.get(a).size() : a.compareTo(b));
    for (String k : claves)
      sb.append("  ").append(nombreMusculo.get(k)).append(": ").append(diasMusculo.get(k).size())
        .append(" (").append(String.join(", ", diasMusculo.get(k))).append(")\n");
    sb.append("Equipamiento total: ").append(equipo.isEmpty() ? "-" : String.join(", ", equipo.values())).append('\n');
    sb.append("Tiempo semanal total: ").append(formatoMinutos(totalMin));
    Object semanas = p.semanas;
    if (semanas instanceof Number)
      sb.append("  (x ").append(((Number) semanas).intValue()).append(" semanas = ")
        .append(formatoMinutos(totalMin * ((Number) semanas).intValue())).append(')');
    sb.append('\n');
    if (conNivel == 0) sb.append("Intensidad promedio: -\n");
    else {
      sb.append(String.format(Locale.ROOT, "Intensidad promedio: %.2f/5", sumaNivel / conNivel));
      if (sumaPeso > 0) sb.append(String.format(Locale.ROOT, "  (ponderada por duración: %.2f/5)", sumaPond / sumaPeso));
      sb.append('\n');
    }
    return sb.toString();
  }

  public static void imprimirReportes(PlanEntrenamiento p, PrintStream out) {
    out.println(conteo(p));
    out.println(calendario(p));
    out.println(resumen(p));
  }
}
