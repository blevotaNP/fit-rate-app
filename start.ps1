# Starts the whole stack, including Docker Desktop itself.
#
# Docker Desktop does not start with Windows by default, so after a reboot `docker compose up`
# fails with "cannot connect to the Docker daemon" and nothing runs. This script removes that
# step: run it and wait.
#
#   powershell -ExecutionPolicy Bypass -File start.ps1

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

function Test-DockerReady {
    try {
        docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
        return $?
    } catch {
        return $false
    }
}

if (-not (Test-DockerReady)) {
    $exe = "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe"
    if (-not (Test-Path $exe)) {
        $exe = "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
    }
    if (-not (Test-Path $exe)) {
        Write-Host "Docker Desktop not found. Start it manually, then run this script again." -ForegroundColor Red
        exit 1
    }

    Write-Host "Starting Docker Desktop..." -ForegroundColor Cyan
    Start-Process $exe

    $deadline = (Get-Date).AddMinutes(3)
    while (-not (Test-DockerReady)) {
        if ((Get-Date) -gt $deadline) {
            Write-Host "Docker Desktop did not become ready within 3 minutes." -ForegroundColor Red
            exit 1
        }
        Start-Sleep -Seconds 5
        Write-Host "  waiting for the Docker engine..." -ForegroundColor DarkGray
    }
}

Write-Host "Docker is ready. Starting OutfitRating..." -ForegroundColor Cyan
docker compose --profile full up -d

# The API needs ~20s for Flyway and Hibernate before nginx can proxy to it; without this wait the
# first page load returns a 502 and looks like a broken build.
Write-Host "Waiting for the API to pass its health check..." -ForegroundColor Cyan
$deadline = (Get-Date).AddMinutes(3)
while ($true) {
    $status = docker compose ps --format '{{.Name}} {{.Status}}'
    if ($status -match 'outfitrating-api.*healthy') { break }
    if ($status -match 'outfitrating-api.*(Exited|unhealthy)') {
        Write-Host "The API container failed to start. Logs:" -ForegroundColor Red
        docker compose logs --tail 40 api
        exit 1
    }
    if ((Get-Date) -gt $deadline) {
        Write-Host "The API did not become healthy within 3 minutes." -ForegroundColor Red
        docker compose logs --tail 40 api
        exit 1
    }
    Start-Sleep -Seconds 5
}

Write-Host ""
docker compose ps --format '{{.Name}} -> {{.Status}}'
Write-Host ""
Write-Host "  Site     http://localhost:5173" -ForegroundColor Green
Write-Host "  API docs http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host ""
Write-Host "  Demo sign-in: mara / password123" -ForegroundColor DarkGray
Write-Host "  Stop with:    docker compose --profile full down" -ForegroundColor DarkGray
