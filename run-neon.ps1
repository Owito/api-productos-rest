# Arranca la aplicacion contra la base de datos de Neon leyendo el archivo .env.
#
#   1. Copiar .env.example como .env y completar DB_URL, DB_USERNAME y DB_PASSWORD
#      con la cadena de conexion de Neon.
#   2. Ejecutar:  .\run-neon.ps1
#
# El archivo .env esta ignorado por git: las credenciales no se versionan.

$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env'

if (-not (Test-Path $envFile)) {
    Write-Host "No existe .env. Copia .env.example como .env y completa las credenciales de Neon." -ForegroundColor Red
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $linea = $_.Trim()
    if ($linea -and -not $linea.StartsWith('#') -and $linea.Contains('=')) {
        $i = $linea.IndexOf('=')
        $clave  = $linea.Substring(0, $i).Trim()
        $valor  = $linea.Substring($i + 1).Trim().Trim('"')
        Set-Item -Path "env:$clave" -Value $valor
    }
}

foreach ($requerida in @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD')) {
    if (-not (Get-Item "env:$requerida" -ErrorAction SilentlyContinue).Value) {
        Write-Host "Falta la variable $requerida en el archivo .env" -ForegroundColor Red
        exit 1
    }
}

$env:SPRING_PROFILES_ACTIVE = 'neon'
Write-Host "Perfil neon activo. Conectando a $($env:DB_URL -replace '://.*@', '://***@')" -ForegroundColor Cyan
& (Join-Path $PSScriptRoot 'gradlew.bat') bootRun
