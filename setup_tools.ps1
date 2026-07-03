$toolsDir = "D:\Assignment codity ai\tools"
if (!(Test-Path $toolsDir)) {
    New-Item -ItemType Directory -Path $toolsDir | Out-Null
}

function Download-And-Extract ($url, $zipName, $destDirName) {
    $zipPath = Join-Path $toolsDir $zipName
    $destPath = Join-Path $toolsDir $destDirName
    
    if (!(Test-Path $destPath)) {
        Write-Host "Downloading $url to $zipPath ..."
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $url -OutFile $zipPath
        
        Write-Host "Extracting $zipPath to $destPath ..."
        Expand-Archive -Path $zipPath -DestinationPath $toolsDir
        
        # Check what directory was created and rename it if needed
        # (Expand-Archive extracts the folder inside the zip)
        $extractedFolder = Get-ChildItem $toolsDir -Directory | Where-Object { $_.LastWriteTime -gt (Get-Date).AddMinutes(-2) -and $_.Name -ne $destDirName -and $_.Name -ne "postgresql" -and $_.Name -ne "maven" -and $_.Name -ne "jdk" -and $_.Name -ne "redis" } | Select-Object -First 1
        if ($extractedFolder) {
            Rename-Item -Path $extractedFolder.FullName -NewName $destDirName
        }
        
        Remove-Item $zipPath -Force
        Write-Host "$destDirName setup complete."
    } else {
        Write-Host "$destDirName already exists, skipping."
    }
}

# 1. Setup Redis
Download-And-Extract "https://github.com/tporadowski/redis/releases/download/v5.0.14.1/Redis-x64-5.0.14.1.zip" "redis.zip" "redis"

# 2. Setup Maven
Download-And-Extract "https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip" "maven.zip" "maven"

# 3. Setup JDK 21
Download-And-Extract "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse" "jdk.zip" "jdk"

# 4. Setup PostgreSQL
Download-And-Extract "https://get.enterprisedb.com/postgresql/postgresql-16.2-1-windows-x64-binaries.zip" "postgresql.zip" "postgresql"

Write-Host "All tools setup complete!"
