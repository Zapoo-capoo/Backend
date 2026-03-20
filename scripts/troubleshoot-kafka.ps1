# Troubleshoot Kafka + Notification Service (PowerShell)
# Usage: Run from repo root or call this script directly
# Example: pwsh -ExecutionPolicy Bypass -File .\scripts\troubleshoot-kafka.ps1

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
Write-Host "=== Kafka & Notification-service troubleshooting script ==="
Write-Host "Timestamp: $timestamp"
Write-Host "Working directory: $(Get-Location)"
Write-Host "Note: This script assumes Docker is used for Kafka (container name 'kafka') and that Kafka's EXTERNAL listener is available at localhost:9094.`n"

function Run-CommandCapture {
    param($cmd)
    try {
        $out = & cmd /c $cmd 2>&1
        return $out
    } catch {
        return $_.Exception.Message
    }
}

# 1) Check Docker
Write-Host "\n[1] Checking Docker availability..."
if (Get-Command docker -ErrorAction SilentlyContinue) {
    docker version --format 'Client: {{.Client.Version}}' 2>$null | Write-Host
    Write-Host "Docker appears available. Listing running containers (filtered for 'kafka')..."
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}" | Select-String -Pattern "kafka" -Quiet | Out-Null
    docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}" | Write-Host
} else {
    Write-Host "Docker CLI not found in PATH. Install Docker Desktop or ensure 'docker' is available." -ForegroundColor Yellow
}

# 2) Check kafka container
Write-Host "\n[2] Checking for Kafka container named 'kafka'..."
$kafkaContainer = docker ps --filter "name=kafka" --format "{{.Names}}" 2>$null
if (-not [string]::IsNullOrWhiteSpace($kafkaContainer)) {
    Write-Host "Found container: $kafkaContainer"
    Write-Host "Listing exposed ports and recent logs (last 50 lines):"
    docker inspect --format '{{json .NetworkSettings.Ports}}' kafka | Write-Host
    docker logs --tail 50 kafka | Write-Host

    Write-Host "\nListing Kafka topics (may error if kafka tools not present inside image):"
    try {
        docker exec kafka /bin/sh -c "kafka-topics --bootstrap-server localhost:9094 --list" 2>&1 | Write-Host
    } catch {
        Write-Host "Could not run kafka-topics inside container. The image may not include the cli or container name differs." -ForegroundColor Yellow
    }
} else {
    Write-Host "Kafka container named 'kafka' not found among running containers." -ForegroundColor Yellow
}

# 3) Network connectivity checks
Write-Host "\n[3] Testing connections to expected ports (host)..."
$portsToTest = @(9094, 8082, 8080, 8081)
foreach ($p in $portsToTest) {
    $res = Test-NetConnection -ComputerName localhost -Port $p -WarningAction SilentlyContinue
    $ok = $res.TcpTestSucceeded
    Write-Host ("Port {0}: {1}" -f $p, ($ok ? "OPEN" : "CLOSED/UNREACHABLE"))
}

# 4) Produce a test message into topic 'onboard-successful' (via docker kafka-console-producer)
Write-Host "\n[4] Producing a test message to topic 'onboard-successful' (via docker exec into 'kafka' container)..."
$testMessage = "Test message from troubleshoot script at $timestamp"
$producerCmd = "printf '%s\n' \"$testMessage\" | kafka-console-producer --broker-list localhost:9094 --topic onboard-successful"
try {
    $produceOut = docker exec -i kafka /bin/sh -c "$producerCmd" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Produced test message to topic 'onboard-successful':`n  $testMessage"
    } else {
        Write-Host "Producer command finished with exit code $LASTEXITCODE. Output:" -ForegroundColor Yellow
        Write-Host $produceOut
    }
} catch {
    Write-Host "Failed to exec into kafka container to produce message. Error: $_" -ForegroundColor Yellow
}

# 5) Consume a few messages from the topic to confirm delivery
Write-Host "\n[5] Consuming up to 5 messages from 'onboard-successful' (from beginning) to verify delivery (via docker exec)..."
try {
    docker exec kafka /bin/sh -c "kafka-console-consumer --bootstrap-server localhost:9094 --topic onboard-successful --from-beginning --max-messages 5" 2>&1 | Write-Host
} catch {
    Write-Host "Failed to run kafka-console-consumer inside container. Error: $_" -ForegroundColor Yellow
}

# 6) Check notification-service availability
Write-Host "\n[6] Checking notification-service (port 8082) and a simple HTTP GET..."
$portCheck = Test-NetConnection -ComputerName localhost -Port 8082 -WarningAction SilentlyContinue
if ($portCheck.TcpTestSucceeded) {
    Write-Host "Port 8082 is reachable. Trying HTTP GET /notification (may 404; this only checks connectivity)"
    try {
        $url = 'http://localhost:8082/notification'
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($resp) {
            Write-Host "HTTP GET $url returned status $($resp.StatusCode)"
        } else {
            Write-Host "HTTP GET $url returned no content (may be 404 or blocked)." -ForegroundColor Yellow
        }
    } catch {
        Write-Host "HTTP GET failed or timed out: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "Port 8082 is not reachable. Ensure notification-service is running (spring boot) on the host." -ForegroundColor Yellow
}

# 7) Helpful commands to run if issues persist
Write-Host "\n[7] Next steps / helpful commands (copy & run in PowerShell):"
Write-Host " - Start both services (from each service folder):" -ForegroundColor Cyan
Write-Host "   cd .\\identity-service; .\\mvnw.cmd -DskipTests spring-boot:run" -ForegroundColor Gray
Write-Host "   cd .\\notification-service; .\\mvnw.cmd -DskipTests spring-boot:run" -ForegroundColor Gray
Write-Host " - Manually produce a message (from host, if kafka tools installed):" -ForegroundColor Gray
Write-Host "   kafka-console-producer --broker-list localhost:9094 --topic onboard-successful" -ForegroundColor Gray
Write-Host " - Show notification-service logs (if run via spring-boot:run you already see logs). If run in background, tail the log file you configured or run jar in foreground:" -ForegroundColor Gray
Write-Host "   java -jar target\\notification-service-0.0.1-SNAPSHOT.jar" -ForegroundColor Gray

Write-Host "\nScript finished. If you still don't see the 'Received message from Kafka' log in notification-service after producing messages, share the outputs above (docker ps, kafka logs, consumer output, and notification-service console logs) and I will continue diagnosing." -ForegroundColor Green

