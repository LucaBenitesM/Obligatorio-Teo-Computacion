import java_cup.runtime.*;

%%
%public
%class Lexer
%cup
%line
%column
%unicode
%state LISTA, TEXTO_LIBRE, EXTRA_CLAVE, EXTRA_IGUAL

%{
  private Diagnosticos diagnosticos = new Diagnosticos();
  private boolean necesitaEol = false;

  public void configurar(Diagnosticos errores) { diagnosticos = errores; }
  public int lineaActual() { return yyline + 1; }

  private Symbol token(int tipo) { return token(tipo, null); }

  private Symbol token(int tipo, Object valor) {
    if (tipo == sym.EOL) necesitaEol = false;
    else if (tipo != sym.EOF) necesitaEol = true;
    return new Symbol(tipo, yyline + 1, yycolumn + 1, valor);
  }

  private String texto(String valor) {
    valor = valor.trim();
    if (valor.startsWith("\"")) {
      if (valor.length() >= 2 && valor.endsWith("\"")) return valor.substring(1, valor.length() - 1);
      diagnosticos.agregar("léxico", yyline + 1, "Texto entre comillas sin cierre.");
    }
    return valor;
  }

  private Symbol invalido(String mensaje) {
    diagnosticos.agregar("léxico", yyline + 1, mensaje);
    return token(sym.INVALIDO);
  }
%}

%eofval{
  // Una última línea sin salto también puede terminar una declaración.
  if (necesitaEol) return token(sym.EOL);
  return token(sym.EOF);
%eofval}

DIGITO = [0-9]
ESPACIO = [ \t\f]
SALTO = \r\n|\r|\n
SUPER = [sS][uU][pP][eE][rR][sS][eE][rR][iI][eE]
FIN = [fF][iI][nN]
NOMBRE_LIBRE = [^ \t\f\r\n:\"][^\r\n:\"]*

%%

<YYINITIAL> {
  "PLAN"            { return token(sym.PLAN); }
  "Nivel:"          { return token(sym.NIVEL); }
  "Semanas:"        { return token(sym.SEMANAS); }
  "Objetivos:"      { return token(sym.OBJETIVOS); }
  "Profesor:"       { return token(sym.PROFESOR); }
  "Fecha:"          { return token(sym.FECHA); }
  "RUTINA"          { return token(sym.RUTINA); }
  "Día:"|"Dia:"     { return token(sym.DIA); }
  "Calentamiento:"  { yybegin(TEXTO_LIBRE); return token(sym.CALENTAMIENTO); }
  "EJERCICIOS:"     { return token(sym.EJERCICIOS); }
  "Duración:"|"Duracion:" { return token(sym.DURACION); }
  "Músculos:"|"Musculos:" { return token(sym.MUSCULOS); }
  "Variantes:"      { return token(sym.VARIANTES); }
  "Equipamiento:"   { return token(sym.EQUIPAMIENTO); }
  "Obs:"|"Observaciones:" { yybegin(TEXTO_LIBRE); return token(sym.OBS); }
  "Extra:"          { yybegin(EXTRA_CLAVE); return token(sym.EXTRA); }
  "Intensidad:"     { return token(sym.INTENSIDAD); }

  {FIN}[ \t]+{SUPER} { return token(sym.FIN_SUPERSERIE); }
  {SUPER}           { return token(sym.SUPERSERIE); }
  "vueltas"         { return token(sym.VUELTAS); }
  "Igual"[ \t]+"a" { return token(sym.IGUAL_A); }
  "EXCEPTO"         { return token(sym.EXCEPTO); }
  "vez"            { return token(sym.VEZ); }
  "veces"          { return token(sym.VECES); }
  {DIGITO}+"%"     { return token(sym.PORCENTAJE, yytext().substring(0, yylength()-1)); }
  "descanso"       { return token(sym.DESCANSO); }
  "MAX"            { return token(sym.MAX); }
  "reps"           { return token(sym.REPS); }
  {DIGITO}+[ \t]*"x" { return token(sym.SERIES_X, yytext().substring(0, yylength()-1).trim()); }
  "x"              { return token(sym.X); }
  ":"              { return token(sym.DOS_PUNTOS); }
  "@"              { return token(sym.ARROBA); }
  ","              { return token(sym.COMA); }
  "["              { yybegin(LISTA); return token(sym.LCORCH); }

  "Lunes"|"Martes"|"Miércoles"|"Miercoles"|"Jueves"|"Viernes"|"Sábado"|"Sabado"|"Domingo"
                   { return token(sym.VAL_DIA, yytext()); }
  {DIGITO}+("kg"|"lb"|"%1RM") { return token(sym.CARGA, yytext()); }
  {DIGITO}+("h"|"m"|"s") { return token(sym.TIEMPO, yytext()); }
  {DIGITO}+"km/h"  { return token(sym.VELOCIDAD, yytext()); }
  "ALTA"|"MEDIA"|"BAJA"|[*]{1,5}|[1-5]"/5" { return token(sym.VAL_INTENSIDAD, yytext()); }
  "BASICO"|"INTERMEDIO"|"AVANZADO" { return token(sym.NIVEL_VAL, yytext()); }
  {DIGITO}{1,2}"/"{DIGITO}{1,2}"/"{DIGITO}{4} { return token(sym.FECHA_VAL, yytext()); }
  {DIGITO}+        { return token(sym.NUM, yytext()); }
  \"[^\"\r\n]*\" { return token(sym.STRING, texto(yytext())); }

  // El contexto derecho permite texto libre hasta ':' o SUPERSERIE.
  // Las reglas de campos reservados anteriores tienen prioridad en los empates.
  {NOMBRE_LIBRE} / ":" { return token(sym.NOMBRE, yytext().trim()); }
  {NOMBRE_LIBRE} / [ \t]+{SUPER} { return token(sym.NOMBRE, yytext().trim()); }
  \"[^\"\r\n]*  { return invalido("Cadena entre comillas sin cierre."); }
}

<LISTA> {
  "]"              { yybegin(YYINITIAL); return token(sym.RCORCH); }
  ","              { return token(sym.COMA); }
  [^ \t\f\r\n,\[\]\"][^\r\n,\[\]\"]* { return token(sym.NOMBRE, yytext().trim()); }
}

<TEXTO_LIBRE> {
  [^ \t\f\r\n][^\r\n]* { yybegin(YYINITIAL); return token(sym.TEXTO, texto(yytext())); }
}

<EXTRA_CLAVE> {
  [^ \t\f\r\n=][^\r\n=]* { yybegin(EXTRA_IGUAL); return token(sym.CLAVE, yytext().trim()); }
}

<EXTRA_IGUAL> {
  "=" { yybegin(TEXTO_LIBRE); return token(sym.IGUAL); }
}

<YYINITIAL,LISTA,TEXTO_LIBRE,EXTRA_CLAVE,EXTRA_IGUAL> {
  {ESPACIO}+ { /* La indentación no modifica la estructura. */ }
  {SALTO}    { yybegin(YYINITIAL); return token(sym.EOL); }
  .          { return invalido("Carácter no válido: '" + yytext() + "'."); }
}
