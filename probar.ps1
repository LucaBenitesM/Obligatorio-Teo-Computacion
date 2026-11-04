param(
    [string]$Archivo = "plan.txt"
)

$ErrorActionPreference = "Stop"
Push-Location $PSScriptRoot
try {
    Get-Command java, javac -ErrorAction Stop | Out-Null
    $entrada = (Resolve-Path -LiteralPath $Archivo).Path
    $herramientas = Join-Path $PSScriptRoot ".tools"
    $generados = Join-Path $PSScriptRoot "build/generated"
    $clases = Join-Path $PSScriptRoot "build/classes"
    New-Item -ItemType Directory -Force -Path $herramientas, $generados, $clases | Out-Null

    $jflex = Join-Path $herramientas "jflex-1.9.1.jar"
    $cup = Join-Path $PSScriptRoot "java-cup-11b (1).jar"
    $runtime = Join-Path $PSScriptRoot "java-cup-11b-runtime (1).jar"
    if (-not (Test-Path -LiteralPath $jflex)) {
        Write-Host "Descargando JFlex 1.9.1 (solo la primera vez)..."
        Invoke-WebRequest -UseBasicParsing -Uri "https://repo.maven.apache.org/maven2/de/jflex/jflex/1.9.1/jflex-1.9.1.jar" -OutFile $jflex
    }

    Write-Host "Generando parser con CUP..."
    & java -jar $cup -destdir $generados parser.cup
    if ($LASTEXITCODE -ne 0) { throw "CUP no pudo generar el parser." }

    Write-Host "Generando lexer con JFlex..."
    & java -cp "$jflex;$runtime" jflex.Main --encoding UTF-8 --nobak -d $generados scanner.jflex
    if ($LASTEXITCODE -ne 0) { throw "JFlex no pudo generar el lexer." }

    Write-Host "Compilando..."
    & javac -encoding UTF-8 -cp $runtime -d $clases Main.java (Join-Path $generados "Lexer.java") (Join-Path $generados "parser.java") (Join-Path $generados "sym.java")
    if ($LASTEXITCODE -ne 0) { throw "No se pudo compilar el proyecto." }

    Write-Host "Analizando $entrada"
    & java -cp "$clases;$runtime" Main $entrada
    if ($LASTEXITCODE -ne 0) { throw "El analisis termino con un error." }
} finally {
    Pop-Location
}
