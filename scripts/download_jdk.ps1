function Get-RedirectedUrl
{
    Param (
        [Parameter(Mandatory=$true)]
        [String]$URL
    )

    $request = [System.Net.WebRequest]::Create($url)
    $request.AllowAutoRedirect=$false
    $response=$request.GetResponse()

    If ($response.StatusCode -eq "Found")
    {
        $response.GetResponseHeader("Location")
    }
}

$url= "https://api.adoptopenjdk.net/v2/binary/releases/openjdk11?openjdk_impl=hotspot&os=windows&arch=x64&release=latest&type=jdk"

$fUrl = Get-RedirectedUrl $url
$filename = [System.IO.Path]::GetFileName($fUrl); 

Write-Host "Downloading $filename (approx. 230MB)"

[Net.ServicePointManager]::SecurityProtocol = "tls12, tls11, tls"
Invoke-WebRequest -Uri $url -OutFile $filename

Write-Host "Extracting JDK to $PSScriptRoot"

Expand-Archive $filename -DestinationPath $PSScriptRoot
