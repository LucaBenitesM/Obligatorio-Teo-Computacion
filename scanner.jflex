import java_cup.runtime.*;

%%
%class Lexer
%cup
%line

DIGITO =[0-9]
LETRA=[a-zA-Z]

%%

"PLAN"           { return new Symbol(sym.PLAN, yyline+1, 0); }
"Semanas:"         { return new Symbol(sym.SEMANAS, yyline+1, 0);}
"Objetivos:"     { return new Symbol(sym.OBJETIVOS, yyline+1,0);}
"Profesor:"          { return new Symbol(sym.PROFESOR, yyline+1,0);}
"Fecha:"            { return new Symbol(sym.FECHA, yyline+1,0);}
"RUTINA"        { return new Symbol(sym.RUTINA, yyline+1,0);}
"Dia:"        { return new Symbol(sym.DIA, yyline+1,0);}
"Calentamiento:"        { return new Symbol(sym.CALENTAMIENTO, yyline+1,0);}
"EJERCICIOS:"        { return new Symbol(sym.EJERCICIOS, yyline+1,0);}
"Duracion:"        { return new Symbol(sym.DURACION, yyline+1,0);}
"Musculos:"        { return new Symbol(sym.MUSCULOS, yyline+1,0);}
"Variantes:"        { return new Symbol(sym.VARIANTES, yyline+1,0);}
"Equipamiento:"      {return new Symbol(sym.EQUIPAMIENTO, yyline+1,0);}
"Obs:"      {return new Symbol(sym.OBS, yyline+1,0);}
"Extra:"      {return new Symbol(sym.EXTRA, yyline+1,0);}


/* --- Superserie (sin distinguir mayúsculas) --- */
[fF][iI][nN][ \t]+[sS][uU][pP][eE][rR][sS][eE][rR][iI][eE]   { return new Symbol(sym.FIN_SUPERSERIE, yyline+1, 0); }
[sS][uU][pP][eE][rR][sS][eE][rR][iI][eE]                     { return new Symbol(sym.SUPERSERIE, yyline+1, 0); }
"vueltas"        { return new Symbol(sym.VUELTAS, yyline+1, 0); }

/* --- Igual a --- */
"Igual"[ \t]+"a" { return new Symbol(sym.IGUAL_A, yyline+1, 0); }
"EXCEPTO"        { return new Symbol(sym.EXCEPTO, yyline+1, 0); }
"vez"            { return new Symbol(sym.VEZ, yyline+1, 0); }
"veces"          { return new Symbol(sym.VECES, yyline+1, 0); }
{DIGITO}+"%"     { return new Symbol(sym.PORCENTAJE, yyline+1, 0, Integer.valueOf(yytext().substring(0, yylength()-1))); }

/* --- Ejercicios --- */
"descanso"       { return new Symbol(sym.DESCANSO, yyline+1, 0); }
"MAX"            { return new Symbol(sym.MAX, yyline+1, 0); }
"reps"           { return new Symbol(sym.REPS, yyline+1, 0); }
{DIGITO}+"x"     { return new Symbol(sym.SERIES_X, yyline+1, 0, Integer.valueOf(yytext().substring(0, yylength()-1))); }
"x"              { return new Symbol(sym.X, yyline+1, 0); }

/* --- Símbolos sueltos --- */
":"              { return new Symbol(sym.DOS_PUNTOS, yyline+1, 0); }
"@"              { return new Symbol(sym.ARROBA, yyline+1, 0); }
"="              { return new Symbol(sym.IGUAL, yyline+1, 0); }


"Lunes"|"Martes"|"Miercoles"|"Jueves"|"Viernes"|"Sabado"|"Domingo" {return new Symbol(sym.VAL_DIA,lineyy+1,0);}

{DIGITO}+ ("kg"|"lb"|"%1RM") {return new Symbol(sym.CARGA,lineyy+1,0);}
{DIGITO}+ ("m"|"s"|"h") {return new Symbol(sym.TIEMPO,yyline+1,0);}
{DIGITO}+ "km/h" {return new Symbol(sym.VELOCIDAD,yyline+1,0);}


"ALTA"|"MEDIA"|"BAJA"|[*]{1,5}|[1-5]"/5"  { return new Symbol(sym.VAL_INTENSIDAD, yyline+1,0, yytext());}

"BASICO"|"INTERMEDIO"|"AVANZADO"          { return new Symbol(sym.NIVEL_VAL, yyline+1, 0, yytext()); }

"["    { return new Symbol(sym.LCORCH, yyline+1, 0); }
"]"    { return new Symbol(sym.RCORCH, yyline+1, 0); }
","    { return new Symbol(sym.COMA, yyline+1, 0); }

{DIGITO}{1,2}"/"{DIGITO}{1,2}"/"{DIGITO}{4}   { return new Symbol(sym.FECHA_VAL, yyline+1, 0, yytext()); }

{LETRA}+        { return new Symbol(sym.ID, yyline+1, 0, yytext()); }
{DIGITO}+            { return new Symbol(sym.NUM, yyline+1, 0, Integer.valueOf(yytext()));}
\"[^\"\n]*\"     { return new Symbol(sym.STRING, yyline+1, 0, yytext()); }
[ \t\r\n]+       { /* ignorar espacios */ }
.                { System.out.println("Error léxico en línea " + (yyline+1) + ": " + yytext()); }