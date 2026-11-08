$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
& (Join-Path $PSScriptRoot 'build.ps1')
$testClasses = Join-Path $projectRoot 'build\test-classes'
New-Item -ItemType Directory -Path $testClasses -Force | Out-Null
$sourceRoots = @((Join-Path $projectRoot 'src'), (Join-Path $projectRoot 'test'))
$testSources = Get-ChildItem -LiteralPath $sourceRoots -Recurse -Filter '*.java'
$sourceList = Join-Path $projectRoot 'build\test-sources.txt'
$sourceLines = $testSources | ForEach-Object { '"' + $_.FullName.Replace('\','/') + '"' }
[IO.File]::WriteAllLines($sourceList, [string[]]$sourceLines, [Text.UTF8Encoding]::new($false))
& javac --release 17 -encoding UTF-8 -d $testClasses "@$sourceList"
if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
& java -cp $testClasses aralbuddy.ServiceTests
if ($LASTEXITCODE -ne 0) { throw 'Service tests failed.' }
& java -cp $testClasses aralbuddy.GuiTests
if ($LASTEXITCODE -ne 0) { throw 'Swing tests failed.' }
Write-Output 'All checks passed. The GUI test briefly opens a test window.'
