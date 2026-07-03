$rootDir = "D:\Assignment codity ai"
$dbDataDir = Join-Path $rootDir "db-data"
$postgresBin = Join-Path $rootDir "tools\postgresql\bin\postgres.exe"
$initDbBin = Join-Path $rootDir "tools\postgresql\bin\initdb.exe"
$pgCtlBin = Join-Path $rootDir "tools\postgresql\bin\pg_ctl.exe"
$redisServerBin = Join-Path $rootDir "tools\redis-server.exe"

# 1. Initialize PostgreSQL if not done
if (!(Test-Path $dbDataDir)) {
    Write-Host "Initializing PostgreSQL database cluster in $dbDataDir..."
    & $initDbBin -D $dbDataDir -U postgres --auth=trust
    Write-Host "PostgreSQL cluster initialized."
} else {
    Write-Host "PostgreSQL database cluster already exists, skipping initialization."
}

# 2. Start PostgreSQL
Write-Host "Starting PostgreSQL..."
& $pgCtlBin -D $dbDataDir -l (Join-Path $rootDir "postgres.log") start
Write-Host "PostgreSQL started."

# 3. Start Redis in the background (we run it in a separate process)
Write-Host "Starting Redis Server..."
Start-Process -FilePath $redisServerBin -ArgumentList "--port 6379" -NoNewWindow -PassThru
Write-Host "Redis Server started on port 6379."

# Wait a second to ensure services are online
Start-Sleep -Seconds 2

# Check if port 5432 and 6379 are listening
Write-Host "Checking ports..."
Get-NetTCPConnection -LocalPort 5432 -ErrorAction SilentlyContinue | Format-Table -AutoSize
Get-NetTCPConnection -LocalPort 6379 -ErrorAction SilentlyContinue | Format-Table -AutoSize

Write-Host "Services startup script completed!"
