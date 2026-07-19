# Kullanim: .\run-dev.ps1 ticket-service customer-service
# Portlar: identity 9001, customer 9002, catalog 9003, order 9004, subscription 9005,
#          usage 9006, billing 9007, payment 9008, notification 9009, ticket 9010

param([string[]]$Services = @())

Set-Location $PSScriptRoot

function Wait-Port($port, $name) {
    Write-Host "$name ($port) bekleniyor..." -ForegroundColor Yellow
    while (-not (Test-NetConnection localhost -Port $port -WarningAction SilentlyContinue -InformationLevel Quiet)) {
        Start-Sleep 2
    }
    Write-Host "$name hazir." -ForegroundColor Green
}

function Start-Svc($name) {
    Write-Host "$name baslatiliyor..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList '-NoExit', '-Command', "cd '$PSScriptRoot'; mvn -pl $name spring-boot:run"
}

docker compose -f docker/docker-compose.yml up -d
Wait-Port 8085 keycloak

# config-server once ve tam ayakta olmali: servisler portlarini ondan aliyor,
# import "optional" oldugu icin hazir degilse hatasizca yanlis portta acilirlar
Start-Svc config-server
Wait-Port 8888 config-server

Start-Svc discovery-server
Wait-Port 8761 discovery-server

Start-Svc api-gateway
Start-Svc bff-server
Wait-Port 9011 bff-server

foreach ($s in $Services) { Start-Svc $s }

Write-Host "`nFrontend: cd telco-crm-fe; npm install; npm run dev  -> http://localhost:5173" -ForegroundColor Magenta
