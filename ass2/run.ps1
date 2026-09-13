<#
    Chay bai tap 2 ma khong can Maven - chi can JDK 21.

      .\run.ps1 build              # bien dich src/main/java vao out/
      .\run.ps1 demo               # chay bo kich ban mau (mac dinh)
      .\run.ps1 quote "Ha Noi" 1200   # bao gia mot don hang
      .\run.ps1 test               # chay bo kiem thu SelfCheck
      .\run.ps1 clean
#>
param(
    [Parameter(Position = 0)]
    [string]$Command = "demo",

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Rest
)

$ErrorActionPreference = "Stop"

$root       = $PSScriptRoot
$outDir     = Join-Path $root "out"
$testOutDir = Join-Path $root "out-test"
$mainClass  = "com.example.logistics.bootstrap.Main"
$testClass  = "com.example.logistics.SelfCheck"

# Console Windows mac dinh dung codepage 1258/437 -> ten thanh pho co dau se bi
# vo. Ep ca console lan JVM ve UTF-8.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$javaOpts = @("-Dstdout.encoding=UTF-8", "-Dfile.encoding=UTF-8")
# Fitness function doc thang ma nguon trong src/main/java de canh luat kien truc,
# nen no can biet goc du an bat ke dang dung o thu muc nao.
$testOpts = $javaOpts + @("-Dproject.root=$root")

function Invoke-Build {
    Write-Host "==> Bien dich src/main/java" -ForegroundColor Cyan
    if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }
    $sources = Get-ChildItem -Path (Join-Path $root "src/main/java") -Filter *.java -Recurse |
               ForEach-Object { $_.FullName }
    & javac -d $outDir -encoding UTF-8 $sources
    if ($LASTEXITCODE -ne 0) { throw "Bien dich that bai" }
    Write-Host "    OK" -ForegroundColor Green
}

function Invoke-BuildTests {
    Invoke-Build
    Write-Host "==> Bien dich src/test/java" -ForegroundColor Cyan
    if (-not (Test-Path $testOutDir)) { New-Item -ItemType Directory -Path $testOutDir | Out-Null }
    $sources = Get-ChildItem -Path (Join-Path $root "src/test/java") -Filter *.java -Recurse |
               ForEach-Object { $_.FullName }
    & javac -d $testOutDir -cp $outDir -encoding UTF-8 $sources
    if ($LASTEXITCODE -ne 0) { throw "Bien dich test that bai" }
    Write-Host "    OK" -ForegroundColor Green
}

switch ($Command.ToLower()) {
    "build" { Invoke-Build }
    "demo"  { Invoke-Build; & java @javaOpts -cp $outDir $mainClass }
    "quote" {
        if ($Rest.Count -lt 2) {
            Write-Host 'Cach dung: .\run.ps1 quote "Ha Noi" 1200' -ForegroundColor Yellow
            exit 2
        }
        Invoke-Build
        & java @javaOpts -cp $outDir $mainClass @Rest
    }
    "test" {
        Invoke-BuildTests
        & java @testOpts -cp "$outDir;$testOutDir" $testClass
        if ($LASTEXITCODE -ne 0) { throw "Co bai kiem thu that bai" }
    }
    "clean" {
        foreach ($dir in @($outDir, $testOutDir)) {
            if (Test-Path $dir) { Remove-Item -Recurse -Force $dir }
        }
        Write-Host "Da xoa thu muc build" -ForegroundColor Green
    }
    default {
        Write-Host "Lenh khong hop le: $Command"
        Write-Host "Cac lenh: build | demo | quote <thanh pho> <gram> | test | clean"
        exit 2
    }
}
