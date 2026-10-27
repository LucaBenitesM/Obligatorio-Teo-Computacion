import java_cup.runtime.*;

%%
%class Lexer
%cup
%line

%%

"PLAN"           { return new Symbol(sym.PLAN, yyline+1, 0); }
\"[^\"\n]*\"     { return new Symbol(sym.STRING, yyline+1, 0, yytext()); }
[ \t\r\n]+       { /* ignorar espacios */ }
.                { System.out.println("Error léxico en línea " + (yyline+1) + ": " + yytext()); }