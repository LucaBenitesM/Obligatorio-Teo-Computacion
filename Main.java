import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
  public static class Resultado {
    public final PlanEntrenamiento plan;
    public final Diagnosticos diagnosticos;

    Resultado(PlanEntrenamiento plan, Diagnosticos diagnosticos) {
      this.plan = plan;
      this.diagnosticos = diagnosticos;
    }

    public boolean esValido() { return plan != null && !diagnosticos.hayErrores(); }
  }

  public static Resultado analizar(Reader entrada) throws java.io.IOException {
    Diagnosticos diagnosticos = new Diagnosticos();
    Lexer lexer = new Lexer(entrada);
    lexer.configurar(diagnosticos);
    parser analizador = new parser(lexer, new java_cup.runtime.DefaultSymbolFactory());
    analizador.configurar(diagnosticos);
    try {
      analizador.parse();
    } catch (java.io.IOException e) {
      throw e;
    } catch (Exception e) {
      if (!diagnosticos.hayErrores()) {
        diagnosticos.agregar("sintáctico", lexer.lineaActual(), e.getMessage());
      }
    }
    return new Resultado(analizador.plan, diagnosticos);
  }

  public static void main(String[] args) {
    if (args.length > 1) {
      System.err.println("Uso: java Main [archivo-del-plan]");
      System.exit(2);
    }
    String archivo = args.length > 0 ? args[0] : "plan.txt";
    try (Reader entrada = Files.newBufferedReader(Paths.get(archivo), StandardCharsets.UTF_8)) {
      Resultado resultado = analizar(entrada);
      for (Diagnosticos.Error error : resultado.diagnosticos.errores()) System.err.println(error);
      if (!resultado.esValido()) {
        System.err.println("El plan contiene errores.");
        System.exit(1);
      }
      System.out.println("Plan de entrenamiento: estructura reconocida.");
      Analisis.imprimirReportes(resultado.plan, System.out);   // <-- línea nueva
    } catch (Exception e) {
      System.err.println("No se pudo analizar '" + archivo + "': " + e.getMessage());
      System.exit(1);
    }
  }
}


