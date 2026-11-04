import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
  public static void main(String[] args) {
    String archivo = args.length > 0 ? args[0] : "plan.txt";
    try (Reader entrada = Files.newBufferedReader(Paths.get(archivo), StandardCharsets.UTF_8)) {
      parser p = new parser(new Lexer(entrada));
      p.parse();
    } catch (Exception e) {
      System.err.println("No se pudo analizar '" + archivo + "': " + e.getMessage());
      System.exit(1);
    }
  }
}


