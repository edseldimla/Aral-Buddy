$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
if (!(Get-Command javac -ErrorAction SilentlyContinue) -or !(Get-Command jar -ErrorAction SilentlyContinue)) {
    throw 'Install JDK 17 or newer and add its bin folder to PATH. A JRE alone cannot build the project.'
}
$classes = Join-Path $projectRoot 'build\classes'
$dist = Join-Path $projectRoot 'dist'
New-Item -ItemType Directory -Path $classes,$dist -Force | Out-Null
$sourceList = Join-Path $projectRoot 'build\sources.txt'
$sources = Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java'
$sourceLines = $sources | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
[IO.File]::WriteAllLines($sourceList, [string[]]$sourceLines, [Text.UTF8Encoding]::new($false))
& javac --release 17 -encoding UTF-8 -d $classes "@$sourceList"
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Read the compiler messages above.' }
$jarFile = Join-Path $dist 'AralBuddy.jar'
& jar --create --file $jarFile --main-class aralbuddy.Main -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Could not create the runnable JAR.' }
Write-Output "Build passed: $jarFile"
