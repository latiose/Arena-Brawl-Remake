$key = "C:\Users\drago\Desktop\lol\Server_key.pem"
$vm  = "azureuser@9.223.114.126"

.\gradlew build
if ($LASTEXITCODE -ne 0) { Write-Host "Fallo la compilacion"; exit 1 }

$jar = Get-ChildItem .\build\libs\ArenaBrawl-*.jar |
       Where-Object { $_.Name -notmatch "sources|javadoc|plain" } |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1

scp -i $key $jar.FullName "${vm}:/tmp/ArenaBrawl.jar"

ssh -i $key $vm "sudo rm -f /opt/minecraft/server/plugins/ArenaBrawl*.jar && sudo mv /tmp/ArenaBrawl.jar /opt/minecraft/server/plugins/ArenaBrawl.jar && sudo chown minecraft:minecraft /opt/minecraft/server/plugins/ArenaBrawl.jar && sudo systemctl restart minecraft"

Write-Host "Desplegado: $($jar.Name)"