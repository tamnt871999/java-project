<#
    Chay REST API ma khong can Maven - chi can JDK 21.

      .\run.ps1 build      # bien dich vao out/
      .\run.ps1 serve      # chay API tai http://localhost:8080
      .\run.ps1 serve 9090 # chay o cong khac
      .\run.ps1 clean
#>
param(
    [Parameter(Position = 0)]
    [string]$Command = "serve",

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Rest
)

$ErrorActionPreference = "Stop"

$root      = $PSScriptRoot
$outDir    = Join-Path $root "out"
$mainClass = "com.example.ordering.adapter.Main"

function Invoke-Build {
    Write-Host "==> Bien dich src/main/java" -ForegroundColor Cyan
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
    $sources = Get-ChildItem -Path (Join-Path $root "src/main/java") -Filter *.java -Recurse |
               ForEach-Object { $_.FullName }
    & javac -d $outDir -encoding UTF-8 $sources
    if ($LASTEXITCODE -ne 0) { throw "Bien dich that bai" }
    Write-Host "    OK" -ForegroundColor Green
}

switch ($Command.ToLower()) {
    "build" { Invoke-Build }
    "serve" { Invoke-Build; & java -cp $outDir $mainClass @Rest }
    "clean" {
        if (Test-Path $outDir) { Remove-Item -Recurse -Force $outDir }
        Write-Host "Da xoa thu muc build" -ForegroundColor Green
    }
    default {
        Write-Host "Lenh khong hop le: $Command"
        Write-Host "Cac lenh: build | serve [port] | clean"
        exit 2
    }
}
