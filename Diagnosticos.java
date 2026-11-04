import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Errores compartidos por el lexer, el parser y el validador. */
public class Diagnosticos {
  public static class Error {
    public final String tipo;
    public final int linea;
    public final String mensaje;

    Error(String tipo, int linea, String mensaje) {
      this.tipo = tipo;
      this.linea = Math.max(1, linea);
      this.mensaje = mensaje;
    }

    @Override public String toString() {
      return "Error " + tipo + " en línea " + linea + ": " + mensaje;
    }
  }

  private final List<Error> errores = new ArrayList<>();

  public void agregar(String tipo, int linea, String mensaje) {
    errores.add(new Error(tipo, linea, mensaje));
  }

  public boolean hayErrores() { return !errores.isEmpty(); }
  public List<Error> errores() { return Collections.unmodifiableList(errores); }
}
